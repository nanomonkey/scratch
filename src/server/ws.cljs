(ns server.ws
  "Sente based web socket"
  (:require
   [server.ssb :as ssb]
   [server.message-bus :refer (dispatch! handle!)]
   [clojure.string     :as str]
   [hiccups.runtime    :as hiccupsrt]
   [cljs.core.async    :as async  :refer (<! >! put! take! chan)]
   [taoensso.encore    :as encore :refer ()]
   [taoensso.timbre    :as timbre :refer-macros (tracef debugf infof warnf errorf)]
   [taoensso.sente     :as sente]
   ;[taoensso.sente.server-adapters.community.generic-node :as generic-node]
   ;[taoensso.sente.server-adapters.community.express :as sente-express]
   [taoensso.sente.server-adapters.express :as sente-express]

   ;;nodejs libraries
   ["http" :as http]
   ["express" :as express]
   ["express-ws" :as express-ws]
   ["ws" :as ws]
   ["cookie-parser" :as cookie-parser]
   ["body-parser" :as body-parser]
   ["csurf" :as csurf]
   ["express-session" :as express-session]
   ;["express-static" :as express-static]
   ["formidable" :as formidable]
   ["path" :as path])
  (:require-macros
   [hiccups.core :as hiccups :refer [html]]
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

(set! js/console.debug js/console.log)
(enable-console-print!)
(timbre/set-level! :trace) ; Uncomment for more logging

;;;; Ring handlers

(let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn
              connected-uids]}
      (sente-express/make-express-channel-socket-server! 
       {:packer :edn
        :csrf-token-fn nil
        ;:csrf-token-fn (fn [^js ring-req] (str (.csrfToken  ring-req)))
        :user-id-fn (fn [^js ring-req] (aget (:body ring-req) "session" "uid"))})]
  (def ajax-post                ajax-post-fn)
  (def ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn)
  (def ch-chsk                  ch-recv) ; ChannelSocket's receive channel
  (def chsk-send!               send-fn) ; ChannelSocket's send API fn
  (def connected-uids           connected-uids) ; Watchable, read-only atom
  )


(defn express-account-creation-handler
  [req res]
  (let [req-session (aget req "session")
        body (aget req "body")
        username (aget body "username")
        password (aget body "password")]
    (aset req-session "uid" username)
    ;TODO decide if filename should be generated from username,
    ;     check if it already exists, and encrypt it with a password.
    (if-let [id (ssb/use-account username)]
      (-> res
          (.status 200)
          (.json (clj->js {:username username
                           :id id})))
      (.send res "ERR_making_account"))))

