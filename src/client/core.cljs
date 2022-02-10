(ns client.core
  (:require [reagent.core :as reagent]
            [reagent.dom :as rdom]
            [re-frame.core :as re-frame]
            [client.events :as events]
            [client.views :as views]
            [client.config :as config]))


(defn dev-setup []
  (when config/debug?
    (enable-console-print!)
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (rdom/render [views/main-panel]
               (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [::events/initialize-db])
 ;(re-frame/dispatch-sync [::events/load-localstore])
 ;(re-frame/dispatch-sync [::events/load-from-ssb])
  (dev-setup)
  (mount-root)
  (re-frame/dispatch-sync [:server/connect!]))
