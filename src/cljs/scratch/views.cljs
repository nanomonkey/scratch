(ns scratch.views
  (:require [re-frame.core :as rf]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [reagent.core :as reagent]
            [accountant.core :as accountant]
            [secretary.core :as secretary :refer-macros [defroute]]
            [scratch.subs :as subs]
            [scratch.widgets :refer [markdown-section inline-editor tag-editor]]
            [goog.string :as gstring]
            [goog.string.format]))


(defn header []
  [:div.header "Made From Scratch"])

(defn topnav []
  [:div.topnav
      [:a {:href "#1"} "One"]
      [:a {:href "#2"} "Two"]])

(defn display-line-item [line-item]
  "unpacks dictionary with :unit :item and :qty into readable string"
  (let [qty (:qty line-item)
        unit (rf/subscribe [:unit-abbrev (:unit line-item)])
        item (rf/subscribe [:item-name (:item line-item)])]
    (goog.string/format "%i%s - %s" qty @unit @item)))

(defn span-items [items]
  [:span 
   (for [item items]
     (display-line-item item))])

(defn list-items [items]
   [:ul
    (for [i items]
      [:li {:key (:item i)} (display-line-item i)])])

(defn display-procedure [task]
  [:div#task
   [:div.steps-indicator
    [:div.connector]
    [:div.connector.complete]
    [:ol.steps    
     (for [step @(rf/subscribe [:task-procedure task])]
       [:li.active (markdown-section step)])
     [:li.active "Yields: " (span-items @(rf/subscribe [:task-yields task]))]]]])

(defn task-table [recipe-id]
  (fn [recipe-id]
    (let [tasks @(rf/subscribe [:recipe-task-list recipe-id])]
      [:table#tasks
       [:tr
        (doall
         (for [h 
               ["Equipment" "Ingredients" "Procedure"]]
           [:th h]))]
       [:tbody
        (doall
         (for [task tasks]
           [:tr 
            [:td (list-items @(rf/subscribe [:task-equipment-line-items task]))]
            [:td (list-items @(rf/subscribe [:task-ingredients-line-items task]))]
            [:td (display-procedure task)]]))]]))) 

(defn line-item-editor []
  (let [s (reagent/atom {})]
    [:span 
     [:form {:on-submit #(do (.preventDefault %))}
      [:input {:type :number :name "qty" :value (:qty @s)}]
      [:select 
       (doall
        (for [[id unit] @(rf/subscribe [:units])]
          [:option {:value id} (:name unit)]))]
      [:select
       (doall
        (for [[id item] @ (rf/subscribe [:items])]
          [:option {:value id} (:name item)]))]]]))


(defn create-item []
  (let [s (reagent/atom {})]
    [:form {:on-submit #(do
                          (.preventDefault %))}
     [:input {:type :text :name "item-name" :value (:name @s)}]
     [:input {:type :text :name "item-description" :value (:description @s)}]
     ;; add tag input
     [:button {:on-click #(do (rf/dispatch [:new-item @s])
                              (.preventDefault %))}
      "Create Item"]]))


(defn add-item [name description tags]
  (rf/dispatch [:new-item name description tags]))


(defn main-panel []
  (let [name (rf/subscribe [:recipe-name "r1"])
        description (rf/subscribe [:recipe-description "r1"])]
    [:div
     (header)
     (topnav)
     [:div.row
      [:div.column.left "left side"]
      [:div.column.middle
       [:h2 [inline-editor @name
             #(rf/dispatch [:update-name "r1" %])]]
       [:div [inline-editor @description
              #(rf/dispatch [:update-description "r1" %])]]
       [:div [tag-editor "r1"]]
       [:div [task-table "r1"]]
       [:div [line-item-editor]]
       [:div (prn-str @(rf/subscribe [:items]))]]]]))

 (when-some [el (js/document.getElementById "scratch-views")]
    (defonce _init (rf/dispatch-sync [:initialize]))
    (reagent/render [main-panel] el))


