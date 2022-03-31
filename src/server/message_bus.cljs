(ns server.message-bus
  (:require [cljs.core.async :refer [go go-loop pub sub chan <! put! take!]]))

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



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; PullStreams to core.async channels ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn pull->chan
  "Convert a pull-stream source into an error and return channel"
  [error-ch return-ch source]
  (source nil (fn read [err val]
                (if err
                  (go 
                    (put! error-ch err))
                  (go
                    (put! return-ch val
                          #(if %
                             (source nil read))))))))

(defn chan->pull
  "Convert a channel into a pull-stream source"
  [ch]
  (fn [end f]
    (if end
      (f end)
      (take! ch
             (fn [v]
               (if (nil? v) ; then channel has been closed
                 (f true)   ; and we should tell the pull-stream so (only once)
                 (f nil v)  ; otherwise pass on the value from the channel
                 ))))))
