(ns scratch.subs
  (:require [re-frame.core :as re-frame]))


;; Units
(re-frame/reg-sub
 :unit
 (fn [db [_ key]]
   (get-in db [:unit key])))

(re-frame/reg-sub
 :unit-name
 (fn [db [_ key]]
   (get-in db [:unit key :name])))

(re-frame/reg-sub
 :unit-abbrev
 (fn [db [_ key]]
   (get-in db [:unit key :abbrev])))

;; Items
(re-frame/reg-sub
 :item
 (fn [db [_ id]]
   (get-in db [:item id])))

(re-frame/reg-sub
 :item-name
 (fn [db [_ id]]
   (get-in db [:item id :name])))

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
 :recipe-tags-raw
 (fn [db [_ id]]
   (get-in db [:recipe id :tags] [])))

(re-frame/reg-sub
 :recipe-tags-sorted
 (fn [id] (re-frame/subscribe [:recipe-tags-raw id]))
 (fn [tags]
   (sort tags)))


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
  :task-ingredients
  (fn [db [_ task-id]]
    (get-in db [:tasks task-id :ingredients])))

(comment
  (rf/reg-sub
   :task-
   (fn [db]
     (mapv (fn [id]
             {:quantity (get-in db [:cart :quantities id])
              :item (get-in db [:products id])})
           (get-in db [:cart :order])))))
