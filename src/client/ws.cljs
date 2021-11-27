(ns client.ws
  (:require
   ;[clojure.string  :as str]
   ;[clojure.edn :as edn]
   ;[cljs.core.async :as async  :refer (<! >! put! chan)]
   ;[taoensso.encore :as encore :refer ()]
   ;[taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
   [taoensso.sente  :as sente  :refer (cb-success?)])

  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))

;; Sente Channnels
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

;;;; Sente event handlers

(defmulti -event-msg-handler
  "Multimethod to handle Sente `event-msg`s"
  :id ; Dispatch on event-id
  )

(defn event-msg-handler
  "Wraps `-event-msg-handler` with logging, error catching, etc."
  [{:as ev-msg :keys [id ?data event]}]
  (-event-msg-handler ev-msg))

(defmethod -event-msg-handler
  :default ; Default/fallback case (no other matching handler)
  [{:as ev-msg :keys [event]}]
  (->output! "Unhandled event: %s" event))

(defmethod -event-msg-handler 
  :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (->output! "Channel socket successfully established!")
    (->output! "Channel socket state change: %s" ?data)))

(defmethod -event-msg-handler 
  :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (->output! "Handshake: %s" ?data)))

(defmulti chsk-recv (fn [id ?data] id))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  ;(->output! "Push event from server: %s" ?data)
  (chsk-recv (?data 0) (?data 1)))

;; recieved message handlers
(defmethod chsk-recv 
  :post-event
  [id {:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data ?msg] ?data]
    (->output! "Message Posted: %s" ?msg)))

(defmethod chsk-recv 
  :ssb/error-event
  [id {:as ?data :keys [message]}]
  (->errors-put! "Error: %s" message))

(defmethod chsk-recv 
  :ssb/response
  [id {:as ?data :keys [message]}]
  (->output! "SSB-response: %s" message))

(defmethod chsk-recv 
  :ssb/feed
  [id {:as ?data :keys [message]}]
  (->feed! "* %s" message))

(defmethod chsk-recv 
  :ssb/contact-name
  [id {:as ?data :keys [message]}]
  (->feed! "* %s" message))

(defmethod chsk-recv 
  :ssb/blob
  [id {:as ?data :keys [message]}]
  (->image! message))

(defmethod chsk-recv 
  :ssb/display
  [id {:as ?data :keys [message]}]
  
  (add-image "name" message 200 200 "alt"))


;;;; Sente event router (our `event-msg-handler` loop)

(defonce router_ (atom nil))

(defn  stop-router! [] (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_
          (sente/start-client-chsk-router!
           ch-chsk event-msg-handler)))

