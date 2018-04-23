
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
(defn tag-editor [source delete save id]
  "adds a tag to a recipient in the database using the
   subscription, remove and save handlers"
  (let [s (reagent/atom "")
        k (reagent/atom "")]
    (fn [source remove save id]
      [:div#tags
       [:span
        "Tags: "
        (doall
         (for [tag @(rf/subscribe [source id])]
           [:div#tag {:style {:display :inline-block
                              :background-color :yellow
                              :color :blue
                              :margin-right "8px"}}
            tag
            [:a {:href "#"
                 :style {:margin-left "4px"}
                 :on-click (fn [e] 
                             (.preventDefault e)
                             (rf/dispatch [remove id tag]))}
             [:sup "x"]]]))]
       [:span
        [:input {:type :text
                 :value @s
                 :on-change #(reset! s (-> % .-target .-value))
                 :on-key-up (fn [e]
                              (reset! k (-> e .-key))
                              (when (or (= " " (-> e .-key))
                                        (= "Enter" (-> e .-key)))
                                (rf/dispatch [save id (.trim @s)])
                                (reset! s "")))}]]])))

(defn find-or-add [items add-event create-event]
  "text field that searches through items, if one is found add it, otherwise create a new item with the text provided"
)

;; Datalist
(defn data-list [name options]
  [:div
   [:label name]
   [:input {:list name}]
   [:datalist {:id name}
    (for [[item id] options]
      [:option {:value id} item])]])


(defn item-selector []
  (data-list "select-item" @(rf/subscribe [:item/name-id])))

;;Work in progress:

(defn display-recipes [search-string]
  (let [recipes @(rf/subscribe [:recipe-names])] 
    [:center
     (for [recipe recipes]
       ;; regular expression to see if the search string matches the recipe name
       (if (or (re-find (re-pattern (str "(?i)" search-string)) (:name recipe))
               (= "" search-string))
         ^{ :key (.indexOf recipes recipe)}
         [:div
          [:div [:a {:href "#"
                     :on-click #(do
                                  (.preventDefault %)
                                  (rf/dispatch [:load-recipe (:id recipe)]))} 
                 (:name recipe)]]]))]))


(defn recipe-search []
  (let [search-string (reagent/atom "")]
    (fn []
      [:div
       [:p {:class "center"} "Filter recipes" ]
       [:input.form-control {:type "text"
                             :placeholder "Filter names"
                             :value @search-string
                             :on-change #(reset! search-string (-> % .-target .-value))}]
       [:button {:on-click #(do (.preventDefault %)
                                (let [recipe-id (rf/dispatch [:new-recipe search-string "" #{} []])]
                                  (rf/dispatch [:load-recipe recipe-id])))} "+"]
       (when (< 1 (count @search-string))
         [display-recipes @search-string])])))


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
