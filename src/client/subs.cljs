(ns client.subs
  (:require [re-frame.core :as rf]
            [client.widgets :refer [duration->sec]])
  (:require-macros [reagent.ratom :refer [reaction]]))

;; Server
(rf/reg-sub
 :server/status
 (fn [db _]
    (get-in db [:server :status])))

(rf/reg-sub
 :server/account
 (fn [db _]
   (get-in db [:server :account])))

(rf/reg-sub
 :server/id
 (fn [db _]
   (get-in db [:server :id])))

;; UI elements
(rf/reg-sub-raw
 :modal
 (fn [db _] (reaction (:modal @db))))

(rf/reg-sub
  :active-panel
  (fn [db _]
    (:active-panel db)))

(rf/reg-sub
 :loaded
 (fn [db]
   (:loaded db)))

(rf/reg-sub
 :loaded-recipe
 (fn []
   (rf/subscribe [:loaded]))
 (fn [loaded]
   (:recipe loaded)))

(rf/reg-sub
 :loaded-supplier
 (fn []
   (rf/subscribe [:loaded]))
 (fn [loaded]
   (:supplier loaded)))

(rf/reg-sub
 :loaded-location
 (fn []
   (rf/subscribe [:loaded]))
 (fn [loaded]
   (:location loaded)))

(rf/reg-sub
 :loaded-date
 (fn []
   (rf/subscribe [:loaded]))
 (fn [loaded]
   (:date loaded)))

(rf/reg-sub
 :errors
 (fn [db]
   (get db :errors [])))

(rf/reg-sub
 :feed
 (fn [db]
   (get db :feed [])))


(rf/reg-sub
 :updates
 (fn [db [_ id]]
   (get-in db [:updates id] [])))


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
               [(str (:name unit) " [" (:abbrev unit) "]") id]))))


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

(rf/reg-sub
 :recipe/task-pos
 (fn [ [_ recipe task]]
   (rf/subscribe [:recipe/task-list recipe]))
 (fn [task-list [_ recipe task]]
   (.indexOf task-list task)))

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
(fn [db [_ task-id]]
  (get-in db [:tasks task-id :equipment])))

(rf/reg-sub
  :task/ingredients
  (fn [db [_ task-id]]
    (get-in db [:tasks task-id :ingredients])))

(rf/reg-sub
 :task/yields
 (fn [db [_ task-id]]
   (get-in db [:tasks task-id :yields])))

(rf/reg-sub
 :task/status
 (fn [db [_ task-id]]
   (if (integer? task-id)            ; only temp-ids should be an integer
     :new
     (if (> 0 (count @(rf/subscribe [:updates task-id])))
       :dirty
       :saved))))

;; Locations
(rf/reg-sub
:locations
(fn [db]
  (:locations db)))

(rf/reg-sub
:location/source
(fn []
  (rf/subscribe [:locations]))
(fn [location-index]
  (into [] (for [[id location] location-index]
             [(:name location)  id]))))

(rf/reg-sub
:location/name
(fn [db [_ location-id]]
  (get-in db [:locations location-id :name])))

(rf/reg-sub
:location/description
(fn [db [_ location-id]]
  (get-in db [:locations location-id :description])))

(rf/reg-sub
:location/address
(fn [db [_ location-id]]
  (get-in db [:locations location-id :address])))

(rf/reg-sub
 :location/inventory
 (fn [db [_ location-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:inventory location-id :qty item-id])
              :unit (get-in db [:inventory location-id :units item-id])
              :item item-id})
           (get-in db [:inventory location-id :items]))))

;Supplers

(rf/reg-sub
 :suppliers
 (fn [db]
   (:suppliers db)))

(rf/reg-sub
 :supplier/source
 (fn []
   (rf/subscribe [:suppliers]))
 (fn [supplier-index]
   (into [] (for [[id supplier] supplier-index]
              [(:name supplier) id]))))

(rf/reg-sub
 :supplier/name
 (fn [db [_ supplier-id]]
   (get-in db [:suppliers supplier-id :name])))

(rf/reg-sub
 :supplier/description
 (fn [db [_ supplier-id]]
   (get-in db [:suppliers supplier-id :description])))

(rf/reg-sub
 :supplier/address
 (fn [db [_ supplier-id]]
   (get-in db [:suppliers supplier-id :address])))


;; Schedule

(rf/reg-sub
 :events
 (fn [db]
   (:events db)))

(rf/reg-sub
:event/duration 
(fn [db [_ event-id]]
  (let [tasks (get-in db [:events event-id :tasks])]          ;;TODO determine if recipes are stored in :tasks field
    (reduce + (map #(duration->sec (get-in db [:tasks % :duration] 0)))))))

(comment
  (rf/reg-sub
   :events/in-range
   (fn [start end]
     (rf/subscribe [:events]))
   (fn [start end events]
     (filter (overlaps? start end (event-start event) (event-end end))))))
