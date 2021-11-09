(ns server.core
  (:require [cljs.core.async :refer (go-loop pub sub chan put! take! close! timeout >! <!)]
            [goog.object :as gobj]
            [server.ws :as ws]
            [server.ssb :as ssb]
            [server.message-bus :as bus])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))


;; Main Loop

(defn main [& args]
  (ws/start!))

(defn reload! []
  (js/console.log "re-starting server"))
