(ns scratch.views
  (:require [re-frame.core :as rf]
            [reagent.core :as r] 
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
                                     full-modal 
                                     display-line-item
                                     display-duration
                                     display-rational
                                     parse-rational
                                     parse-duration]]
            [goog.string :as gstring]))


(defn header []
  [:div.header "Made From Scratch"])

(defn topnav []
  [:nav  [:ul [:li [recipe-search]]
          [:li [:a {:href "inventory"} "Inventory"]]
          [:li [:a {:href "suppliers"} "Suppliers"]]
          [:li [:a {:href "schedule"} "Schedule"]]]])

(defn list-items [items remove-event task]
  (fn [items remove-event task]
    [:ul.items
     (for [i items]
       [:li {:key (:item i)} [:span.removable (display-line-item i )]
        [:button.hidden
         {:title "Remove"
          :on-click #(do (.preventDefault %)
                         (rf/dispatch [remove-event task (:item i)]))} "X"]])]))

(defn add-step [task {:keys [on-add]}]
  (let [s (r/atom "")]
    (fn [task]
      [:form {:on-submit #(do
                            (.preventDefault %)
                            (when (> (count @s) 0)
                              (rf/dispatch [:task/add-step task @s]))
                            (reset! s "")
                            (on-add))}
       [:input {:type :textarea
                :value @s
                :on-change #(reset! s (-> % .-target .-value))}]])))

(defn put-before [items pos item]
  (let [items (remove #{item} items)
        head (take pos items)
        tail (drop pos items)]
    (concat head [item] tail)))

(defn display-steps [task]
  (let [steps (rf/subscribe [:task/steps task])
        s (r/atom {:order (range (count @steps))})]
    (fn [task]
      (when (:changed? @s) (reset! s {:order (range (count @steps))}))
      [:div.steps-indicator
       [:div.connector]
       [:div.connector.complete]
       [:ol.steps 
        (doall
         (for [[i pos] (map vector (:order @s) (range))]
           [:li.active {:key i
                        :style {:border (when (= i (:drag-index @s))
                                          "1px dotted orange")}
                        :draggable true
                        :on-drag-start #(swap! s assoc :drag-index i)
                        :on-drag-over (fn [e]
                                        (.preventDefault e)
                                        (swap! s assoc :drag-over pos)
                                        (swap! s update :order 
                                               put-before pos (:drag-index @s)))
                        :on-drag-leave #(swap! s assoc :drag-over :nothing)
                        :on-drag-end (fn []
                                       (swap! s dissoc :drag-over :drag-index)
                                       (rf/dispatch 
                                        [:task/update-all-steps task 
                                         (vec (map @steps (:order @s)))])
                                       (swap! s assoc :changed? true))}
            [inline-editor (get @steps i) {:on-update 
                                           #(do (rf/dispatch 
                                                 [:task/replace-step task % pos])
                                                (reset! s {:order (range (count @steps))}))
                                           :on-remove 
                                           #(do (rf/dispatch 
                                                 [:task/remove-step task pos])
                                                (swap! s assoc :changed? true))}]]))
        [:li.active [add-step task {:on-add #(swap! s assoc :changed? true)}]]]])))

(defn display-products [task-id]
  [:div [:strong "Yields: "] 
   [list-items @(rf/subscribe [:task/yields task-id])
    :task/remove-product task-id]
   [add-product task-id]])

(defn line-item-editor [task submit]
  (let [items @(rf/subscribe [:items])
        item (r/atom (key (first items)))
        qty (r/atom 1)
        units @(rf/subscribe [:units])
        unit (r/atom (key (first units)))]
    (fn [task submit] 
      [:div.white-panel
       [:form {:on-submit #(do (.preventDefault %)
                               (rf/dispatch [submit task @item @qty @unit])
                               (rf/dispatch [:modal {:show? false
                                                     :child nil
                                                     :size :default}]))}
        [:div.row
         [:label "Quantity:"]
         [:input  {:type :number
                   :auto-focus true
                   :value @qty
                   :style {:size "4"}
                   :on-change #(reset! qty (-> % .-target .-value int))}]]
        [:div.row
         [:label "Unit:"]
         [:select 
          {:on-change #(reset! unit (-> % .-target .-value))}
          (doall
           (for [[id unit] units]
             [:option {:key id :value id} (:name unit)]))]]
        [:div.row
         [:label "Item:"]
         [:select
          {:on-change #(reset! item (-> % .-target .-value))}
          (doall
           (for [[id item] items]
             [:option {:value id}
              (:name item)]))]]
        [:button "+ Item"]]])))

(defn create-item [new-name]
  (let [name (r/atom new-name)
        description (r/atom "")
        tags (r/atom #{})]
    (fn [new-name]
      [:div.white-panel
       [:form {:on-submit #(do (.preventDefault %)
                               (rf/dispatch [:item/new @name @description @tags])
                               (reset! name "")
                               (reset! description "")
                               (reset! tags #{})
                               (rf/dispatch [:modal {:show? false
                                                     :child nil
                                                     :size :default}]))}
        [:input {:type "text"
                 :placeholder "Name"
                 :name "item-name"
                 :auto-focus true
                 :value @name
                 :on-change #(reset! name (-> % .-target .-value))}]
        [:div
         [:input {:type "text"
                  :placeholder "Description"
                  :value @description
                  :on-change #(reset! description 
                                      (-> % .-target .-value))}]]
        [tag-editor @tags #(swap! tags disj %) #(swap! tags conj %)]
        [:button "Create Item"]]])))

(defn add-task [recipe-id]
  (let [name (r/atom "new task")]
    (fn [recipe-id]
      [:div [:button.wide {:on-click #(do (.preventDefault %)
                                          (rf/dispatch 
                                           [:recipe/new-task recipe-id @name]))}
             "+ Task"]])))

(defn svg-clock []
  [:svg {:class "icon icon-clock" 
         :width "24"
         :height "28" 
         :viewBox "0 0 24 28" 
         :aria-hidden "true"}
   [:path  {:fill "grey" :d "M14 8.5v7c0 .281-.219.5-.5.5h-5a.494.494 0 0 1-.5-.5v-1c0-.281.219-.5.5-.5H12V8.5c0-.281.219-.5.5-.5h1c.281 0 .5.219.5.5zm6.5 5.5c0-4.688-3.813-8.5-8.5-8.5S3.5 9.313 3.5 14s3.813 8.5 8.5 8.5 8.5-3.813 8.5-8.5zm3.5 0c0 6.625-5.375 12-12 12S0 20.625 0 14 5.375 2 12 2s12 5.375 12 12z"}]])

(defn task-duration [task]
  (let [duration (r/atom (parse-duration @(rf/subscribe [:task/duration task])))
        editing? (r/atom false)]
    (fn [task]
      (if @editing?
        [:form {:on-submit #(do
                              (.preventDefault %)
                              (reset! editing? false)
                              (rf/dispatch [:task/new-duration task @duration]))}
         [:input {:type :number
                  :placeholder "HHH"
                  :auto-focus "true"
                  :value (:hr @duration)
                  :style {:width "3em"}
                  :on-change #(swap! duration assoc :hr (-> % .-target .-value))}]
         ":"
         [:input {:type :number
                  :placeholder "MM"
                  :value (:min @duration)
                  :style {:width "2em"}
                  :min 0
                  :max 59
                  :on-change #(swap! duration assoc :min (-> % .-target .-value))}]
         ";"
         [:input {:type :number
                  :placeholder "SS.SSS"
                  :value (:sec @duration)
                  :style {:width "5em"}
                  :min 0
                  :max 59
                  :step "0.01"
                  :on-change #(swap! duration assoc :sec (-> % .-target .-value))}]
         [:button "✓"]
         [:button {:on-click #(do
                                (.preventDefault %) 
                                (reset! duration 
                                        (parse-duration 
                                         @(rf/subscribe [:task/duration task])))
                                (reset! editing? false))
                   :on-blur #(do
                               (.preventDefault %)
                               (reset! editing? false))}
          "X"]]
        [:button {:style {:border-radius 100}
                  :on-click #(do (.preventDefault %)
                                 (reset! editing? true))}
         [svg-clock]
         (when @duration
           (display-duration @duration))]))))

(defn line-item [submit]
  (let [item-id (r/atom "")
        qty (r/atom {:text ""
                     :value {}
                     :editing? true})
        unit-id (r/atom "")]
    (fn [submit] 
      [:div.white-panel
       [:form {:on-submit #(do (.preventDefault %)
                               (submit @item-id (:value @qty) @unit-id)
                               (rf/dispatch [:modal {:show? false
                                                     :child nil
                                                     :size :default}]))}
        (if (:editing? @qty) 
          [:input  {:type :text
                    :placeholder "quantity (i.e. 2.3 or 5 2/3)"
                    :auto-focus true
                    :value (:text @qty)
                    :style {:border (when (= "non-parsable" (:value @qty)) 
                                      "2px solid red")}
                    :on-change #(do 
                                  (swap! qty assoc :text (-> % .-target .-value))
                                  (swap! qty assoc :value 
                                         (parse-rational (:text @qty))))
                    :on-blur #(do
                                (.preventDefault %)
                                (when (not (= "non-parsable" 
                                              (parse-rational (:text @qty))))
                                  (swap! qty dissoc :editing?)))}]
          [:a.edit {:href "#"
                   :on-click #(swap! qty assoc :editing? true)}
           (display-rational (:value @qty))])
        (if (= @unit-id "")
          [item-search {:placeholder "unit"
                        :source  (rf/subscribe [:unit/source])
                        :add #(reset! unit-id %)}]
          [:a.edit {:href "#"
                           :style {:margin-left "4px"
                                   :alt-text "edit"}
                           :on-click #(reset! unit-id "")}  
           @(rf/subscribe [:unit/abbrev @unit-id])])
        (if (= @item-id "")
          [item-search {:placeholder "item"
                        :create nil
                        :source (rf/subscribe [:item/source])
                        :add #(reset! item-id %)}]
          [:a.edit {:href "#"
                           :style {:margin-left "4px"
                                   :alt-text "edit"}
                           :on-click #(reset! item-id "")}
           @(rf/subscribe [:item/name @item-id])])
        [:button "+ Line Item"]]])))


(defn task-table [recipe-id]
  (fn [recipe-id]
    (let [tasks @(rf/subscribe [:recipe/task-list recipe-id])]
      [:table#tasks
       [:thead
        [:tr ^{:key "header"}
         [:th "Items"] [:th "Steps"]]]
       [:tbody
        (doall
         (for [task tasks]
           [:tr ^{:key (:id task)} 
            [:td ^{:key (str (:id task) "items")}
             [:div ^{:key (str (:id task) "equipment")}
              [modal-button "Add Equipment" "Equipment:"
               [line-item #(rf/dispatch [:task/add-equipment task %1 %2 %3])]
               "equipment-qty"]]
             [list-items @(rf/subscribe [:task/equipment-line-items task])
              :task/remove-equipment task]
             [:div ^{:key (str (:id task) "ingredients")}
              [modal-button "Add Ingredient" "Ingredients:"
               [line-item #(rf/dispatch [:task/add-ingredient task %1 %2 %3])]
               "ingredient-qty"]]
             [list-items @(rf/subscribe [:task/ingredients-line-items task])
              :task/remove-ingredient task]
             [:div ^{:key (str (:id task) "optional")}
              [modal-button "Add Optional Item" "Optional:"
               [line-item #(rf/dispatch [:task/add-optional task %1 %2 %3])]
               "optional-qty"]]
              [list-items @(rf/subscribe [:task/optional-line-items task])
               :task/remove-optional task]]
            [:td#task ^{:key (str (:id task) "steps")}
             [:h2 [inline-editor @(rf/subscribe [:task/name task]) 
                   {:on-update #(rf/dispatch [:task/update-name task %])}]]
             [task-duration task]
             [display-steps task]
             (display-products task)]]))
        [:tr ^{:key "Add_Task_row"}
         [:td ^{:key "Add_Task"}
          [add-task recipe-id]]]]]))) 

(defn main-panel []
  (let [recipe-id (rf/subscribe [:loaded-recipe])
        name (rf/subscribe [:recipe/name @recipe-id])
        description (rf/subscribe [:recipe/description @recipe-id])]
    [:div
     [modal]
     (topnav)
     [:div.row
      [:div.column.left
       [:div [modal-button "Create Item" "Create New Item" [create-item ""] "item-name"]]
       ]
      [:div.column.middle
       [:h1 [inline-editor @name 
             {:on-update #(rf/dispatch [:recipe/update-name @recipe-id %])}]]
       [:h2 [inline-editor @description 
             {:on-update #(rf/dispatch [:recipe/update-description @recipe-id %])}]]
       [:div [tag-editor 
              @(rf/subscribe [:recipe/tags @recipe-id]) 
              #(rf/dispatch [:recipe/remove-tag @recipe-id %]) 
              #(rf/dispatch [:recipe/save-tag @recipe-id %])]]
       [:div [task-table @recipe-id]]
       ]
      [:div.column.right
       
       ]]]))

 (when-some [el (js/document.getElementById "scratch-views")]
    (defonce _init (rf/dispatch-sync [:initialize]))
    (r/render [main-panel] el))

