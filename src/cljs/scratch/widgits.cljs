(ns scratch.widgets
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [goog.string.format]))

;; Markdown
(defonce converter (new js/showdown.Converter))

(defn ->html [s]
  (.makeHtml converter s))

(defn markdown-section [s]
  [:span
   {:dangerouslySetInnerHTML {:__html (->html s)}}])

;; Inline Editor
(defn inline-editor [txt {:keys [on-update on-remove]}]
  (let [s (r/atom {})]
    (fn [txt event-handlers]
      [:span
       (if (:editing? @s)
         [:form {:on-submit #(do
                               (.preventDefault %)
                               (swap! s dissoc :editing?)
                               (when on-update
                                 (on-update (:text @s))))}
           [:input {:type :text 
                    :value (:text @s)
                    :on-change #(swap! s assoc 
                                     :text (-> % .-target .-value))}]
          [:button "✓"]
          [:button {:on-click #(do
                                 (.preventDefault %)
                                 (swap! s dissoc :editing?))
                    :on-blur #(do
                             (.preventDefault %)
                             (swap! s dissoc :editing?))}
           "X"]]
         [:span [:span.removable
                {:on-click #(swap! s assoc
                                   :editing? true
                                   :text txt)}
                 [:a.edit {:href "#"
                           :style {:margin-left "4px"
                                   :alt-text "edit"}
                           :on-click (fn [e]
                                       (.preventDefault e)
                                       #(swap! s assoc
                                               :editing? true
                                               :text txt))}
                 txt]
                 (when on-remove
                   [:a.trash {:href "#"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (on-remove))
                              :style {:font-size "small"}} "🗑"])]])])))

;; Tag Editor
(defn tag-editor [source remove save id]
  "adds a tag to a recipient in the database using the
   subscription, remove and save handlers"
  (let [s (r/atom "")
        k (r/atom "")]
    (fn [source remove save id]
      [:div#tags
       [:span
        "Tags: "
        (doall
         (for [tag @(rf/subscribe [source id])]
           [:div#tag {:key tag
                      :style {:display :inline-block
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

(defn has-tag? [col tag]
  (filter #(contains? (:tags %) tag) col))


(defn item-search [{:keys [items placeholder create find-by-name add-new]}]
  (let [search-string (r/atom "")]
    (fn [props]
      [:div
       [:input.search {:type "text"
                       :placeholder placeholder
                       :value @search-string
                       :on-change #(reset! search-string 
                                           (-> % .-target .-value))}]
       [:button {:style {:position "relative"
                         :left "-22px"
                         :border "none"
                         :font-size "8"
                         :background-color "white"}
                 :on-click #(doall
                             (.preventDefault %)
                             (reset! search-string ""))} "X"]
       [:button {:on-click 
                 #(do 
                    (.preventDefault %)
                    (create @search-string)
                    (if-let [item-id (find-by-name @search-string)]
                      (add-new item-id))
                    (reset! @search-string ""))} "+"]
       (when (< 1 (count @search-string))
         [:div#options-container
          [:div#options
           (for [item @(rf/subscribe [:item-names])]
             ;; regular expression to see if the search string matches the name
             (if (re-find (re-pattern (str "(?i)" @search-string)) (:name item))     
               [:div#option {:key (:id item)}
                [:a {:href "#"
                     :on-click 
                     #(do
                        (.preventDefault %)
                        (add-new (:id item))
                        (reset! search-string ""))} 
                 (:name item)]]))]])])))

(defn display-line-item [line-item]
  "unpacks dictionary with :unit :item and :qty into readable string"
  (let [qty (:qty line-item)
        unit (rf/subscribe [:unit-abbrev (:unit line-item)])
        item (rf/subscribe [:item-name (:item line-item)])]
    (goog.string/format "%f %s - %s" qty @unit @item)))


;; Durations of time
(defn display-duration [duration]
  [:time {:datetime  (str "PT" duration)}])

(defn encode-duration [h m s]
  (goog.string/format "%dH %dM %fS" h m s))

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
  (let [search-string (r/atom "")]
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
         [:div#options-container
          [:div#options
           (for [item @(rf/subscribe [:item-names])]
             ;; regular expression to see if the search string matches the name
             (if (or (re-find (re-pattern (str "(?i)" @search-string)) (:name item))
                     (= "" @search-string)) 
               [:div#option {:key (:id item)}
                [:a {:href "#"
                     :on-click 
                     #(do
                        (.preventDefault %)
                        (rf/dispatch 
                         [:task/add-product task (:id item) 1 "u1"])
                        (reset! search-string ""))} 
                 (:name item)]]))]])])))

;; Recipe Search
(defn recipe-search []
  (let [search-string (r/atom "")]
    (fn [] 
      [:span
       [:input.search {:type "text"
                       :placeholder "Load Recipe"
                       :value @search-string
                       :on-change #(reset! search-string (-> % 
                                                             .-target 
                                                             .-value))}]
       [:button {:on-click #(do (.preventDefault %)
                                (rf/dispatch [:recipe/new @search-string])
                                (reset! search-string ""))} "+"]
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
                      (:name recipe)]]))]))])))


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
  [:div.modal-content
   [:div.modal-header
    [:button {:type "button" :title "Cancel"
              :class "close"
              :on-click #(close-modal)}
     [:i.material-icons "close"]]
    [:h4.modal-title title]]
   [:div.modal-body body]
   [:div.modal-footer footer]])

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
