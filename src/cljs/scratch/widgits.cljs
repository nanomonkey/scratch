(ns scratch.widgets
  (:require [re-frame.core :as rf]
            [reagent.core :as reagent]))

;; Markdown
(defonce converter (new js/showdown.Converter))

(defn ->html [s]
  (.makeHtml converter s))

(defn markdown-section [s]
  [:span
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
          [:button "✓"]
          [:button {:on-click #(do
                                 (.preventDefault %)
                                 (swap! s dissoc :editing?))}
           "X"]]
         [:div [:span.removable
                {:on-click #(swap! s assoc
                                   :editing? true
                                   :text txt)}
                txt][:span.hidden [:a {:href "#"}
                                   [:sup "✎"]]]])])))


;; Mark-down editor
(defn markdown-editor [txt on-change]
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
            [:sup "✎"]] [:span (markdown-section txt)]])])))

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
    (fn [search-string source action]
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
                                    (action (:id item)))} 
                   (:name item)]]]))])))


(defn item-search [source action create]
  (let [search-string (reagent/atom "")]
    (fn [source action create]
      [:div
       [:input.form-control {:type "text"
                             :placeholder "add item"
                             :value @search-string
                             :on-change #(reset! search-string (-> % 
                                                                   .-target 
                                                                   .-value))}]
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
  "data-lists don't work in Safari..."
  (data-list "select-item" @(rf/subscribe [:item/name-id])))

(defn add-product [task]
  (let [search-string (reagent/atom "")
        key (reagent/atom "")
        items @(rf/subscribe [:item-names])]
    (fn [task]
      [:div
       [:input.form-control {:type "text"
                             :placeholder "add product"
                             :value @search-string
                             :on-change #(reset! search-string 
                                                 (-> % .-target .-value))}]
       [:button {:on-click 
                 #(doall 
                   (.preventDefault %)
                   (let [item-id (rf/dispatch [:item/new @search-string "" #{} []])]
                     (rf/dispatch [:task/add-product task item-id 1 "u1"])))} "+"]
       (when (< 1 (count @search-string)) 
         (for [item items]
           ;; regular expression to see if the search string matches the name
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
                     (:name item)]]])))])))

;; Recipe Search
(defn recipe-search []
  (let [search-string (reagent/atom "")]
    (fn []
      [:nav 
       [:ul
        [:li
         [:button {:on-click #(do (.preventDefault %)
                                  (rf/dispatch [:recipe/new @search-string])
                                  (reset! search-string ""))} "+"]
         [:input.form-control {:type "text"
                               :placeholder "Load Recipe"
                               :value @search-string
                               :on-change #(reset! search-string (-> % 
                                                                     .-target 
                                                                     .-value))}]
         (when (< 1 (count @search-string))
           (let [recipes @(rf/subscribe [:recipe-names])] 
             [:ul
              (for [recipe recipes]
                (if (or (re-find (re-pattern (str "(?i)" @search-string)) (:name recipe))
                        (= "" @search-string))
                  ^{ :key (.indexOf recipes recipe)}
                  [:li [:a {:href "#"
                            :on-click #(do
                                         (.preventDefault %)
                                         (rf/dispatch [:load-recipe (:id recipe)])
                                         (reset! search-string ""))} 
                        (:name recipe)]]))]))]]])))


;; Modals
(defn modal-panel
  [{:keys [child size show?]}]
  [:div {:class "modal-wrapper"}
   [:div {:class "modal-backdrop"
          :on-click (fn [event]
                      (do
                        (rf/dispatch [:modal {:show? (not show?)
                                           :child nil
                                           :size :default}])
                        (.preventDefault event)
                        (.stopPropagation event)))}]
   [:div {:class "modal-child"
          :style {:width (case size
                           :extra-small "15%"
                           :small "30%"
                           :large "70%"
                           :extra-large "85%"
                           "50%")}} child]])

(defn modal []
  (let [modal (rf/subscribe [:modal])]
    (fn []
      [:div
       (if (:show? @modal)
         [modal-panel @modal])])))


(defn- close-modal []
  (rf/dispatch [:modal {:show? false :child nil}]))

(defn modal-button [title icon child]
 [:button
  {:title title
   :on-click #(do (.preventDefault %)  
                  (rf/dispatch [:modal {:show? true
                                        :child child
                                        :size :small}]))} icon])

(defn full-modal [title body footer]
  [:div {:class "modal-content"}
   [:div {:class "modal-header"}
    [:button {:type "button" :title "Cancel"
              :class "close"
              :on-click #(close-modal)}
     [:i {:class "material-icons"} "close"]]
    [:h4 {:class "modal-title"} title]]
   [:div {:class "modal-body"} footer]
   [:div {:class "modal-footer"} footer]])

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
