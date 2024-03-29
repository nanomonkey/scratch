(ns client.views
  (:require [re-frame.core :as rf]
            [reagent.core :as r] 
            [client.subs :as subs]
            [client.widgets :refer [markdown-section 
                                    inline-editor 
                                    tag-editor
                                    recipe-search
                                    item-search
                                    modal
                                    modal-button
                                    full-modal 
                                    display-line-item
                                    display-duration
                                    display-rational
                                    parse-rational
                                    duration-editor]]
            [client.svg :refer [arrow-up-icon
                                arrow-down-icon
                                comment-icon]]
            [goog.string :as gstring]
            [cljs-time.core :as dt]))


(defn topnav []
  (fn []
    (let [active @(rf/subscribe [:active-panel])]
      [:nav [:ul
             (map (fn [[key value]] 
                    (if (= active key)
                      [:li.active_panel {:key value} [:a value]]
                      [:li.alt_panel {:key value} 
                       [:a {:href "#" :on-click #(rf/dispatch [:set-active-panel key])} value]]) ) 
                  {:recipe "Recipes" 
                   :inventory "Inventory" 
                   :supplier "Suppliers" 
                   :schedule "Schedule"
                   :settings "Settings"})]])))


(defn create-transaction []
  (let [type (r/atom "")
        content (r/atom "")]
    (fn []
      [:form {:on-submit #(do (.preventDefault %)
                              (rf/dispatch [:create-transaction @type @content])
                              (reset! type "")
                              (reset! content ""))}
       [:h2 "Create Transaction"]
       [:div
        [:label "Type"]
        [:input {:type "text"
                 :name "type"
                 :value @type
                 :on-change #(reset! type (-> % .-target .-value))}]]
       [:div
        [:label "Content:"]
        [:input {:type "text"
                 :name "content"
                 :value @content
                 :on-change #(reset! content (-> % .-target .-value))}]]
       [:div
        [:button {:type "submit"} "Post"]]])))


(defn comment-form [root branch]
  (let [text (r/atom "")]
    (fn [root branch]
      [:div.white-panel
       [:form {:on-submit #(do (.preventDefault %)
                               (rf/dispatch [:post-reply root branch @text])
                               (reset! text  "")
                               (rf/dispatch [:modal {:show? false :child nil}]))}
        [:div
         [:label "Reply: "]
         [:textarea{:rows 9
                    :auto-focus true
                    :style {:resize "none"
                            :width "100%" 
                            :margin "auto"
                            :overlfow "auto"}
                    :value @text
                    :on-change #(reset! text (-> % .-target .-value))}]]
        [:div [:button {:type "submit"} "Post"]]]
       (when (not= "" @text)
         [:fieldset
          [:legend "Preview"]
          [:div (markdown-section @text)]])])))

(defn display-post [post-id]
  (let [author (rf/subscribe [:post/author post-id])
        text (rf/subscribe [:post/text post-id])
        timestamp (rf/subscribe [:post/timestamp post-id])
        root (rf/subscribe [:post/root post-id])
        replies (rf/subscribe [:reply-ids post-id])
        comment (r/atom "")
        comment? (r/atom false)]
    (fn [post-id]
      [:div.post 
       [:div (.toDateString (js/Date. @timestamp))]
       [:div.author  @(rf/subscribe [:contact/name @author])]
       [:div (markdown-section @text)]
       [modal-button "Post Comment" "Comment" [comment-form @root post-id]]
       [:div 
        (if (first @replies) [:ul.replies (doall (map #(vector :li {:key %} [display-post %]) @replies))])]])))

(defn comments-view []
  (let [loaded-comment @(rf/subscribe [:loaded-comment])
        replies @(rf/subscribe [:reply-ids loaded-comment])]
    [:div 
     [modal-button "Post New Comment" "Comments" [comment-form loaded-comment loaded-comment]]
     (if (first replies)
       [:div
        [:ul.replies (doall (map #(vector :li {:key %} [display-post %]) replies))]])]))

(defn post []
  (let [content (r/atom "")]
    (fn []
      [:form {:on-submit #(do (.preventDefault %)
                              (rf/dispatch [:post/save @content])
                              (reset! content  ""))}
       [:h2 "Post Message"]
       [:div
        [:label "Content:"]
        [:input {:type "text"
                 :name "content"
                 :value @content
                 :on-change #(reset! content (-> % .-target .-value))}]]
       [:div
        [:button {:type "submit"} "Post"]]])))

(defn query []
  (let [query-map (r/atom nil)
        query-filter (r/atom "{:value {:content {:type \"task\"}}}")
        query-reduce (r/atom nil)
        query-reverse? (r/atom false)
        query-limit (r/atom 5)]
    (fn []
      (let [query {:query-map @query-map
                   :query-filter @query-filter
                   :query-reduce @query-reduce
                   :limit  @query-limit
                   :reverse? @query-reverse?}]
        [:form {:on-submit #(do (.preventDefault %) 
                                (rf/dispatch [:query query]))}
         [:h2 "Query"]
         [:div
          [:label "Map"]
          [:input {:type :textarea
                   :name "query-map"
                   :value @query-map
                   :on-change #(reset! query-map (-> % .-target .-value))}]]
         [:div
          [:label "Filter"]
          [:input {:type :textarea
                   :name "query-filter"
                   :value @query-filter
                   :on-change #(reset! query-filter (-> % .-target .-value))}]]
         [:div
          [:label "Reduce"]
          [:input {:type :textarea
                   :name "query-reduce"
                   :value @query-reduce
                   :on-change #(reset! query-reduce (-> % .-target .-value))}]]
         [:div
          [:label "Reverse"]
          [:input {:type :checkbox
                   :name "query-reverse?"
                   :value @query-reverse?
                   :on-change #(reset! query-reverse? (-> % .-target .-value))}]]
         [:div
          [:label "Limit"]
          [:input {:type :number
                   :name "query-limit"
                   :value @query-limit
                   :on-change #(reset! query-limit (-> % .-target .-value))}]]     
         [:div
          [:button {:type "submit"} "Query"]]]))))

(defn right-panel []
  [:div
   [create-transaction]
   [post]
   [query]
   [:h4 "Feed:"]
   [:button {:on-click #(rf/dispatch [:clear-feed])} "Clear Feed"]
   (doall
    (for [message @(rf/subscribe [:feed])]
      [:div (str message)]))
   [:h4 "Errors:"]
   (doall
    (for [error @(rf/subscribe [:errors])]
      [:div (str error)]))])

(defn server-status []
  [:div
   [:div (str "Websocket Connection: " @(rf/subscribe [:server/status]))]
   [:div (str "Account: " @(rf/subscribe [:server/account]))]])

(defn login []
  (let [name (r/atom "")
        password (r/atom "")]
    (fn []
      [:div 
       (server-status)
       [:form {:on-submit #(do (.preventDefault %)
                               (rf/dispatch [:login @name @password]))}
        [:input {:type "text"
                 :placeholder "Feed Name"
                 :name "login-name"
                 :auto-focus true
                 :value @name
                 :on-change #(reset! name (-> % .-target .-value))}]
        [:div
         [:input {:type "password"
                  :placeholder "Password"
                  :value @password
                  :on-change #(reset! password (-> % .-target .-value))}]]
        [:button {:type "submit"} "Login to Existing Account"]]
       [:div [:button {:on-click #(do (.preventDefault %)
                                      (rf/dispatch [:create-account @name @password]))} 
              "Create New Account"]]])))

(defn login-view []
[:div
 [:div.row
  [:div.column.left]
  [:div.column.middle
   [login]]
  [:div.column.right]]])

(defn list-items [items remove-event task]
  (fn [items remove-event task]
    [:ul.items
     (doall
      (for [i items]
        [:li {:key (:item i)} [:span.removable (display-line-item i )]
         [:button.hidden
          {:title "Remove"
           :on-click #(do (.preventDefault %)
                          (rf/dispatch [remove-event task (:item i)]))} "X "]]))]))

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

(defn move-step [steps pos step]
  (let [steps (remove #{step} steps)
        head (take pos steps)
        tail (drop pos steps)]
    (concat head [step] tail)))

(defn display-steps [task]
  (let [s (r/atom {:order (range (count @(rf/subscribe [:task/steps task])))})]
    (fn [task]
      (let [steps (rf/subscribe [:task/steps task])]
        (when (not (= task (:changed? @s))) 
          (reset! s {:order (range (count @steps))
                     :changed? task}))
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
                                                 move-step pos (:drag-index @s)))
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
          [:li.active [add-step task {:on-add #(swap! s assoc :changed? true)}]]]]))))

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

(defn task-duration [task]
  (duration-editor (rf/subscribe [:task/duration task]) #(rf/dispatch [:task/set-duration task %])))

(defn price-field [price currency])

(defn line-item 
  ([submit](line-item submit {})) 
  ([submit  {:keys [price?]}]
   (let [item-id (r/atom "")
         qty (r/atom {:text ""
                      :value {}
                      :editing? true})
         unit-id (r/atom "")
         price (r/atom "")]
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
                                       "4px solid red")}
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
                         :create #(rf/dispatch [:item/new % "" #{} []])
                         :source (rf/subscribe [:item/source])
                         :add #(reset! item-id %)}]
           [:a.edit {:href "#"
                     :style {:margin-left "4px" :alt-text "edit"}
                     :on-click #(reset! item-id "")}
            @(rf/subscribe [:item/name @item-id])])
         (if price?
           [price-field {:placeholder "price $1.23"
                         }])
         [:button "+ Line Item"]]]))))

(defn task-table [recipe-id]
  (fn [recipe-id]
    (let [tasks @(rf/subscribe [:recipe/tasks recipe-id])]
      [:table#tasks
       [:tbody
        (doall
         (for [task tasks]
           [:tr ^{:key (str task "row")} 
            [:td ^{:key (str task "items")}
;; Equipment for task
             [:div ^{:key (str task "equipment")}
              [modal-button "Add Equipment" "Equipment:"
               [line-item #(rf/dispatch [:task/add-equipment task %1 %2 %3])]]]
             [list-items @(rf/subscribe [:task/equipment task])
              :task/remove-equipment task]
;; Ingredients for task
             [:div ^{:key (str task "-ingredients")}
              [modal-button "Add Ingredient" "Ingredients:"
               [line-item #(rf/dispatch [:task/add-ingredient task %1 %2 %3])]]]
             [list-items @(rf/subscribe [:task/ingredients task])
              :task/remove-ingredient task]]
;; Steps for task
            [:td#task ^{:key (str task "-steps")}
             [:h2 [inline-editor @(rf/subscribe [:task/name task]) 
                   {:on-update #(rf/dispatch [:task/update-name task %])}]]
             [task-duration task]
             [display-steps task]]
;; Yielded products:        
            [:td ^{:key (str task "-products")}
             [modal-button "Add Product Item" "Yields:"
              [line-item #(rf/dispatch [:task/add-product task %1 %2 %3])]]
              [list-items @(rf/subscribe [:task/yields task])
               :task/remove-product task]]
;; Rearrange Tasks
            [:td ^{:key (str task "-arrange")}
             (if-not (= task (first tasks))
               [:button.wide {:title "Move Task up"
                         :on-click #(do (.preventDefault %)
                                             (rf/dispatch [:recipe/move-task-up recipe-id task]))} 
                [arrow-up-icon]])
             (if-not (= task (last tasks))
               [:button.wide
                {:title "Move Task Down"
                 :on-click #(do (.preventDefault %)
                                (rf/dispatch [:recipe/move-task-down recipe-id task]))} 
                [arrow-down-icon]])
             [:button.wide 
                {:title "Remove Task from Recipe"
                 :on-click #(do (.preventDefault %)
                                (rf/dispatch [:recipe/remove-task recipe-id task]))} 
                "Remove"]
             
             (if @(rf/subscribe [:task/dirty? task])
               [:button.wide {:on-click #(do (.preventDefault %)
                                        (rf/dispatch [:task/save task]))} "Save Task"]) ] ]))
        [:tr ^{:key "Add_Task_row"}
         [:td ^{:key "Add_Task"}
          [add-task recipe-id]]] ]]))) 


(defn recipe-view []
  (let [recipe-id @(rf/subscribe [:loaded-recipe])]
    [:div
     [modal]
     [topnav]
     [:div.row
      [:div.column.left
       [:div [recipe-search]]]
      [:div.column.middle
       (if @(rf/subscribe [:recipe/dirty? recipe-id])
         [:div
          [:a.right {:href "#" 
                     :on-click #(rf/dispatch [:recipe/save recipe-id])
                     :style {:border "solid blue"}} "Save Recipe"]])
       [:div
        [:a.right {:href "#" :on-click #(rf/dispatch [:load-comment recipe-id])} 
         [comment-icon (count @(rf/subscribe [:comments recipe-id]))]]]
       [:h1 [inline-editor @(rf/subscribe [:recipe/name recipe-id])
             {:on-update #(rf/dispatch [:recipe/update-name recipe-id %])}]] 
       [inline-editor @(rf/subscribe [:recipe/description recipe-id])
        {:on-update #(rf/dispatch [:recipe/update-description recipe-id %])
         :markdown? true}]
       [:div [tag-editor 
              @(rf/subscribe [:recipe/tags recipe-id]) 
              #(rf/dispatch [:recipe/remove-tag recipe-id %]) 
              #(rf/dispatch [:recipe/save-tag recipe-id %])]]
       [:div [task-table recipe-id]]]
      [:div.column.right
       ;;(right-panel)
       (comments-view)]]]))

(defn supplier-view []
  (let [supplier-id (rf/subscribe [:loaded-supplier])]
    [:div
     [topnav]
     [:div.row
      [:div.column.left
       (doall
        (for [[name id] @(rf/subscribe [:supplier/source])]
          (if (not (= id @supplier-id))
            [:div [:a.supplier {:href "#"
                                :on-click #(rf/dispatch [:load-supplier id])}
                   name]])))]
      [:div.column.middle
       [:div
        [:h1 [inline-editor @(rf/subscribe [:supplier/name @supplier-id])
              {:on-update #(rf/dispatch [:supplier/update-name @supplier-id %])}]]]]
      [:div.column.right
       (right-panel)]]]))

(defn leap-year? [year] 
  "The year must be evenly divisible by 4;
   If the year can also be evenly divided by 100, it is not a leap year;
	unless...
  The year is also evenly divisible by 400. Then it is a leap year."
  (condp #(zero? (mod %2 %1)) year
      400 true
      100 false
      4 true
      false))

(defn =date [date1 date2]
  "Checks to see if dates are the same day.
   Sadly, equal? and = from cljs.time library are a different type of equality"
  (and
    (= (dt/year date1)  (dt/year date2))
    (= (dt/month date1) (dt/month date2))
    (= (dt/day date1)   (dt/day date2))))

(def months ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"])

(defn calendar-view [] 
  (let [today (dt/today)
        year  (dt/year today)
        jan1 (dt/date-time year 1 1)
        start (dt/minus jan1 (dt/days (dt/day-of-week jan1)))]
    [:div
     [:h2 [:center [:a {:href "#"} "<  "] year [:a {:href "#"} "  >"]]]
     [:table#calendar
      [:tbody
        [:tr 
        (doall (for [day [" " "Mon" "Tue" "Wed" "Thu" "Fri" "Sat" "Sun"]]
                 [:th [:div day]]))]
       (for [week (partition-all 7 (map #(dt/plus start (dt/days %)) (range 1 372)))]
         [:tr 
          (let [month (get months (dec (dt/month (first week))))]
            [:th {:class month} (when (> 8 (dt/day (first week))) month)])
          (for [date week]
            [:td {:class (if (=date date today) "today" (get months (dec (dt/month date))))}
             [:a {:href "#" :on-click #(rf/dispatch [:load-date date])} (dt/day date)]])])]]]))


(defn time-cells [rows]
  (for [row (range 1 (inc rows))
        hr (range 0 24)]
    [:div {:style {:grid-area (str row "/" (inc (* 4 hr)) "/"  (inc row) "/" (+ 5 (* 4 hr)))}} (if (= 1 row) (str hr) " ")]))

(defn grid-cells [row]
  (for [hr (range 0 24)]
         [:div {:style {:grid-area (str row "/" (inc (* 4 hr)) "/"  (inc row) "/" (+ 5 (* 4 hr)))}} ""])) 

(defn event-span [row event]
"grid-area:grid-row-start / grid-column-start / grid-row-end / grid-column-end"
  (let [name (:name event)
        start (:starts event)
        hr (dt/hour start)
        min (dt/minute start)
        cell-start (+ (* 4 hr) (quot min 15))
        duration (:duration event)
        cell-end (+ cell-start (quot duration 15))]
    [:div.event {:style {:grid-area (str row "/" cell-start "/" row "/" cell-end)}} name]))


(defn day-events [date]
  (let [events [{:id "e1"
                 :name "Reoccurring Event"
                 :location "l1"
                 :participants ["g1"]
                 :tasks ["t1"]
                 :start "20210515T173308"
                 :reoccurring :weekly}
                {:id "e2"
                 :name "One-off Event"
                 :location "l1"
                 :participants ["g1"]
                 :tasks ["t1"]
                 :start "20210517T170000"}
                {:id "e3"
                 :name "Recipe making Event"
                 :location "l1"
                 :participants "g1"
                 :recipes ["r1"]
                 :starts "20210517T180000"}]]
    [:div.events-container
     (time-cells 4)
     (let [row 2]
       (grid-cells row)
       (event-span row {:name "Task 4"
                        :starts (dt/now) ;;#inst "2021-05-17T18:00:00"
                        :duration 90}))
     (let [row 3]
       (for [hr (range 0 24)]
         [:div {:style {:grid-area (str row "/" (inc (* 4 hr)) "/" (inc row) "/" (+ 5 (* 4 hr)))}} ""])
       [:div.event {:style {:grid-area (str row "/" 5 "/" (inc row) "/" 10)}} "Task 1"])]))

(defn schedule-view []
  (let [today (dt/today)]
    [:div
     [modal]
     [topnav]
     [:div.row
      [:div.column.left
       (calendar-view)]
      [:div.column.middle (day-events today)]
      [:div.column.right
       [:div [:strong "Loaded: "] (str @(rf/subscribe [:loaded-date]))]]]]))


(defn settings-view []
  [:div 
   [modal]
   [topnav]
   [:div.row
    [:div.column.left]
    [:div.column.middle
     [:h3 "Profile"]
     [:div "Logged in as: " @(rf/subscribe [ :server/account])]
     [:div "Connection status: " @(rf/subscribe [:server/status])]
     [:div "ID: " @(rf/subscribe [:server/id])] 
     [:h3 "Default Names"]
     [:h3 "Recipes"]
     [:h3 "Products"]
     [:h3 "Agents"]
     [:h3 "Groups"]
     [:h3 "CSS attributes"]
     [:h3 "Themes"]]
    [:div.column.right]]])


(defn inventory-view []
  (let [location-id (rf/subscribe [:loaded-location])]
    [:div 
     [modal]
     [topnav]
     [:div.row
      [:div.column.left 
       (doall
        (for [[name id] @(rf/subscribe [:location/source])]
          (if (not (= id @location-id))
            [:div [:a.location {:href "#"
                                :on-click #(rf/dispatch [:load-location id])}
                   name]])))
       ]
      [:div.column.middle
       [:div
        [:h1 [inline-editor @(rf/subscribe [:location/name @location-id])
              {:on-update #(rf/dispatch [:location/update-name @location-id %])}]]
        [inline-editor @(rf/subscribe [:location/description @location-id])
         {:on-update #(rf/dispatch [:location/update-description @location-id %])
          :markdown? true}]
        [inline-editor @(rf/subscribe [:location/address @location-id])
         {:on-update #(rf/dispatch [:location/update-address @location-id %])}]
        [:div 
         [modal-button "Add Inventory" "+ Inventory"
          [line-item #(rf/dispatch [:inventory/add @location-id %1 %2 %3])]]
          (doall
             (for [item @(rf/subscribe [:location/inventory @location-id])]
               [:div
                [:span item]
                [:span [display-line-item item]]]))
         ]]]]
      [:div.column.right
       ]]))

(defn main-panel []
  (let [active (rf/subscribe [:active-panel])]
    (fn []
      (do
        (println "Active Panel: " @active)
        (condp = @active
          :login (login-view)
          :recipe (recipe-view)
          :inventory (inventory-view)
          :supplier (supplier-view)
          :schedule (schedule-view)
          :settings (settings-view))))))
