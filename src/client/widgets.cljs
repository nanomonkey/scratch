(ns client.widgets
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [client.svg :refer [tag-icon
                                clock-icon]]
            [goog.string.format]))

;; Markdown

(defonce converter (new js/showdown.Converter {:strikethrough true}))

(defn ->html [s]
  (.makeHtml converter s))

(defn markdown-section [s]
  [:span
   {:dangerouslySetInnerHTML {:__html (->html s)}}])


;; Inline Editor

(defn inline-editor [txt {:keys [on-update on-remove markdown?]}]
  (let [s (r/atom {})
        rows (r/atom 0)]
    (fn [txt event-handlers]
      [:span
       (if (:editing? @s)
         [:form {:on-submit #(do
                               (.preventDefault %)
                               (swap! s dissoc :editing?)
                               (when on-update
                                 (on-update (:text @s))))}
           [:textarea {:rows (+ 2 @rows)
                       :style {:resize "none"
                               :width "100%" 
                               :margin "auto"
                               :overlfow "auto"}
                       :value (:text @s)
                       :on-change #(do (swap! s assoc 
                                              :text (-> % .-target .-value))
                                       (reset! rows (count (re-seq #"\n" (:text @s)))))}]
          [:button "✓"]
          [:button {:on-click #(do
                                 (.preventDefault %)
                                 (swap! s dissoc :editing?))
                    :on-blur #(do
                                (.preventDefault %)
                                (swap! s dissoc :editing?))}
           "X"]]
         [:span 
          [:a.edit {:href "#"
                    :style {:margin-left "4px"
                            :alt-text "edit"}
                    :on-click #(do
                                 (.preventDefault %)
                                 (swap! s assoc
                                        :editing? true
                                        :text txt)
                                 (reset! rows (count (re-seq #"\n" (:text @s)))))}
           (if markdown? (markdown-section txt) txt)]
          (when on-remove
            [:a.trash {:href "#"
                       :on-click (fn [e]
                                   (.preventDefault e)
                                   (on-remove))
                       :style {:font-size "small"}} "🗑"])])])))


;; Tag Editor

(defn tag-editor [source remove add]
  "view and modify tags using event handlers source remove and add"
  (let [s (r/atom "")
        k (r/atom "")]
    (fn [source remove add]
      [:div#tags
      [tag-icon]
       [:span
        "Tags: "
        (doall
         (for [tag source]
           [:div#tag {:key tag
                      :style {:display :inline-block
                              :background-color :yellow
                              :color :blue
                              :margin-right "8px"}}
            tag
            [:a {:href "#"
                 :style {:margin-left "4px"}
                 :on-click (fn [e] (.preventDefault e) (remove tag))}
             [:sup "x"]]]))]
       [:span
        [:input {:type :text
                 :value @s
                 :style {:width "6em"}
                 :on-change #(reset! s (-> % .-target .-value))
                 :on-key-up (fn [e]
                              (reset! k (-> e .-key))
                              (when (or (= " " (-> e .-key))
                                        (= "Enter" (-> e .-key)))
                                (add (.trim @s))
                                (reset! s "")))}]]])))

(defn has-tag? [col tag]
  (filter #(contains? (:tags %) tag) col))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Search Field                            ;;
;;                                         ;;
;; displays drop down of results           ;;
;; other options weren't working in Safari ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn item-search [{:keys [placeholder source add create find-by-name]}]
  (let [search-string (r/atom "")]
    (fn [props]
      [:div
       [:input.search {:type "search"
                       :placeholder placeholder
                       :value @search-string
                       :on-change #(reset! search-string 
                                           (-> % .-target .-value))}]
       (when create
         [:button {:on-click 
                   #(do 
                      (.preventDefault %)
                      (create @search-string)
                      #_(if-let [item-id (find-by-name @search-string)]
                          (add item-id)
                          (reset! search-string ""))
                      )} "+"])
       (when (< 1 (count @search-string))
         [:div#options-container
          [:div#options
           (for [[name id] @source]
             ;; regular expression to see if the search string matches the name
             (if (re-find (re-pattern (str "(?i)" @search-string)) name)     
               [:div#option {:key id}
                [:a {:href "#"
                     :on-click 
                     #(do
                        (.preventDefault %)
                        (add id)
                        (reset! search-string ""))} 
                 name]]))]])])))


;;;;;;;;;;;;;;;;;;;;;;
;; Rational Numbers ;;
;;;;;;;;;;;;;;;;;;;;;;

(defn parse-rational [string]
  "parses string into components of rational number if applicable"
  (let [[orig whole numer denom numer2 denom2 float int]
        (re-matches #"(\d+)\s+(\d+)/(\d+)|(\d+)/(\d+)|(\d+[.]\d+)|(\d+)" string)]
    (cond whole {:whole whole :numer numer :denom denom}
          numer2 {:numer numer2 :denom denom2}
          float float
          int  int
          :else "non-parsable")))

(defn display-rational [{:keys [whole numer denom] :as qty}]
  (cond whole [:span whole [:sup numer] "/" [:sub denom]] 
        numer [:span [:sup numer] "/" [:sub denom]] 
        :else (str qty)))

(defn display-line-item [line-item]
  "unpacks dictionary with :unit :item and :qty into readable string"
  (let [qty (:qty line-item)
        unit (rf/subscribe [:unit/abbrev (:unit line-item)])
        item (rf/subscribe [:item/name (:item line-item)])]
    [:span (display-rational qty) " " @unit " " [:strong @item]]))


;;;;;;;;;;;;;;;;;;;;;;;
;; Durations of time ;;
;;;;;;;;;;;;;;;;;;;;;;;

(defn display-duration [{:keys [hr min sec] :as duration}]
  [:time {:dateTime (str "PT"
                         (when hr (str "H" hr))
                         (when min (str "M" min))
                         (when sec (str "S" sec)))}
   
   (when (> hr 0) (str hr " hour" (if (< 1 hr) "s " " ")))
   (when (> min 0) (str min " minute" (if (< 1 min) "s " " ")))
   (when (> sec 0) (str sec " second" (if (< 1 sec) "s " " ")))])

(defn parse-duration [duration]
  "parse duration string of the form H100M59S59 into components"
  (if duration
    (let [[orig h m s]
          (re-matches #"H*(\d+)*M*(\d+)*S*(\d+)*" duration)]
      {:hr (int h)
       :min (int m)
       :sec (int s)})
    nil))

(defn ->duration-string [{:keys [hr min sec]}]
  (str 
   (when hr (str "H" hr))
   (when min (str "M" min)) 
   (when sec (str "S" sec))))

(defn duration->sec [duration]
  (let [{:keys [hr min sec]} (parse-duration duration)]
    (+ sec (* 60 min) (* 360 hr))))


(defn duration-editor [source update]
  (let [duration (r/atom (parse-duration @source))
        editing? (r/atom false)]
    (fn []
      (if @editing?
        [:form {:on-submit #(do
                              (.preventDefault %)
                              (reset! editing? false)
                              (update (->duration-string @duration)))}
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
                  :style {:width "3em"}
                  :min 0
                  :max 59
                  :on-change #(swap! duration assoc :min (-> % .-target .-value))}]
         ";"
         [:input {:type :number
                  :placeholder "SS"
                  :value (:sec @duration)
                  :style {:width "3em"}
                  :min 0
                  :max 59
                  :on-change #(swap! duration assoc :sec (-> % .-target .-value))}]
         [:button "✓"]
         [:button {:on-click #(do
                                (.preventDefault %) 
                                (reset! duration (parse-duration @source))
                                (reset! editing? false))
                   :on-blur #(do
                               (.preventDefault %)
                               (reset! editing? false))}
          "X"]]
        [:button {:style {:border-radius 100}
                  :on-click #(do (.preventDefault %)
                                 (reset! editing? true))}
         [clock-icon]
         (when @source
           (display-duration (parse-duration @source)))]))))


;;;;;;;;;;;;;;;;;;;
;; Recipe Search ;;
;;;;;;;;;;;;;;;;;;;

;; should be replaced with more generalized Item Search above

(defn recipe-search []
  (let [search-string (r/atom "")]
    (fn [] 
      [:span
       [:input.search {:type "search"
                       :placeholder "Load Recipe"
                       :value @search-string
                       :on-change #(reset! search-string (-> % 
                                                             .-target 
                                                             .-value))}]
       [:button {:on-click #(do (.preventDefault %)
                                (rf/dispatch [:recipe/new @search-string])
                                (reset! search-string ""))} "+"]
       (when (< 1 (count @search-string))
         (let [recipes @(rf/subscribe [:recipe/names])] 
           [:ul.search
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


;;;;;;;;;;;;
;; Modals ;;
;;;;;;;;;;;;

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

(defn modal-button [title icon child first-focus]
 [:button.wide
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


;;;;;;;;;;;;;;
;; Spinners ;;
;;;;;;;;;;;;;;

(defn moon-spinner [visible?] 
  (when visible? [:div.moon [:div.disc]]))



  
