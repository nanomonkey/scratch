(ns client.ws
  (:require
   ;[clojure.string  :as str]
   ;[clojure.edn :as edn]
   ;[cljs.core.async :as async  :refer (<! >! put! chan)]
   ;[taoensso.encore :as encore :refer ()]
   ;[taoensso.timbre :as timbre :refer-macros (tracef debugf infof warnf errorf)]
   [taoensso.sente  :as sente  :refer (cb-success?)]
   [re-frame.core :as rf]
   [client.handlers :as handlers])

  (:require-macros
   [cljs.core.async.macros :as asyncm :refer (go go-loop)]))


(def router_ (atom nil))

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
  (.warn js/console "New state" new-state))

(defn create-client! []
  (let [{:keys [ch-recv send-fn state]} (sente/make-channel-socket-client! "/chsk" nil config)]
    (reset! ch-chsk ch-recv)
    (reset! chsk-send! send-fn)
    (add-watch state :state-watcher state-watcher)))

(defn stop-router! []
  (when-let [stop-f @router_] (stop-f)))

(defn start-router! []
  (stop-router!)
  (reset! router_ (sente/start-client-chsk-router! @ch-chsk handlers/event-msg-handler)))

(defn start! []
  (create-client!)
  (start-router!))
