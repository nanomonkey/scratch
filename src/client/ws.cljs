(ns client.ws
  (:require
   [taoensso.sente  :as sente  :refer (cb-success?)]
   [re-frame.core :as rf]
   [client.handlers :as handlers]))


(defonce router_ (atom nil))

(let [{:keys [chsk ch-recv send-fn state]}
      (sente/make-channel-socket-client!
       "/chsk" ; Must match server Ring routing URL
       {:type   :auto
        :packer :edn})]

  (def chsk       chsk)
  (def ch-chsk    ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state)   ; Watchable, read-only atom
)


(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! ch-chsk handlers/event-msg-handler)))

(defn start! []
  (println "Start WS client..")
  ;(create-client!)
  (start-router!))

(defn ssb-login! [username password]   
  "Trigger an Ajax POST request that resets our server-side session. Then we ask
 our channel socket to reconnect, thereby picking up the new  session" 
  (sente/ajax-lite "/login"
                   {:method :post
                    :headers {:x-csrf-token (:csrf-token @chsk-state)}
                    :params {:username    (str username)
                             :password    (str password)}}
                   (fn [ajax-resp]
                     (let [login-successful? true 
                                        ; Your logic here
                           ]
                       (if-not login-successful?
                         (rf/dispatch [:login-failed])
                         (do
                           (rf/dispatch [:login-successful username @chsk-state])
                           (sente/chsk-reconnect! chsk)))))))

(defn ssb-create-account! [username password]   
  (sente/ajax-lite "/new_account"
                   {:method :post
                    :headers {:x-csrf-token (:csrf-token @chsk-state)}
                    :params {:username    (str username)
                             :password   (str password)}}
                   (fn [ajax-resp]
                     (let [content (:?content (js->clj ajax-resp :keywordize-keys true))
                           account-created? true 
                                        ; Your logic here
                           ]
                       (if-not account-created?
                         (rf/dispatch [:account :creation-failed])
                         (do
                           (rf/dispatch [:account content])
                           (ssb-login! username password)))))))


;; New methods that don't seem to work reliably


(comment
  (def socket (atom nil))
  (def ch-chsk (atom nil))    ; ChannelSocket's receive channel
  (def chsk-send! (atom nil)) ; ChannelSocket's send API fn
  (def chsk-state (atom nil)) ; Watchable, read-only atom

  (def config {:protocol :http
               :host     "localhost"  ;;TODO get from config file or localstore
               :port     5000
               :type     :auto
               :packer   :edn})

(defn state-watcher [_key _atom _old-state new-state]
  (reset! chsk-state new-state)
  (rf/dispatch [:server/status new-state])
  (.warn js/console "New state" new-state))

(defn create-client! []
    (let [?csrf-token (when-let [el (.getElementById js/document "sente-csrf-token")]
                        (.getAttribute el "data-csrf-token"))
          {:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk" ?csrf-token config)]
      (reset! socket chsk)
      (reset! ch-chsk ch-recv)
      (reset! chsk-send! send-fn)
      (add-watch state :state-watcher state-watcher)))

)
