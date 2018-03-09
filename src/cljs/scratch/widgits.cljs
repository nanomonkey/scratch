(ns scratch.widgets
  (require [re-frame.core :as rf]))

;; Markdown
(defonce converter (new js/showdown.Converter))

(defn ->html [s]
  (.makeHtml converter s))

(defn markdown-section [s]
  [:div
    {:dangerouslySetInnerHTML {:__html (->html s)}}])


;; Inline Editor

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
           [:input {:type :text 
                    :value (:text @s)
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


;; Tag Editor

(defn tag-editor [recipe-id]
  (let [s (reagent/atom "")
        k (reagent/atom "")]
    (fn []
      [:div#tags
       [:span
        "Tags: "
        (doall
         (for [tag @(rf/subscribe [:recipe-tags recipe-id])]
           [:div#tag {:style {:display :inline-block
                              :background-color :yellow
                              :color :blue
                              :margin-right "8px"}}
            tag
            [:a {:href "#"
                 :style {:margin-left "4px"}
                 :on-click (fn [e] 
                             (.preventDefault e)
                             (rf/dispatch [:remove-tag recipe-id tag]))}
             [:sup "x"]]]))]
       [:span
        [:input {:type :text
                 :value @s
                 :on-change #(reset! s (-> % .-target .-value))
                 :on-key-up (fn [e]
                              (reset! k (-> e .-key))
                              (when (or (= " " (-> e .-key))
                                        (= "Enter" (-> e .-key)))
                                
                                (rf/dispatch [:save-tag recipe-id (.trim @s)])
                                (reset! s "")))}]]])))


;; Work in progress on Item search field

(defn friend-source [text]
  (filter
    #(-> % (.toLowerCase %) (.indexOf text) (> -1))
    ["Alice" "Alan" "Bob" "Beth" "Jim" "Jane" "Kim" "Rob" "Zoe"]))

(comment
  (defn item-search []
    [:div
     (row
      [:div {:field           :typeahead
             :id              :item-search
             :data-source     @(rf/subscribe :item-source)
             :input-placeholder "Item"
             :input-class     "form-control"
             :list-class      "typeahead-list"
             :item-class      "typeahead-item"
             :highlight-class "highlighted"}])]))


(comment ;;examples of available css

[:div.arrow_box "crazy arrow box"]
[:div.blue-panel "crazy blue panel"]
[:div.white-panel "crazy white panel"]
[:div#task
 [:div.steps-indicator
  [:div.connector]
  [:div.connector.complete]
  [:ol.steps
   [:li.complete [:strong "completed"] " step"]
   [:li.active "not complete"]
   [:li.active "not complete"]
   [:li.inactive "inactive"]
   [:li.warning "warning"]
   [:li.active "last one"]]]]
)