(defn express-login-handler
  [req res]
  (let [req-session (aget req "session")
        body        (aget req "body")
        username     (aget body "username")
        password      (aget body "password")]
    (aset req-session "uid" username)
    ;; (-> res
    ;;     (.status 200)
    ;;     (.json (clj->js {:username username
    ;;                      :uid username}))
    (dispatch! :ssb-login {:username username :password password}) ;;TODO create login
    (.send res "Success")))

(defn express-upload-handler [req res next]
  (let [form (formidable. #js {:multiples true})
        uid (aget req "session" "body" "user-id")]
    (.parse form req 
            (fn [err fields files] (if err (next err) 
                                       (let [filename (.-filename files)
                                             name (.-name filename)
                                             filepath (.-path filename)]
                                         (debugf "Filename: %s Name: %s Path: %s" filename name filepath)
                                         (ssb/add-blob! uid filename)))))))  ; TODO get this to work?!

(defn landing-pg-handler [^js ring-req]
  (hiccups/html
   [:html5
    [:head
     ;[:div#sente-csrf-token {:data-csrf-token (str (.csrfToken  ring-req))}]
     [:meta {:charset "utf-8"}]
     [:link {:rel "stylesheet"
             :href "/css/main.css"}]]
    [:body
     [:div {:id "app"}]
     [:script {:src "/js/showdown.min.js" :type "application/javascript"}]
     [:script {:src "/js/main.js" :type "application/javascript"}]]]))

(defn routes [^js express-app]
  (doto express-app
    (.get "/" (fn [req res] (.send res (landing-pg-handler req))))
    (.ws "/chsk"
         (fn [ws req next]
           (ajax-get-or-ws-handshake req nil nil
                                     {:websocket? true
                                      :websocket  ws})))
    (.get "/chsk" ajax-get-or-ws-handshake)
    (.post "/chsk" ajax-post)
    (.post "/login" express-login-handler)
    (.post "/new_account" express-account-creation-handler)
    (.post "/upload" express-upload-handler)
    (.use (.static express "resources/public/"))
    (.use (fn [^js req res next]
            (warnf "Unhandled request: %s" (.-originalUrl req))
            (next)))))

(defn wrap-defaults [^js express-app ^js routes]
  (let [cookie-secret "the shiz"]
    (doto express-app
      (.use (fn [^js req res next]
              (tracef "Request: %s" (.-originalUrl req))
              (next)))
      (.use (express-session
             #js {:secret            cookie-secret
                  :resave            true
                  :cookie            #js {:sameSite "None"
                                          :secure true}
                  :store             (.MemoryStore express-session)
                  :saveUninitialized true}))
      (.use (.urlencoded body-parser
                         #js {:extended false}))
      (.use (cookie-parser cookie-secret))
      ;; (.use (csurf       ; CSRF protection middleware
      ;;        #js {:cookie true}))
      (routes))))

(defn main-ring-handler [express-app]
  (wrap-defaults express-app routes))

(defn start-selected-web-server! [ring-handler port]
  (infof "Starting express...")
  (let [express-app       (express)
        express-ws-server (express-ws express-app)]

    (ring-handler express-app)

    (let [http-server (.listen express-app port)]
      {:express-app express-app
       :ws-server   express-ws-server
       :http-server http-server
       :stop-fn     #(.close http-server)
       :port        port})))



;;;; Sente event handlers

(defmulti -event-msg-handler :id)

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (debugf "Unhandled event: %s" event)
    (when ?reply-fn
      (?reply-fn {:umatched-event-as-echoed-from-from-server event}))))

(defmethod -event-msg-handler
  :chsk/ws-ping
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn]}]
  (println "ping"))  ;;TODO: print "." without flush?

(defmethod -event-msg-handler
  :ssb/post
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [msg (:msg ?data)]
    (debugf "Post event: %s" event)
    (debugf "ev-msg: %s" ev-msg)
    (dispatch! :add-message {:uid uid :msg msg})))

(defmethod -event-msg-handler
  :ssb/upsert
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [response (ssb/upsert! uid ?data)]
    (when ?reply-fn (?reply-fn {:response response}))))

(defmethod -event-msg-handler
  :ssb/create
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [content (:content ?data)]
    (println (str content))
    ;(dispatch! :create {:uid uid :type type :content content}) 
    (ssb/publish! uid content :created)))

(defmethod -event-msg-handler
  :ssb/update
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [id (:id ?data)
        updates (:updates ?data)]
    ;(dispatch! :update {:uid uid :id :changes changes})
    (ssb/publish! uid {:type "update" :root id :updates updates} :response)))

(defmethod -event-msg-handler
  :ssb/tombstone
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [type (:type ?data)
        content (:content ?data)]
    ;(dispatch! :tombstone {:uid uid :id id})
    (ssb/publish! uid {:type "tombstone" :root id} :response)))

(defmethod -event-msg-handler
  :ssb/query
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [msg (:msg ?data)]
    (debugf "Query event: %s" event)
    (debugf "msg: %s" msg)
    (dispatch! :query {:uid uid :msg msg}) 
    ;(ssb/query uid msg)
    (when ?reply-fn (?reply-fn {:message (str "Query" msg)}))))

(defmethod -event-msg-handler
  :ssb/whoami
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]     
  (let [id (ssb/whoami! uid)]
    (do
      (println "Id: " id)
      (when ?reply-fn (?reply-fn {:id id})))))

