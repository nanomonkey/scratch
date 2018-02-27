(ns scratch.subs
  (:require [re-frame.core :as re-frame]))


;; Units
(re-frame/reg-sub
 :units
 (fn [db]
   (:units db)))

(re-frame/reg-sub
 :unit
 (fn [db [_ key]]
   (get-in db [:unit key])))

(re-frame/reg-sub
 :unit-name
 (fn [db [_ key]]
   (get-in db [:units key :name])))

(re-frame/reg-sub
 :unit-abbrev
 (fn [db [_ key]]
   (get-in db [:units key :abbrev])))

;; Items
(re-frame/reg-sub
 :items
 (fn [db]
   (:items db)))

(re-frame/reg-sub
 :item
 (fn [db [_ id]]
   (get-in db [:items id])))

(re-frame/reg-sub
 :item-name
 (fn [db [_ id]]
   (get-in db [:items id :name])))

;;Recipes

(re-frame/reg-sub
 :recipe
 (fn [db [_ id]]
   (get-in db [:recipes id])))

(re-frame/reg-sub
  :recipe-name
  (fn [db [_ id]]
    (get-in db [:recipes id :name])))

(re-frame/reg-sub
 :recipe-description
 (fn [db [_ id]]
   (get-in db [:recipes id :description])))

(re-frame/reg-sub
 :recipe-tags
 (fn [db [_ id]]
   (sort (get-in db [:recipes id :tags]))))

(re-frame/reg-sub
 :recipe-task-list
 (fn [db [_ id]]
   (get-in db [:recipes id :task-list])))

;; Tasks
(re-frame/reg-sub
 :tasks
 (fn [db]
   (:tasks db)))

(re-frame/reg-sub
 :task
 (fn [db [_ id]]
   (get-in db [:task id])))

(re-frame/reg-sub
 :task-name
 (fn [db [_ id]]
   (get-in db [:task id :name])))

(re-frame/reg-sub
 :task-procedure
 (fn [db [_ id]]
   (get-in db [:tasks id :procedure])))

(re-frame/reg-sub
 :task-equipment
(fn [db [_ id]]
  (get-in db [:tasks id :equipment])))

(re-frame/reg-sub
  :task-ingredients-items
  (fn [db [_ task-id]]
    (get-in db [:tasks task-id :ingredients :items])))

(re-frame/reg-sub
 :task-ingredients-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :ingredients :qty item-id])
              :unit (get-in db [:tasks task-id :ingredients :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :ingredients :items]))))

(re-frame/reg-sub
 :task-equipment-line-items
 (fn [db [_ task-id]]
   (mapv (fn [item-id]
             {:qty (get-in db [:tasks task-id :equipment :qty item-id])
              :unit (get-in db [:tasks task-id :equipment :units item-id])
              :item item-id})
           (get-in db [:tasks task-id :equipment :items]))))

