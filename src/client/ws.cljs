(ns client.ws
  (:require
   [taoensso.sente  :as sente  :refer (cb-success?)]
   [re-frame.core :as rf]
   [client.handlers :as handlers]))


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
  (rf/dispatch [:server/connected new-state])
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

(defn ssb-create-account! [username password]   
  (sente/ajax-lite "/new_account"
                   {:method :post
                    :headers {:x-csrf-token (:csrf-token @chsk-state)}
                    :params {:username    (str username)
                             :password   (str password)}}
                   (fn [resp]
                     (let [account-created? true 
                                        ; Your logic here
                           ]
                       (if-not account-created?
                         (rf/dispatch [:account :creation-failed])
                         (rf/dispatch [:account resp]))))))

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
                           (sente/chsk-reconnect! @ch-chsk)))))))
 
