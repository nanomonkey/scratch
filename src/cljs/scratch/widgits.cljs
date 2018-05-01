
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

(defn display-items [search-string source action]
  (let [items @(rf/subscribe [source])] 
    (fn [search])
    [:center
     (for [item items]
       ;; regular expression to see if the search string matches the item name
       (if (or (re-find (re-pattern (str "(?i)" search-string)) (:name item))
               (= "" search-string))
         ^{ :key (.indexOf items item)}
         [:div
          [:div [:a {:href "#"
                     :on-click #(do
                                  (.preventDefault %)
                                  (apply action (:id item)))} 
                 (:name item)]]]))]))


(defn item-search [source action create]
  (let [search-string (reagent/atom "")]
    (fn [source action create]
      [:div
       [:input.form-control {:type "text"
                             :placeholder "add item"
                             :value @search-string
                             :on-change #(reset! search-string (-> % .-target .-value))}]
       [:button {:on-click #(do (.preventDefault %)
                                (let [id (rf/dispatch [create search-string "" #{} []])]
                                  (apply action id)))} "+"]
       (when (< 1 (count @search-string))
         [display-items @search-string source action])])))


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

(defn add-product [task]
  (let [search-string (reagent/atom "")]
    (fn [task]
      [:div
       [:p {:class "center"} "Add Product" ]
       [:button {:on-click #(doall (.preventDefault %)
                                (let [item-id @(rf/dispatch [:new-item @search-string "" #{} []])]
                                  (rf/dispatch [:task/add-product task item-id 1 "u1"])))} "+"]
       [:input.form-control {:type "text"
                             :placeholder "Filter names"
                             :value @search-string
                             :on-change #(reset! search-string (-> % .-target .-value))}]
       (when (< 1 (count @search-string))
         (let [items @(rf/subscribe [:item-names])] 
           [:center
            (for [item items]
              ;; regular expression to see if the search string matches the recipe name
              (if (or (re-find (re-pattern (str "(?i)" @search-string)) (:name item))
                      (= "" @search-string))
                ^{ :key (.indexOf items item)}
                [:div
                 [:div [:a {:href "#"
                            :on-click #(do
                                         (.preventDefault %)
                                         (rf/dispatch 
                                          [:task/add-product task (:id item) 1 "u1"])
                                         (reset! search-string ""))} 
                        (:name item)]]]))]))])))

;; Recipe Search
(defn recipe-search []
  (let [search-string (reagent/atom "")]
    (fn []
      [:div
       [:p {:class "center"} "Recipes" ]
       [:button {:on-click #(do (.preventDefault %)
                                (let [recipe-id (rf/dispatch [:new-recipe search-string "" #{} []])]
                                  (rf/dispatch [:load-recipe recipe-id])))} "+"]
       [:input.form-control {:type "text"
                             :placeholder "Filter names"
                             :value @search-string
                             :on-change #(reset! search-string (-> % .-target .-value))}]
       (when (< 1 (count @search-string))
         (let [recipes @(rf/subscribe [:recipe-names])] 
           [:center
            (for [recipe recipes]
              ;; regular expression to see if the search string matches the recipe name
              (if (or (re-find (re-pattern (str "(?i)" @search-string)) (:name recipe))
                      (= "" @search-string))
                ^{ :key (.indexOf recipes recipe)}
                [:div
                 [:div [:a {:href "#"
                            :on-click #(do
                                         (.preventDefault %)
                                         (rf/dispatch [:load-recipe (:id recipe)])
                                         (reset! search-string ""))} 
                        (:name recipe)]]]))]))])))


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
