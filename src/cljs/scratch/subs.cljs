(ns scratch.subs
  (:require [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::description
 (fn [db]
   (:description db)))

(re-frame/reg-sub
 :tags-raw
 (fn [db _]
   (:tags db)))

(re-frame/reg-sub
 :tags
 (fn [db _]
   (:tags db [])))

(re-frame/reg-sub
 :tabs-sorted
 (fn [] (re-frame/subscribe [:tags-raw]))
 (fn [tags]
   (sort tags)))

(re-frame/reg-sub
 :tasks
 (fn [db]
   (:tasks db)))