(defmethod -event-msg-handler
  :ssb/query-explain
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [msg (:msg ?data)]
    (dispatch! :query-explain {:uid uid :msg msg})))


(defmethod -event-msg-handler
  :ssb/lookup-name
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [id (:msg ?data)]
    (dispatch! :lookup-name {:uid uid :id id})))

(defmethod -event-msg-handler
  :ssb/add-file
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [file (:file ?data)]
    (dispatch! :add-file {:uid uid :file file})))

(defmethod -event-msg-handler
  :ssb/get-blob
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [blob-id (:blob-id ?data)]
    (dispatch! :get-blob {:uid uid :blob-id blob-id})))

(defmethod -event-msg-handler
  :ssb/serve-blob
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (let [blob-id (:blob-id ?data)]
    (dispatch! :serve-blob {:uid uid :blob-id blob-id})))

(defmethod -event-msg-handler
  :ssb/list-blobs
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (dispatch! :list-blobs {:uid uid}))

(defmethod -event-msg-handler
  :ssb/display-blobs
  [{:as ev-msg :keys [event id ?data ring-req ?reply-fn send-fn uid]}]
  (dispatch! :display-blobs {:uid uid}))

(defmethod -event-msg-handler :chsk/uidport-open
  [{:as ev-msg :keys [ring-req]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (if uid
      (timbre/infof "User connected: user-id `%s`" uid)
      (timbre/infof "User connected: no user-id (user didn't have login session)"))))

(defmethod -event-msg-handler :chsk/uidport-close
  [{:as ev-msg :keys [ring-req]}]
  (let [session (:session ring-req)
        uid     (:uid     session)]
    (if uid
      (timbre/infof "User disconnected: user-id `%s`" uid)
      (timbre/infof "User disconnected: no user-id (user didn't have login session)"))))

;; Message Bus Handlers
;; routes data to seperate processes via tagged async channels

(defonce message-handlers 
  {:error  (fn [{:keys [uid message]}] (println "Error: " message)
             (chsk-send! uid [:ssb/error-event {:message message}]))
   :response (fn [{:keys [uid message]}] (chsk-send! uid [:ssb/response {:message message}]))
   :created (fn [{:keys [uid message]}] (chsk-send! uid [:ssb/created {:message message}]))
   :feed (fn [{:keys [uid message]}] (chsk-send! uid [:ssb/feed {:message  message}]))
   :query-response (fn [{:keys [uid message]}] (chsk-send! uid [:ssb/query-response {:message message}]))
   :name (fn [{:keys [uid message]}] (chsk-send! uid [:ssb/contact-name {:message message}]))
   :blob (fn [{:keys [uid id url]}] (chsk-send! uid [:ssb/blob {:blob-id id :url url}]))
   :display (fn [{:keys [uid message]}] (chsk-send! uid [:ssb/display {:message message}]))}
)
(doall (map (fn [[k v]] (handle! k v)) message-handlers))

;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))

(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-server-chsk-router!
           ch-chsk event-msg-handler)))

;;;; Server to user async push 

(defn ws-send-ch [event-type ch]
  "continuously sends content from supplied channel"
  (go-loop []
    (chsk-send! [event-type (<! ch)])
  (recur)))

;;;; Init stuff

(defonce web-server_ (atom nil)) ; {:server _ :port _ :stop-fn (fn [])}
(defn stop-web-server! [] (when-let [m @web-server_] ((:stop-fn m))))

(defn start-web-server! [& [port]]
  (stop-web-server!)
  (let [{:keys [stop-fn port] :as server-map}
        (start-selected-web-server! (var main-ring-handler) (or port 4000))
        uri (str "http://localhost:" port "/")]
    (infof "Web server is running at `%s`" uri)
    (reset! web-server_ server-map)))

(defn stop!  []  (stop-router!)  (stop-web-server!))

(defn start! [] (start-router!) (start-web-server!))
