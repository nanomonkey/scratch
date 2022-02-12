(ns client.handlers
 (:require [re-frame.core :as rf]
           [taoensso.sente  :as sente  :refer (cb-success?)]))


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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; recieved message handlers ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod chsk-recv 
  :post-event
  [id {:as ev-msg :keys [?data]}]
  (let [[?uid ?csrf-token ?handshake-data ?msg] ?data]
    (log "Message Posted: %s" ?msg)))

(defmethod chsk-recv 
  :ssb/error-event
  [id {:as ?data :keys [message]}]
  (rf/dispatch [:error message])
  (log "Error: %s" message))

(defmethod chsk-recv 
  :ssb/response
  [id {:as ?data :keys [message]}]
  (rf/dispatch [:feed message]))

(defmethod chsk-recv 
  :ssb/feed
  [id {:keys [message]}]
  (rf/dispatch [:feed message]))

(defmethod chsk-recv 
  :ssb/contact-name
  [id {:as ?data :keys [message]}]
  (log "* %s" message))

(defmethod chsk-recv 
  :ssb/blob
  [id {:as ?data :keys [message]}]
  (log "Blob: %" message))



