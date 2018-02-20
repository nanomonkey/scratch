(ns scratch.events
  (:require [re-frame.core :as re-frame]
            [scratch.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/recipe-db))

(re-frame/reg-event-db
 :update-name
 (fn [db [_ name]]
   (assoc db :name name)))

(re-frame/reg-event-db
 :update-description
 (fn [db [_  description]]
   (assoc db :description description)))

(re-frame/reg-event-db
 :save-tag
 (fn [db [_ tag]]
   (let [tag (-> tag
                 .trim
                 .toLowerCase)])
   (update db :tags (fnil conj #{}) tag)))

(re-frame/reg-event-db
 :remove-tag
 (fn [db [_ tag]]
   (update db :tags (fn [tags]
                      (vec (remove #{tag} tags))))))
