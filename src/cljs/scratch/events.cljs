(ns scratch.events
  (:require [re-frame.core :as rf]
            [scratch.db :as db]))


(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/recipe-db))

;; UI elements
(rf/reg-event-db
 :load-recipe 
(fn [db [_ recipe-id]]
  (assoc-in db [:loaded-recipe] recipe-id)))

(rf/reg-event-db
 :modal
 (fn [db [_ data]]
   (assoc-in db [:modal] data)))

;; Recipes
(rf/reg-event-db
 :recipe/update-name
 (fn [db [_ recipe-id name]]
   (assoc-in db [:recipes recipe-id :name] name)))

(rf/reg-event-db
 :recipe/update-description
 (fn [db [_  recipe-id description]]
   (assoc-in db [:recipes recipe-id :description] description)))

(rf/reg-event-db
 :recipe/save-tag
 (fn [db [_ recipe-id tag]]
   (let [tag (-> tag
                 .trim
                 .toLowerCase)])
   (update-in db [:recipes recipe-id :tags] (fnil conj #{}) tag)))

(rf/reg-event-db
 :recipe/remove-tag
 (fn [db [_ recipe-id tag]]
   (update-in db [:recipes recipe-id :tags] (fn [tags]
                                              (vec (remove #{tag} tags))))))

(defonce last-temp-id (atom 0))

(rf/reg-cofx
  :temp-id 
  (fn [cofx _]
    (assoc cofx :temp-id (swap! last-temp-id inc))))

(rf/reg-event-fx
 :recipe/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name description tags tasks]]
   {:db
    (update (:db cofx) :recipe assoc
             (:temp-id cofx) {:id (:temp-id cofx)
                                   :name name
                                   :description description
                                   :tags tags
                                   :tasks tasks})}
   ;return temp-id 
   (:temp-id cofx)))

(rf/reg-event-fx
 :item/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name description tags]]
   {:db 
    (update (:db cofx) :items assoc
             (:temp-id cofx) {:id (:temp-id cofx) 
                                   :name name 
                                   :description description
                                   :tags tags})}))

(rf/reg-event-fx
 :unit/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name abbrev type]]
   (let [temp-id (:temp-id cofx)]
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
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :ingredients :qty item-id] qty)
         (assoc-in [:tasks task-id :ingredients :units item-id] unit)
         (update-in [:tasks task-id :ingredients :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty, assume the same unit (?!)
     (update-in db [:tasks task-id :ingredients :qty item-id] + qty))))

(rf/reg-event-db
 :task/add-product
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :yields :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :yields :qty item-id] qty)
         (assoc-in [:tasks task-id :yields :units item-id] unit)
         (update-in [:tasks task-id :yields :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :yields :qty item-id] + qty))))

(rf/reg-event-db
 :task/add-equipment
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :equipment :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :equipment :qty item-id] qty)
         (assoc-in [:tasks task-id :equipment :units item-id] unit)
         (update-in [:tasks task-id :equipment :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :equipment :qty item-id] + qty))))

(rf/reg-event-db
 :task/add-optional
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :optional :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :optional :qty item-id] qty)
         (assoc-in [:tasks task-id :optional :units item-id] unit)
         (update-in [:tasks task-id :optional :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :optional :qty item-id] + qty))))

(rf/reg-event-db
 :task/add-step
 (fn [db [_ task-id step]]
   (update-in db [:tasks task-id :steps] (fnil conj []) step)))


(defn vec-remove
  "remove elem in coll"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(rf/reg-event-db
 :task/remove-step
 (fn [db [_ task-id step-pos]]
   (update-in db [:tasks task-id :steps] #(vec-remove % step-pos))))
