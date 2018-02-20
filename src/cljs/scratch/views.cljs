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
          [:sup "✎"] txt])])))

(defn tag-editor []
  (let [s (reagent/atom "")
        k (reagent/atom "")]
    (fn []
      [:div
       [:span
        "Tags: "
        (doall
         (for [tag @(re-frame/subscribe [:tags])]
           [:div {:style {:display :inline-block
                             :background-color :yellow
                             :color :blue
                             :margin-right "8px"}}
            tag
            [:a {:href "#"
                 :style {:margin-left "4px"}
                 :on-click (fn [e]
                             (.preventDefault e)
                             (re-frame/dispatch [:remove-tag tag]))}
             [:sup "x"]]]))]
       [:span
        [:input {:type :text
                 :value @s
                 :on-change #(reset! s (-> % .-target .-value))
                 :on-key-up (fn [e]
                              (reset! k (-> e .-key))
                              (when (or (= " " (-> e .-key))
                                        (= "Enter" (-> e .-key)))
                                
                                (re-frame/dispatch [:save-tag (.trim @s)])
                                (reset! s "")))}]]])))

(defn get-name [line_item key]
  (let [i (key line_item)]
    (:name (i (first (keys i))))))

(defn display-line-item [line-item]
  (gstring/format "%i %s %s" (:qty line-item) 
                  (get-name line-item :unit) 
                  (get-name line-item :item)))

(defn task-table [tasks]
  (fn [tasks]
    [:table {:style {:border "solid grey 1px" }}
     [:tr
      (doall
       (for [h ["Equipment" "Ingredients" "Instructions"]]
         [:th h]))] 
     (doall
      (for [[task-id task] tasks]
        (list
         [:tr {:key task-id}
          [:td
           [:ul
            (for [e (:equipment task)]
              [:li (display-line-item e)])]]
          [:td 
           [:ul
            (for [i (:items task)]
              [:li (display-line-item i)])]] 
          [:td [:strong (:name task)] (:instructions task)]])))]))  

(defn main-panel []
  (let [name (re-frame/subscribe [::subs/name])
        description (re-frame/subscribe [::subs/description])]
    [:div
     [:h2 [inline-editor @name
           #(re-frame/dispatch [:update-name %])]]
     [:div [inline-editor @description
            #(re-frame/dispatch [:update-description %])]]
     [:div [tag-editor]]
     [:div [task-table @ (re-frame/subscribe [:tasks])]]]))

 (when-some [el (js/document.getElementById "scratch-views")]
    (defonce _init (re-frame/dispatch-sync [:initialize]))
    (reagent/render [main-panel] el))

(comment
 (unit-name {"u123" {:id "u123" :name "each"}} )
)
