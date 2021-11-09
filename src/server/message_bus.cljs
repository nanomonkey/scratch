(ns server.message-bus
  (:require [cljs.core.async :refer [go-loop pub sub chan <! put!]]))

(defonce msg-ch (chan 1))                                     
(defonce msg-bus (pub msg-ch ::type))                          

(defn dispatch!                                            
 ([type] (dispatch! type nil))
 ([type payload]
  (put! msg-ch {::type type
                ::payload payload})))

(defn handle! [type handle]                              
  (let [sub-ch (chan)]
    (sub msg-bus type sub-ch)
    (go-loop []
      (handle (::payload (<! sub-ch)))
      (recur))))
