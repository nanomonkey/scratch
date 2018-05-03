(ns scratch.events
  (:require [re-frame.core :as rf]
            [scratch.db :as db]))


(rf/reg-event-db
 :modal
 (fn [db [_ data]]
   (assoc-in db [:modal] data)))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/recipe-db))

(rf/reg-event-db
 :load-recipe 
(fn [db [_ recipe-id]]
  (assoc-in db [:loaded-recipe] recipe-id)))

(rf/reg-event-db
 :update-name
 (fn [db [_ recipe-id name]]
   (assoc-in db [:recipes recipe-id :name] name)))

(rf/reg-event-db
 :update-description
 (fn [db [_  recipe-id description]]
   (assoc-in db [:recipes recipe-id :description] description)))

(rf/reg-event-db
 :save-tag
 (fn [db [_ recipe-id tag]]
   (let [tag (-> tag
                 .trim
                 .toLowerCase)])
   (update-in db [:recipes recipe-id :tags] (fnil conj #{}) tag)))

(rf/reg-event-db
 :remove-tag
 (fn [db [_ recipe-id tag]]
   (update-in db [:recipes recipe-id :tags] (fn [tags]
                                              (vec (remove #{tag} tags))))))

(defonce last-temp-id (atom 0))

(rf/reg-cofx
  :temp-id 
  (fn [cofx _]
    (assoc cofx :temp-id (swap! last-temp-id inc))))

(rf/reg-event-fx
 :new-recipe
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name description tags tasks]]
   {:db
    (update (:db cofx) :recipe assoc
            (str (:temp-id cofx)) {:id (:temp-id cofx)
                                   :name name
                                   :description description
                                   :tags tags
                                   :tasks tasks})}
   ;return temp-id 
   (:temp-id cofx)))

(rf/reg-event-fx
 :new-item
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name description tags]]
   {:db 
    (update (:db cofx) :items assoc
            (str (:temp-id cofx)) {:id (:temp-id cofx) 
                                   :name name 
                                   :description description
                                   :tags tags})}))

(rf/reg-event-fx
 :new-unit
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name abbrev type]]
   (let [temp-id (str (:temp-id cofx))]
     {:db
      (update (:db cofx) :units assoc
              temp-id {:id temp-id 
                       :name name 
                       :abbrev abbrev 
                       :type type})})))

(rf/reg-event-db
 :task/add-ingredient
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :ingredients :qty item-id]))
     ;; not in the task, add to qty and ingredients
     (-> db
         (assoc-in [:tasks task-id :ingredients :qty item-id] qty)
         (update-in [:tasks :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :ingredients :qty item-id] + qty))))

(rf/reg-event-db
 :task/add-product
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :ingredients :qty item-id]))
     ;; not in the task, add to qty and ingredients
     (-> db
         (assoc-in [:tasks task-id :yields :qty item-id] qty)
         (update-in [:tasks :yields] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :yields :qty item-id] + qty))))

(rf/reg-event-db
 :task/add-step
 (fn [db [_ task-id step]]
   (update-in db [:tasks task-id :steps] (fnil conj []) step)))
