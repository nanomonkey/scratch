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
                                     modal
                                     modal-button
                                     full-modal]]
            [goog.string :as gstring]
            [goog.string.format]))


(defn header []
  [:div.header "Made From Scratch"])

(defn topnav-old []
  [:div.topnav
   [:a {:href "#1"} "One"]
   [:a {:href "#2"} "Two"]])

(defn topnav []
  [:nav  [:ul [:li [recipe-search]]
          [:li
           [:a {:href "inventory"} "Inventory"]]
          [:li
           [:a {:href "items"} "Items"]]
          [:li
           [:a {:href "units"} "Units"]]]])

(defn display-line-item [line-item]
  "unpacks dictionary with :unit :item and :qty into readable string"
  (let [qty (:qty line-item)
        unit (rf/subscribe [:unit-abbrev (:unit line-item)])
        item (rf/subscribe [:item-name (:item line-item)])]
    (goog.string/format "%f %s - %s" qty @unit @item)))

(defn list-items [items remove-event task]
  (fn [items remove-event task]
    [:ul.items
     (for [i items]
       [:li {:key (:item i)} [:span.removable (display-line-item i )]
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
       [:input {:type :textarea
                :value @s
                :on-change #(reset! s (-> % .-target .-value))}]])))


(defn put-before [items pos item]
  (let [items (remove #{item} items)
        head (take pos items)
        tail (drop pos items)]
    (concat head [item] tail)))
         
(defn display-steps [task steps]
  (let [s (reagent/atom {:order (range (count steps))})]
    (fn [task steps]
      [:div (prn-str steps)
       [:div.steps-indicator
        [:div.connector]
        [:div.connector.complete]
        [:ol.steps  
         (for [[i pos] (map vector (:order @s) (range))]
           [:li.active {:key i
                        :style {:border (when (= i (:drag-index @s))
                                          "1px dotted orange")}
                        :draggable true
                        :on-drag-start #(swap! s assoc :drag-index i)
                        :on-drag-over (fn [e]
                                        (.preventDefault e)
                                        (swap! s assoc :drag-over pos)
                                        (swap! s update :order put-before pos (:drag-index @s)))
                        :on-drag-leave #(swap! s assoc :drag-over :nothing)
                        :on-drag-end (fn []
                                       (swap! s dissoc :drag-over :drag-index)
                                       (rf/dispatch 
                                        [:task/update-all-steps task [vec (map steps (:order @s))]]))}
            [inline-editor (get steps i) {:on-update #(rf/dispatch 
                                                       [:task/update-step task % pos])
                                          :on-remove (fn [] 
                                                       (rf/dispatch 
                                                        [:task/remove-step task pos]))}]
            [:button.hidden {:title "Remove" 
                             :on-click #(do
                                          (.preventDefault %)
                                          (rf/dispatch [:task/remove-step task pos]))} "X"]])
         [:li.active [add-step task]]]]])))

(defn display-products [task-id]
  [:div [:strong "Yields: "] 
   [list-items @(rf/subscribe [:task-yields task-id])
    :task/remove-product task-id]
   [add-product task-id]])

(defn line-item-editor [task submit]
  (let [items @(rf/subscribe [:items])
        item (reagent/atom (key (first items)))
        qty (reagent/atom 1)
        units @(rf/subscribe [:units])
        unit (reagent/atom (key (first units)))]
    (fn [task submit] 
      [:div.white-panel
       [:form {:on-submit #(do (.preventDefault %)
                               (rf/dispatch [submit task @item @qty @unit]))}
        [:div.row
         [:label "Quantity:"]
         [:input  {:type :number 
                   :value @qty
                   :size "4"
                   :on-change #(reset! qty (-> % .-target .-value int))}]]
        [:div.row
         [:label "Unit:"]
         [:select 
          {:on-change #(reset! unit (-> % .-target .-value))}
          (doall
           (for [[id unit] units]
             [:option {:value id} (:name unit)]))]]
        [:div.row
         [:label "Item:"]
         [:select
          {:on-change #(reset! item (-> % .-target .-value))}
          (doall
           (for [[id item] items]
             [:option {:value id}
              (:name item)]))]]
        [:button "+Item"]]])))


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

(defn add-task [recipe-id]
  (let [name (reagent/atom "new task")]
    (fn [recipe-id]
      [:button {:on-click #(do (.preventDefault %)
                               (rf/dispatch [:recipe/new-task recipe-id @name]))}
       "+ Task"])))

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
             [:div
              [modal-button "Add Equipment" "Equipment:"
               [line-item-editor task :task/add-equipment]]]
             [list-items @(rf/subscribe [:task-equipment-line-items task])
              :task/remove-equipment task]
             [:div
              [modal-button "Add Ingredient" "Ingredients:"
               [line-item-editor task :task/add-ingredient]]]
             [list-items @(rf/subscribe [:task-ingredients-line-items task])
              :task/remove-ingredient task]
             [:div
              [modal-button "Add Optional Item" "Optional:"
               [line-item-editor task :task/add-optional]]]
              [list-items @(rf/subscribe [:task-optional-line-items task])
               :task/remove-optional task]]
            [:td#task
             [:h2 [inline-editor @(rf/subscribe [:task-name task]) 
                   {:on-update #(rf/dispatch [:task/update-name task %])}]]
             [display-steps task @(rf/subscribe [:task-steps task])]
             (display-products task)]]))
        [:tr [add-task recipe-id]]]]))) 

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
       ;;[recipe-search]
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

