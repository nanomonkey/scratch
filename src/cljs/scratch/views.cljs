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
                                     item-search
                                     add-product
                                     modal]]
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
    (goog.string/format "%f %s - %s" qty @unit @item)))

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

(defn list-items [items remove-event task]
  (fn [items remove-event task]
    [:ul
     (for [i items]
       [:li {:key (:item i)} [:span.bacon (display-line-item i)]
        [:button.hidden
         {:title "Remove"
          :on-click #(do (.preventDefault %)
                         (rf/dispatch [remove-event task (:item i)]))} "X"]])]))

(defn add-step [task]
  (let [s (reagent/atom "")]
    (fn [task]
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (when (> (count @s) 0)
                              (rf/dispatch [:task/add-step task @s]))
                            (reset! s ""))}
       [:input {:type :text
                :value @s
                :on-change #(reset! s (-> % .-target .-value))}]])))

(defn display-steps [task]
  (fn [task]
    [:div#task
     [:h2 [inline-editor @(rf/subscribe [:task-name task]) 
           #(rf/dispatch [:task/update-name task %])]]
     [:div.steps-indicator
      [:div.connector]
      [:div.connector.complete]
      [:ol.steps  
       (let [steps @(rf/subscribe [:task-steps task])]
         (for [[index step] (map-indexed vector steps)]
           [:li.active [:span.bacon step]
            [:button.hidden
             {:on-click #(rf/dispatch [:task/remove-step task index])} "X"]]))
       [:li.active [add-step task]]]]]))

(defn display-products [task-id]
  [:div [:strong "Yields: "] [list-items @(rf/subscribe [:task-yields task-id])
                              :task/remove-product task-id]
   [add-product task-id]])

(defn line-item-editor [task submit]
  (let [item (reagent/atom "")
        qty (reagent/atom 1)
        units @(rf/subscribe [:units])
        unit (reagent/atom (key (first units)))]
    (fn [task submit] 
      [:div.blue-panel
       [:form {:on-submit #(do (.preventDefault %)
                               (rf/dispatch [submit task @item @qty @unit]))}
        [:input  {:type :number 
                  :name "qty" 
                  :value @qty
                  :on-change #(reset! qty (-> % .-target .-value int))}]
        [:select 
         {:on-change #(reset! unit (-> % .-target .-value))}
         (doall
          (for [[id unit] units]
            [:option {:value id} (:name unit)]))]
        [:select
         {:on-change #(reset! item (-> % .-target .-value))}
         (doall
          (for [[id item] @(rf/subscribe [:items])]
            [:option {:value id}
             (when (= (:name item) "each") {:selected "selected"})
             (:name item)]))]
        [:button "+"]]])))

(defn modal-button [title icon child]
 [:button
  {:title title
   :on-click #(do (.preventDefault %)  
                  (rf/dispatch [:modal {:show? true
                                        :child child
                                        :size :small}]))} icon])

(defn create-item [name]
  (let [name (reagent/atom name)
        description (reagent/atom "")
        tags (reagent/atom #{})]
    (fn [name]
      [:div
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
                                :on-change #(reset! description 
                                                    (-> % .-target .-value))}]]]
        [:row
         [:label "Tags"]]
        [:button {:on-click #(do (.preventDefault %)
                                 (rf/dispatch [:item/new @name @description @tags])
                                 (reset! name "")
                                 (reset! description "")
                                 (reset! tags #{}))}
         "Create Item"]]])))

(defn task-table [recipe-id]
  (fn [recipe-id]
    (let [tasks @(rf/subscribe [:recipe-task-list recipe-id])]
      [:table#tasks
       [:thead
        [:tr
         (doall
          (for [h ["Items" "Steps"]]
            [:th h]))]]
       [:tbody
        (doall
         (for [task tasks]
           [:tr 
            [:td 
             [:div [:b "Equipment:"] 
              [modal-button "Add Equipment" "+"
               [line-item-editor task :task/add-equipment]]]
             [list-items @(rf/subscribe [:task-equipment-line-items task])
              :task/remove-equipment task]
             [:div [:b "Ingredients:"]
              [modal-button "Add Ingredient" "+"
               [line-item-editor task :task/add-ingredient]]]
             [list-items @(rf/subscribe [:task-ingredients-line-items task])
              :task/remove-ingredient task]
             [:div [:b "Optional Items:"]
              [modal-button "Add Optional Item" "+"
               [line-item-editor task :task/add-optional]]]
              [list-items @(rf/subscribe [:task-optional-line-items task])
               :task/remove-optional task]]
            [:td [display-steps task]
             (display-products task)]]))]]))) 

(comment
  (defn remove-item
    [task-id item-id]
    [:div.garbage-bin 
     {:on-click #(rf/dispatch [:task/remove-item task-id item-id])}]))


(defn main-panel []
  (let [recipe-id (rf/subscribe [:loaded-recipe])
        name (rf/subscribe [:recipe-name @recipe-id])
        description (rf/subscribe [:recipe-description @recipe-id])]
    [:div
     [modal]
     (header)
     (topnav)
     [:div.row
      [:div.column.left 
       [recipe-search]
       ;[:div "Create New Item:"[modal-button "New Item" "+" [create-item ""]]]
       ]
      [:div.column.middle
       [:h1 [inline-editor @name
             #(rf/dispatch [:recipe/update-name @recipe-id %])]]
       [:h2 [inline-editor @description
              #(rf/dispatch [:recipe/update-description @recipe-id %])]]
       [:div [tag-editor :recipe-tags :recipe/remove-tag :recipe/save-tag @recipe-id]]
       [:div [task-table @recipe-id]]
       ]
      [:div.column.right
       [:div (prn-str @(rf/subscribe [:items]))]
       [:hr]
       [:div (prn-str @(rf/subscribe [:recipe @recipe-id]))]
       [:hr]
       [:div (prn-str @(rf/subscribe [:tasks]))]
       [:hr]
       [:div (prn-str @(rf/subscribe [:units]))]
       ]]]))

 (when-some [el (js/document.getElementById "scratch-views")]
    (defonce _init (rf/dispatch-sync [:initialize]))
    (reagent/render [main-panel] el))

