(ns scratch.subs
  (:require [re-frame.core :as rf]))


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
 :unit-name
 (fn [db [_ key]]
   (get-in db [:units key :name])))

(rf/reg-sub
 :unit-abbrev
 (fn [db [_ key]]
   (get-in db [:units key :abbrev])))

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
 :item-name
 (fn [db [_ id]]
   (get-in db [:items id :name])))

;;Recipes

(rf/reg-sub
 :recipe
 (fn [db [_ id]]
   (get-in db [:recipes id])))

(rf/reg-sub
  :recipe-name
  (fn [db [_ id]]
    (get-in db [:recipes id :name])))

(rf/reg-sub
 :recipe-description
 (fn [db [_ id]]
   (get-in db [:recipes id :description])))

(rf/reg-sub
 :recipe-tags
 (fn [db [_ id]]
   (sort (get-in db [:recipes id :tags]))))

(rf/reg-sub
 :recipe-task-list
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
   (get-in db [:task id])))

(rf/reg-sub
 :task-name
 (fn [db [_ id]]
   (get-in db [:task id :name])))

(rf/reg-sub
 :task-procedure
 (fn [db [_ id]]
   (get-in db [:tasks id :procedure])))

(rf/reg-sub
 :task-equipment
(fn [db [_ id]]
  (get-in db [:tasks id :equipment])))

(rf/reg-sub
  :task-ingredients-items
  (fn [db [_ task-id]]
    (get-in db [:tasks task-id :ingredients :items])))

(rf/reg-sub
 :task-ingredients-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :ingredients :qty item-id])
              :unit (get-in db [:tasks task-id :ingredients :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :ingredients :items]))))

(rf/reg-sub
 :task-equipment-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :equipment :qty item-id])
              :unit (get-in db [:tasks task-id :equipment :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :equipment :items]))))

