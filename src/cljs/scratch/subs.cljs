(ns scratch.subs
  (:require [re-frame.core :as rf])
  (:require-macros [reagent.ratom :refer [reaction]]))

;;UI elements
(rf/reg-sub-raw
 :modal
 (fn [db _] (reaction (:modal @db))))

(rf/reg-sub
 :loaded-recipe
 (fn [db]
   (:loaded-recipe db)))

;; Units
(rf/reg-sub
 :units
 (fn [db]
   (:units db)))

(rf/reg-sub
 :unit
 (fn [db [_ key]]
   (get-in db [:unit key])))

(rf/reg-sub
 :unit/name
 (fn [db [_ id]]
   (get-in db [:units id :name])))

(rf/reg-sub
 :unit/abbrev
 (fn [db [_ key]]
   (get-in db [:units key :abbrev])))

(rf/reg-sub
  :unit/source
  (fn []
    (rf/subscribe [:units]))
  (fn [unit-index]
    (into [] (for [[id unit] unit-index]
               [(:name unit) id]))))


;; Items
(rf/reg-sub
 :items
 (fn [db]
   (:items db)))

(rf/reg-sub
 :item
 (fn [db [_ id]]
   (get-in db [:items id])))

(rf/reg-sub
 :item/name
 (fn [db [_ id]]
   (get-in db [:items id :name])))

(rf/reg-sub
 :item/names
 (fn [db]
   (map val (:items db))))

(rf/reg-sub  
 :item/search
 (fn [db [_ text]]
   (->> (map val (:items db))
        (filter #(-> (:name %)
                     (.toLowerCase)
                     (.indexOf text)
                     (> -1)))
        (mapv #(vector (:name %) (:id %))))))

(rf/reg-sub
  :item/name-id
  (fn []
    @(rf/subscribe [:items]))
  (fn [item-index]
    (into {} (for [[id item] item-index]
                    [(:name item) id]))))

(rf/reg-sub
  :item/id-from-name
  (fn [txt]
    (rf/subscribe [:item/name-id]))
  (fn [txt item-index]
    (txt item-index)))

(rf/reg-sub
  :item/source
  (fn []
    (rf/subscribe [:items]))
  (fn [item-index]
    (into [] (for [[id item] item-index]
               [(:name item) id]))))

;;Recipes
(rf/reg-sub
 :recipe/ids
 (fn [db]
   (map key (:recipes db))))

(rf/reg-sub
 :recipe/names
 (fn [db]
   (map val (:recipes db))))

(rf/reg-sub
 :recipes
(fn [db]
  (filter
   #(-> % (:name %) (:id %)
        (map val (:recipes db))))))

(rf/reg-sub  
 :recipe/source
 (fn [db [_ text]]
    (->> db
         (filter #(-> (:name %)
                      (.toLowerCase)
                      (.indexOf text)
                      (> -1)))
         (mapv #(vector (:name %) (:id %))))))

(rf/reg-sub
 :recipe
 (fn [db [_ id]]
   (get-in db [:recipes id])))

(rf/reg-sub
  :recipe/name
  (fn [db [_ id]]
    (get-in db [:recipes id :name])))

(rf/reg-sub
 :recipe/description
 (fn [db [_ id]]
   (get-in db [:recipes id :description])))

(rf/reg-sub
 :recipe/tags
 (fn [db [_ id]]
   (sort (get-in db [:recipes id :tags]))))

(rf/reg-sub
 :recipe/task-list
 (fn [db [_ id]]
   (get-in db [:recipes id :task-list])))

;; Tasks
(rf/reg-sub
 :tasks
 (fn [db]
   (:tasks db)))

(rf/reg-sub
 :task
 (fn [db [_ id]]
   (get-in db [:tasks id])))

(rf/reg-sub
 :task/name
 (fn [db [_ id]]
   (get-in db [:tasks id :name])))

(rf/reg-sub
 :task/duration
 (fn [db [_ id]]
   (get-in db [:tasks id :duration])))

(rf/reg-sub
 :task/steps
 (fn [db [_ task-id]]
   (get-in db [:tasks task-id :steps])))

(rf/reg-sub
 :task/equipment
(fn [db [_ id]]
  (get-in db [:tasks id :equipment])))

(rf/reg-sub
  :task/ingredients-items
  (fn [db [_ task-id]]
    (get-in db [:tasks task-id :ingredients :items])))

(rf/reg-sub
 :task/ingredients-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :ingredients :qty item-id])
              :unit (get-in db [:tasks task-id :ingredients :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :ingredients :items]))))

(rf/reg-sub
 :task/equipment-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :equipment :qty item-id])
              :unit (get-in db [:tasks task-id :equipment :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :equipment :items]))))

(rf/reg-sub
 :task/optional-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :optional :qty item-id])
              :unit (get-in db [:tasks task-id :optional :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :optional :items]))))

(rf/reg-sub
 :task/yields
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :yields :qty item-id])
              :unit (get-in db [:tasks task-id :yields :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :yields :items]))))


