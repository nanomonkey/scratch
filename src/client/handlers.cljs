(ns client.handlers
 (:require [re-frame.core :as rf]))


;;;; Sente event handlers

(defn log [message data]
  (.log js/console message (.stringify js/JSON (clj->js data))))

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
  (log "Unhandled ws event: " event))

(defmethod -event-msg-handler 
  :chsk/state
  [{:as ev-msg :keys [?data]}]
  (if (= ?data {:first-open? true})
    (rf/dispatch [:ws-established? true])
    (log "Channel socket state change: %s" ?data)))

(defmethod -event-msg-handler 
  :chsk/handshake
  [{:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data] ?data]
    (log "Handshake: %s" ?data)))

(defmulti chsk-recv (fn [id ?data] id))

(defmethod -event-msg-handler :chsk/recv
  [{:as ev-msg :keys [?data]}]
  (chsk-recv (?data 0) (?data 1)))

(comment
  ;; recieved message handlers
  (defmethod chsk-recv 
    :post-event
x    [id {:as ev-msg :keys [?data]}]
    (let [[?uid ?csrf-token ?handshake-data ?msg] ?data]
      (log "Message Posted: %s" ?msg)))

  (defmethod chsk-recv 
    :ssb/error-event
    [id {:as ?data :keys [message]}]
    (log "Error: %s" message))

  (defmethod chsk-recv 
    :ssb/response
    [id {:as ?data :keys [message]}]
    (log "SSB-response: %s" message))

  (defmethod chsk-recv 
    :ssb/feed
    [id {:as ?data :keys [message]}]
    (log "* %s" message))

  (defmethod chsk-recv 
    :ssb/contact-name
    [id {:as ?data :keys [message]}]
    (log "* %s" message))

  (defmethod chsk-recv 
    :ssb/blob
    [id {:as ?data :keys [message]}]
    (log message))
)


(defn ssb-login! [user-id config]   
  "Trigger an Ajax POST request that resets our server-side session. Then we ask
 our channel socket to reconnect, thereby picking up the new  session" 
  (sente/ajax-lite "/login"
                   {:method :post
                    :headers {:x-csrf-token (:csrf-token @chsk-state)}
                    :params {:user-id    (str user-id)
                             :config     (str config)}}
                   (fn [ajax-resp]
                     (rf/dispatch [:ajax-login-response ajax-resp])
                     (let [login-successful? true 
                                        ; Your logic here
                           ]
                       (if-not login-successful?
                         (rf/dispatch [:login-failed])
                         (do
                           (rf/dispatch [:login-successful])
                           (sente/chsk-reconnect! chsk)))))))


