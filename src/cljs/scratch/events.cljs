(ns scratch.events
  (:require [re-frame.core :as re-frame]
            [scratch.db :as db]))

(re-frame/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/recipe-db))

(re-frame/reg-event-db
 :update-name
 (fn [db [_ recipe-id name]]
   (assoc-in db [:recipes recipe-id :name] name)))

(re-frame/reg-event-db
 :update-description
 (fn [db [_  recipe-id description]]
   (assoc-in db [:recipes recipe-id :description] description)))

(re-frame/reg-event-db
 :save-tag
 (fn [db [_ recipe-id tag]]
   (let [tag (-> tag
                 .trim
                 .toLowerCase)])
   (update-in db [:recipes recipe-id :tags] (fnil conj #{} tag))))

(re-frame/reg-event-db
 :remove-tag
 (fn [db [_ recipe-id tag]]
   (update-in db [:recipes recipe-id :tags] (fn [tags]
                                              (vec (remove #{tag} tags))))))
