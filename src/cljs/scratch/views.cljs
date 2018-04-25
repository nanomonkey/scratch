(ns scratch.views
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent] 
            [accountant.core :as accountant]
            [secretary.core :as secretary :refer-macros [defroute]]
            [scratch.subs :as subs]
            [scratch.widgets :refer [markdown-section 
                                     inline-editor 
                                     tag-editor
                                     recipe-search
                                     item-search]]
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

(defn define-items [items]
  [:dl
   (for [item items]
     (do
       [:dt (:name item)]
       [:dd (:description item)]))])

(defn list-items [items]
   [:ul
    (for [i items]
      [:li {:key (:item i)} (display-line-item i)])])

(defn add-step [task]
  (let [s (reagent/atom "")]
    (fn [task]
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (rf/dispatch [:task/add-step task @s])
                            (reset! s ""))}
       [:input {:type :text
                :value @s
                :on-change #(reset! s (-> % .-target .-value))}]
       [:button "Add Step"]])))

(defn display-steps [task]
  [:div#task
   [:div.steps-indicator
    [:div.connector]
    [:div.connector.complete]
    [:ol.steps    
     (for [step @(rf/subscribe [:task-steps task])]
       [:li.active (markdown-section step)])]]
   [add-step task]
   [:div [:strong "Yields: "] (list-items @(rf/subscribe [:task-yields task]))]
   [:div (prn-str @(rf/subscribe [:task-steps task]))]])

(defn task-table [recipe-id]
  (fn [recipe-id]
    (let [tasks @(rf/subscribe [:recipe-task-list recipe-id])]
      [:table#tasks
       [:thead
        [:tr
         (doall
          (for [h 
                ["Equipment" "Ingredients" "Procedural Steps"]]
            [:th h]))]]
       [:tbody
        (doall
         (for [task tasks]
           [:tr 
            [:td (list-items @(rf/subscribe [:task-equipment-line-items task]))]
            [:td (list-items @(rf/subscribe [:task-ingredients-line-items task]))]
            [:td (display-steps task)]]))]]))) 

(defn line-item-editor []
  (let [s (reagent/atom {})]
    [:span 
     [:form {:on-submit #(do (.preventDefault %))}
      [:label "Quantity"]
      [:input  {:type :number :name "qty" :value (:qty @s)}]
      [:label "Unit:"]
      [:select 
       (doall
        (for [[id unit] @(rf/subscribe [:units])]
          [:option {:value id} (:name unit)]))]
      [:label "Item:"]
      [:select
       (doall
        (for [[id item] @(rf/subscribe [:items])]
          [:option {:value id} (:name item)]))]]]))

(defn create-item []
  (let [name (reagent/atom "")
        description (reagent/atom "")
        tags (reagent/atom #{})]
    (fn []
      [:div.blue-panel (prn-str @name @description)
       [:form {:on-submit #(do
                             (.preventDefault %))}
        [:row
         [:label "Name"]
         [:input.form-control {:type "text"
                               :placeholder "item name"
                               :value @name
                               :on-change #(reset! name (-> % .-target .-value))}]]
        [:div
         [:row
          [:label "Description"]
          [:input.form-control {:type "text"
                                :placeholder "item description"
                                :value @description
                                :on-change #(reset! description (-> % .-target .-value))}]]]
        [:row
         [:label "Tags"]]
        [:button {:on-click #(do (.preventDefault %)
                                 (rf/dispatch [:new-item @name @description @tags])
                                 (reset! name "")
                                 (reset! description ""))}
         "Create Item"]]])))

(defn add-line-item [name description tags]
  (rf/dispatch [:new-item name description tags]))

(comment
  (defn add-item [task-id item-id]
    [:button 
     {:on-click #(do (.preventDefault %)
                     (rf/dispatch [:task/add-ingredient task-id item-id qty unit]))}]))

(comment
  (defn remove-item
    [task-id item-id]
    [:div.garbage-bin 
     :on-click #(re-frame.core/dispatch [:task/remove-item task-id item-id])]))

(defn main-panel []
  (let [recipe-id (rf/subscribe [:loaded-recipe])
        name (rf/subscribe [:recipe-name @recipe-id])
        description (rf/subscribe [:recipe-description @recipe-id])]
    [:div
     (header)
     (topnav)
     [:div.row
      [:div.column.left [recipe-search]]
      [:div.column.middle
       [:h2 [inline-editor @name
             #(rf/dispatch [:update-name @recipe-id %])]]
       [:div [inline-editor @description
              #(rf/dispatch [:update-description @recipe-id %])]]
       [:div [tag-editor :recipe-tags :remove-tag :save-tag @recipe-id]]
       [:div [task-table @recipe-id]]
       
      ;; [:div [line-item-editor]]
      ;; [:div (prn-str @(rf/subscribe [:loaded-recipe]))]
       ]
      [:div.column.right
       [create-item]
       [:div (prn-str @ (rf/subscribe [:items]))]
       [:div (prn-str @(rf/subscribe [:recipe @recipe-id]))]]]]))

 (when-some [el (js/document.getElementById "scratch-views")]
    (defonce _init (rf/dispatch-sync [:initialize]))
    (reagent/render [main-panel] el))

