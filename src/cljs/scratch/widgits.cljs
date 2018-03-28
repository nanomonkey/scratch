(ns scratch.widgets
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]))

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
  "adds a tag to a recipient in the database using the "
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


(defn display-recipes [search-string]
  (let [recipes @(rf/subscribe [:recipe-names])] 
    [:center
     (for [recipe recipes]
       ;; regular expression to see if the search string matches the recipe name
       (if (or (re-find (re-pattern (str "(?i)" search-string)) (:name recipe))
               (= "" search-string))
         ^{ :key (.indexOf recipes recipe)}
         [:div
          [:div (:name recipe) (:id recipe)]]))]))


(defn recipe-search []
  (let [search-string (reagent/atom "")]
    (fn []
      [:div
       [:p {:class "center"} "Filter recipes" ]
       [:input.form-control {
                             :type "text"
                             :placeholder "Filter names"
                             :value @search-string
                             :on-change #(reset! search-string (-> % .-target .-value))}]   
       [display-recipes @search-string]])))


(comment
  ;; available css

  [:div.arrow_box "text for arrow box"]
  [:div.blue-panel "text for blue panel"]
  [:div.white-panel "text for white panel"]
  [:div.help-text "help text"]
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
