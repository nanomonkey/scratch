(ns scratch.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [scratch.subs :as subs]
            [goog.string :as gstring]
            [goog.string.format]))

(defn inline-editor [txt on-change]
  (let [s (reagent/atom {})]
    (fn [txt on-change]
      [:span
       (if (:editing? @s)
         [:form {:on-submit #(do
                               (.preventDefault %)
                               (swap! s dissoc :editing?)
                               (when on-change
                                 (on-change (:text @s))))}
           [:input {:type :text :value (:text @s)
                    :on-change #(swap! s assoc 
                                     :text (-> % .-target .-value))}]
          [:button "Save"]
          [:button {:on-click #(do
                                 (.preventDefault %)
                                 (swap! s dissoc :editing?))}
           "Cancel"]]
         [:span
           {:on-click #(swap! s assoc
                            :editing? true
                            :text txt)}
          [:a {:href "#"}
           [:sup "✎"]] txt])])))

(defn tag-editor [recipe-id]
  (let [s (reagent/atom "")
        k (reagent/atom "")]
    (fn []
      [:div
       [:span
        "Tags: "
        (doall
         (for [tag @(re-frame/subscribe [:recipe-tags-sorted recipe-id])]
           [:div {:style {:display :inline-block
                             :background-color :yellow
                             :color :blue
                             :margin-right "8px"}}
            tag
            [:a {:href "#"
                 :style {:margin-left "4px"}
                 :on-click (fn [e]
                             (.preventDefault e)
                             (re-frame/dispatch [:remove-tag recipe-id tag]))}
             [:sup "x"]]]))]
       [:span
        [:input {:type :text
                 :value @s
                 :on-change #(reset! s (-> % .-target .-value))
                 :on-key-up (fn [e]
                              (reset! k (-> e .-key))
                              (when (or (= " " (-> e .-key))
                                        (= "Enter" (-> e .-key)))
                                
                                (re-frame/dispatch [:save-tag recipe-id (.trim @s)])
                                (reset! s "")))}]]])))

(defn display-line-item [line-item]
  (let [qty (:qty line-item)
        unit (re-frame/subscribe [:unit-abbrev (:unit line-item)])
        item (re-frame/subscribe [:item-name (:item line-item)])]
    (goog.string/format "%i%s - %s" qty @unit @item)))

(defn task-table [recipe-id]
  (fn [recipe-id]
    (let [tasks @(re-frame/subscribe [:recipe-task-list recipe-id])]
      [:table#tasks
       [:tr
        (doall
         (for [h 
               ["Equipment" "Ingredient" "Procedure"]]
           [:th h]))] 
       (doall
        (for [task tasks]
          [:tr 
           [:td
            [:ul
             (for [e  @(re-frame/subscribe [:task-equipment-line-items task])]
               [:li e (display-line-item e)])]]
           [:td 
            [:ul
             (for [i @(re-frame/subscribe [:task-ingredients-line-items task])]
               [:li (display-line-item i)])]]
           [:td @(re-frame/subscribe [:task-procedure task])]]
          ))])))  

(defn item-editor []
  (let [s (reagent/atom {})]
    [:span 
     [:form {:on-submit #(do (.preventDefault %))}
      [:input {:type :number :name "qty" :value (:qty @s)}]
      [:select 
       (doall
        (for [[id unit] @(re-frame/subscribe [:units])]
          [:option {:value id} (:name unit)]))]
      [:select
       (doall
        (for [[id item] @ (re-frame/subscribe [:items])]
          [:option {:value id} (:name item)]))]]]))

(defn main-panel []
  (let [name (re-frame/subscribe [:recipe-name "r1"])
        description (re-frame/subscribe [:recipe-description "r1"])]
    [:div
     [:h2 [inline-editor @name
           #(re-frame/dispatch [:update-name "r1" %])]]
     [:div [inline-editor @description
            #(re-frame/dispatch [:update-description "r1" %])]]
     [:div [tag-editor "r1"]]
     [:div [task-table "r1"]]
     [:div [item-editor]]
     [:div [:button {:on-click #(do
                                  (.preventDefault %))} "+Task"]]]))

 (when-some [el (js/document.getElementById "scratch-views")]
    (defonce _init (re-frame/dispatch-sync [:initialize]))
    (reagent/render [main-panel] el))

(comment 
;; https://benincosa.com/?p=3594
(defn display-names
  "Displays APIs that match the search string"
  [search-string]
    [:div {:class "container-fluid"}
     [:center
      (for [person-name names]
        ;; tricky regular expression to see if the search string matches the name
        (if (or (re-find (re-pattern (str "(?i)" search-string)) person-name)
                (= "" search-string))
          ^{ :key (.indexOf names person-name)}
            [:div {:class "col-sm-4"}
            [:div {:class "panel panel-default"}
              [:div {:class "panel-heading"} person-name]]]))]])

)
