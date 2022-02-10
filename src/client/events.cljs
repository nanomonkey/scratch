(ns client.events
  (:require [re-frame.core :as rf]
            [taoensso.sente  :as sente  :refer (cb-success?)]
            [client.db :as db]
            [client.ws :as ws]))

;;;;;;;;;;;;;;;;;;;;;;;;;
;;  Utility Functions  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;

(defn vec-remove
  "remove element at pos(ition) in vector"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn vec-replace
  "replace element at pos(ition) in a vector with new-item"
  [coll new-item pos]
  (vec (concat (subvec coll 0 pos)  (vector new-item) (subvec coll (inc pos)))))

(defn vec-replace-items
  "replace an item with another in a vector"
  [coll old-item new-item]
  (let [pos (.indexOf coll old-item)]
    (vec-replace coll new-item pos)))

(defn vec-swap 
  "move element in pos(ition) up one"
  [coll pos]
  (if (and (< pos (count coll))
           (> pos 0))
    (vec (concat (subvec coll 0 (dec pos)) 
                 (vector (nth coll pos) (nth coll (dec pos))) 
                 (subvec coll (inc pos))))
    coll))

(defn index-by
  "Transform a coll to a map with a given key as a lookup value"
  [key coll]
  (into {} (map (juxt key identity))))


;;;;;;;;;;;;;
;; Effects ;;
;;;;;;;;;;;;;

(rf/reg-fx
 :start-ws
 (fn []
   (ws/start!)))

(rf/reg-fx
 :ssb/create-account
 (fn [{:keys [name password]}]
   (ws/ssb-create-account! name password)))

(rf/reg-fx
 :ssb/login
 (fn [{:keys [username password]}]             
   (ws/ssb-login! username password))) 

(rf/reg-fx
 :ssb/get-id
 (fn []
   (ws/chsk-send! [:ssb/get-id] 5000
                  (fn [reply] 
                    (if (cb-success? reply) 
                      (rf/dispatch [:ssb/id (:id reply)])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/create
 (fn [{:keys [type content]}]
   (let [old-id (:id content)]
     (ws/chsk-send! [:ssb/create {:type type :content (dissoc content :id)}] 5000
                    (fn [reply] 
                      (if (cb-success? reply) 
                        (rf/dispatch [:saved type old-id (:new-id reply)])
                        (rf/dispatch [:error reply])))))))

(rf/reg-fx
 :ssb/update 
 (fn [{:keys [id content]}]
   (ws/chsk-send! [:ssb/update {:id id :content content}] 5000
                  (fn [cb-reply] (rf/dispatch [:clear-updates id])))))

(rf/reg-fx 
 :ssb/tombstone
 (fn [{:keys [id reason]}]
   (ws/chsk-send! [:ssb/tombstone {:id id :reason reason}] 5000
                  (fn [cb-reply] (rf/dispatch [:tombstoned id])))))

(rf/reg-fx
 :ssb/query
 (fn [query]
   (ws/chsk-send! [:ssb/query {:msg query}] 8000
                  (fn [reply] 
                    (if (cb-success? reply) 
                      (rf/dispatch [:feed reply])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/contact
 (fn [contact-id]
   (ws/chsk-send! [:ssb/contact {:msg contact-id}] 8000
                  (fn [cb-reply] (rf/dispatch [:contact cb-reply])))))

(rf/reg-fx
:set-local-store
(fn [local-store-key value]
  (.setItem js/localStorage local-store-key (str value))))


;;;;;;;;;;;;;;;;
;; Coeffects  ;;
;;;;;;;;;;;;;;;;

(rf/reg-cofx          
 :now                
 (fn [cofx _]   
   (assoc cofx :now (js/Date.))))  

(rf/reg-cofx         
 :local-store
 (fn [cofx local-store-key]
   (assoc cofx
          :local-store
          (js->clj (.getItem js/localStorage local-store-key)))))

(defonce last-temp-id (atom 0))

(rf/reg-cofx
  :temp-id 
  (fn [cofx _]
    (assoc cofx :temp-id (swap! last-temp-id inc))))

;;;;;;;;;;;;;;;;;;;;;
;; Event Handlers  ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx
 :query
 (fn [cofx [_ {:keys [:map :filter :reduce :reverse :limit]}]]
   (let [query {:$map map :$filter filter :$reduce reduce :reverse reverse :limit limit}]
     {:db (:db cofx) ;set a spinner?
      :ssb/query query})))

(rf/reg-event-fx         
 :load-defaults-localstore
 [ (rf/inject-cofx :local-store "defaults") ] 
 (fn [cofx  _data]          ;; cofx is a map containing inputs; _data unused
   (let [defaults (:local-store cofx)]  ;; <--  use it here
     {:db (assoc (:db cofx) :defaults defaults)})))  ;; returns effects mapW

(rf/reg-event-fx
 :create-account
 (fn [cofx [_ name password]]
   {:db (assoc-in (:db cofx) [:server :account] "creating")
    :ssb/create-account {:name name
                         :password password}}))

(rf/reg-event-fx
 :login
 (fn [cofx [_ username password]]
   {:db (assoc-in (:db cofx) [:server :status] "verifying")
    :ssb/login {:username username :password password}}))

(rf/reg-event-fx
 :server/connect!
 (fn [cofx [_]]
   {:db (assoc-in (:db cofx) [:server :account] "connecting")
    :start-ws []}))

(rf/reg-event-fx
 :login-successful
 (fn [cofx [_ account state]]
   {:db (assoc-in (:db cofx) [:server] {:account  account
                                        :status state})
    :dispatch [:set-active-panel :recipe]}))

(rf/reg-event-fx
 :login-failed
 (fn [cofx [_]]
   {:db (assoc-in (:db cofx) [:server :account] "login failed")}))

(rf/reg-event-fx
 :save-defaults-localstore
 (fn [cofx [_ defaults]]
   {:db (assoc-in (:db cofx) [:defaults :saved?] true)
    :set-local-store ["defaults" defaults]}))

(rf/reg-event-fx
 ::initialize-ssb
 (fn [cofx [_ conn]]
   {:db (assoc (:db cofx) :ssb-conn conn)}))

(rf/reg-event-db
 :error
 (fn [db [_ error]]
   (update-in db [:errors] (fnil conj error []))))

(rf/reg-event-db
 :ssb/id 
 (fn [db [_ id]]
   (update-in db [:server :id] id)))

(rf/reg-event-db
 :feed
 (fn [db [_ message]]
   (update-in db [:feed] (fnil conj message []))))

(rf/reg-event-db
 :server/account
 (fn [db [_ status]]
   (assoc-in db [:server :account] status)))

(rf/reg-event-db
 :waiting-spinner
 (fn [db _]        ;;TODO Add id of section?!
   (assoc-in db [:waiting-spinner] true)))

(rf/reg-event-db
 :server/status
 (fn [db [_ value]]
   (assoc-in db [:server :status] value)))

(rf/reg-event-db
 ::initialize-db
 (fn  [_ _]
   db/recipe-db))

(defn pluralize-keyword [keyword]
  (keyword (str (symbol keyword) "s")))

(rf/reg-event-db
 :saved
 (fn [db [_ type old-id new-id]]
   (case type
     :task (rf/dispatch [:task/updated old-id new-id])
     )))

(rf/reg-event-db
 :field-updated
 (fn [db [_ id field]]
   (update-in db [:updates id] (fnil conj field #{}))))

(rf/reg-event-db
 :clear-updates
 (fn [db [_ id]]
   (update-in db [:updates] dissoc id)))

;; UI elements
(rf/reg-event-db
  :set-active-panel
  (fn [db [_ value]]
    (assoc db :active-panel value)))

(rf/reg-event-db
 :load-recipe 
 (fn [db [_ recipe-id]]
   (-> db 
       (assoc-in [:active-panel] :recipe)
       (assoc-in [:loaded :recipe] recipe-id))))

(rf/reg-event-db
 :loaded-recipe
 (fn [db [_ id]]
  (assoc-in db [:loaded :recipe] id)))

(rf/reg-event-db
 :loaded-location
 (fn [db [_ id]]
   (assoc-in db [:loaded :location] id)))

(rf/reg-event-db
 :loaded-supplier
 (fn [db [_ id]]
   (assoc-in db [:loaded :supplier] id)))

(rf/reg-event-db
 :loaded-date
 (fn [db [_ date]]
   (assoc-in db [:loaded :date] date)))

(rf/reg-event-db
 :modal
 (fn [db [_ data]]
   (assoc-in db [:modal] data)))


;;;;;;;;;;;;;;
;;  Recipes  ;
;;;;;;;;;;;;;;

(rf/reg-event-db
 :recipe/update-name
 (fn [db [_ recipe-id name]]
   (assoc-in db [:recipes recipe-id :name] name)))

(rf/reg-event-db
 :recipe/update-description
 (fn [db [_ recipe-id description]]
   (assoc-in db [:recipes recipe-id :description] description)))

(rf/reg-event-db
 :recipe/save-tag
 (fn [db [_ recipe-id tag]]
   (let [tag (-> tag
                 .trim
                 .toLowerCase)])
   (update-in db [:recipes recipe-id :tags] (fnil conj #{}) tag)))

(rf/reg-event-db
 :recipe/remove-tag
 (fn [db [_ recipe-id tag]]
   (update-in db [:recipes recipe-id :tags] (fn [tags]
                                              (vec (remove #{tag} tags))))))

(rf/reg-event-db
 :recipe/add-task
 (fn [db [_ recipe-id task-id]]
   (update-in db [:recipes recipe-id :task-list] (fnil conj []) task-id)))

(rf/reg-event-db
 :recipe/move-task-up
 (fn [db [_ recipe-id task-id]]
   (let [tasklist @(rf/subscribe [:recipe/task-list recipe-id])
         task-pos @(rf/subscribe [:recipe/task-pos recipe-id task-id])]
     (update-in db [:recipes recipe-id] assoc :task-list (vec-swap tasklist task-pos)))))

(rf/reg-event-db
 :recipe/move-task-down
 (fn [db [_ recipe-id task-id]]
   (let [tasklist @(rf/subscribe [:recipe/task-list recipe-id])
         task-pos @(rf/subscribe [:recipe/task-pos recipe-id task-id])]
     (update-in db [:recipes recipe-id] assoc :task-list (vec-swap tasklist (inc task-pos))))))

(rf/reg-event-db
 :recipe/remove-task
 (fn [db [_ recipe-id task-id]]
   (let [tasklist @(rf/subscribe [:recipe/task-list recipe-id])
         task-pos @(rf/subscribe [:recipe/task-pos recipe-id task-id])]
     (update-in db [:recipes recipe-id] assoc :task-list (vec-remove tasklist task-pos)))))

(rf/reg-event-fx
 :recipe/new-task
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ recipe name]]
   (let [id (:temp-id cofx)]
     {:db
      (update (:db cofx) :tasks assoc
              (:temp-id cofx) {:id (:temp-id cofx)
                               :name name})
      :dispatch [:recipe/add-task recipe id]})))

(rf/reg-event-fx
 :recipe/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name]]
   {:db
    (update (:db cofx) :recipes assoc
            (:temp-id cofx) {:id (:temp-id cofx)
                             :statis :new
                             :name name
                             :description "..."
                             :tags #{}
                             :task-list []})
    :dispatch [:load-recipe (:temp-id cofx)]}))


;;;;;;;;;;;
;  Items  ;
;;;;;;;;;;;

(rf/reg-event-fx
 :item/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name description tags]]
   {:db 
    (update (:db cofx) :items assoc
             (:temp-id cofx) {:id (:temp-id cofx) 
                              :name name 
                              :description description
                              :tags tags})}))

;;;;;;;;;;;
;  Units  ;
;;;;;;;;;;;

(rf/reg-event-fx
 :unit/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name abbrev type]]
   (let [temp-id (:temp-id cofx)]
     {:db
      (update (:db cofx) :units assoc
              temp-id {:id temp-id 
                       :name name 
                       :abbrev abbrev 
                       :type type})})))

;;;;;;;;;;;
;  Tasks  ;
;;;;;;;;;;;

(rf/reg-event-fx
 :task/save
 (fn [cofx [_ id]]
   (let [status  @(rf/subscribe [:task/status id])]
         (if (= :new status)
           {:ssb/create {:type "task" :content (dissoc @(rf/subscribe [:task id]) :id)}}
           (println "Trying to save" id "with status" status)))))

(rf/reg-cofx
 :task/update
 (fn [cofx id]
   (let [update-keys @(rf/subscribe [:updates id])
         changes (select-keys update-keys @(rf/subscribe [:task id]))]
     {:db (:db cofx) 
      :ssb/update {:id id
                   :content changes}})))

(rf/reg-event-db
 :task/updated
 (fn [db [_ old-id new-id]]
   (let [recipe @(rf/subscribe [:loaded-recipe])
         task-list @(rf/subscribe [:recipe/task-list recipe])
         task-pos (.indexOf task-list old-id)]
     (-> db
         (update-in [:tasks] assoc new-id (merge @(rf/subscribe [:task old-id]) {:id new-id}))
         (update-in [:tasks] dissoc old-id)
         (update-in [:recipes recipe :task-list] assoc (vec-replace task-list task-pos new-id))))))

(rf/reg-event-db 
 :task/update-name
 (fn [db [_ task-id name]]
   (assoc-in db [:tasks task-id :name] name)))

(rf/reg-event-db
:task/set-duration 
(fn [db [_ task-id duration]]
  (assoc-in db [:tasks task-id :duration] duration)))

(rf/reg-event-db
 :task/add-ingredient
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :ingredients :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :ingredients :qty item-id] qty)
         (assoc-in [:tasks task-id :ingredients :units item-id] unit)
         (update-in [:tasks task-id :ingredients :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty, assume the same unit (?!)
     (update-in db [:tasks task-id :ingredients :qty item-id] + qty))))

(rf/reg-event-db
 :task/remove-ingredient
 (fn [db [_ task-id item-id]]
   (-> db
       (update-in [:tasks task-id :ingredients :qty] dissoc item-id)
       (update-in [:tasks task-id :ingredients :units] dissoc item-id)
       (update-in [:tasks task-id :ingredients :items] 
                  (fn [items] (vec (remove #(= item-id %) items)))))))

(rf/reg-event-db
 :task/add-product
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :yields :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :yields :qty item-id] qty)
         (assoc-in [:tasks task-id :yields :units item-id] unit)
         (update-in [:tasks task-id :yields :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :yields :qty item-id] + qty))))

(rf/reg-event-db
 :task/remove-product
 (fn [db [_ task-id item-id]]
   (-> db
       (update-in [:tasks task-id :yields :qty] dissoc item-id)
       (update-in [:tasks task-id :yields :units] dissoc item-id)
       (update-in [:tasks task-id :yields :items] 
                  (fn [items] (vec (remove #(= item-id %) items)))))))

(rf/reg-event-db
 :task/add-equipment
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :equipment :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :equipment :qty item-id] qty)
         (assoc-in [:tasks task-id :equipment :units item-id] unit)
         (update-in [:tasks task-id :equipment :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :equipment :qty item-id] + qty))))

(rf/reg-event-db
 :task/remove-equipment
 (fn [db [_ task-id item-id]]
   (-> db
       (update-in [:tasks task-id :equipment :qty] dissoc item-id)
       (update-in [:tasks task-id :equipment :units] dissoc item-id)
       (update-in [:tasks task-id :equipment :items] 
                  (fn [items] (vec (remove #(= item-id %) items)))))))

(rf/reg-event-db
 :task/add-optional
 (fn [db [_ task-id item-id qty unit]]
   ;; check if it's already in the task
   (if (nil? (get-in db [:tasks task-id :optional :qty item-id]))
     ;; not in the task, add to qty and yields
     (-> db
         (assoc-in [:tasks task-id :optional :qty item-id] qty)
         (assoc-in [:tasks task-id :optional :units item-id] unit)
         (update-in [:tasks task-id :optional :items] (fnil conj []) item-id))
     ;; in the task, add to existing qty
     (update-in db [:tasks task-id :optional :qty item-id] + qty))))

(rf/reg-event-db
 :task/remove-optional
 (fn [db [_ task-id item-id]]
   (-> db
       (update-in [:tasks task-id :optional :qty] dissoc item-id)
       (update-in [:tasks task-id :optional :units] dissoc item-id)
       (update-in [:tasks task-id :optional :items] 
                  (fn [items] (vec (remove #(= item-id %) items)))))))

(rf/reg-event-db
 :task/add-step
 (fn [db [_ task-id step]]
   (update-in db [:tasks task-id :steps] (fnil conj []) step)))

(rf/reg-event-db 
 :task/update-all-steps
 (fn [db [_ task-id steps]]
   (assoc-in db [:tasks task-id :steps] steps)))

(rf/reg-event-db
 :task/replace-step
 (fn [db [_ task-id text step-pos]]
   (update-in db [:tasks task-id :steps] #(vec-replace % text step-pos))))

(rf/reg-event-db
 :task/remove-step
 (fn [db [_ task-id step-pos]]
   (update-in db [:tasks task-id :steps] #(vec-remove % step-pos))))


;;;;;;;;;;;;;;;;
;;  Locations  ;
;;;;;;;;;;;;;;;;

(rf/reg-event-db
 :location/update-name 
 (fn [db [_ location-id name]]
   (assoc-in db [:locations location-id :name] name)))

(rf/reg-event-db
 :location/update-description
 (fn [db [_ location-id description]]
   (assoc-in db [:locations location-id :description] description)))

(rf/reg-event-db
 :location/update-address
 (fn [db [_ location-id address]]
   (assoc-in db [:locations location-id :address] address)))


;;;;;;;;;;;;;;;;
;;  Inventory  ;
;;;;;;;;;;;;;;;;

(rf/reg-event-db
 :inventory/add
 (fn [db [_ location-id item-id qty unit]]
   ;; check if it's already in inventory
   (if (nil? (get-in db [:inventory location-id :items item-id]))
     ;; not in inventory, add
     (-> db
         (assoc-in [:inventory location-id :qty item-id] qty)
         (assoc-in [:inventory location-id  :units item-id] unit)
         (update-in [:inventory location-id :items] (fnil conj []) item-id))
     ;; in the inventory, add to existing qty
     (update-in db [:inventory location-id :qty item-id] + qty)))) ;TODO seperate out by units or use base units


;;;;;;;;;;;;;;;
;  Suppliers  ;
;;;;;;;;;;;;;;;

(rf/reg-event-db
 :supplier/update-name 
 (fn [db [_ supplier-id name]]
   (assoc-in db [:suppliers supplier-id :name] name)))

(rf/reg-event-db
 :supplier/update-description
 (fn [db [_ supplier-id description]]
   (assoc-in db [:suppliers supplier-id :description] description)))

(rf/reg-event-db
 :supplier/update-address
 (fn [db [_ supplier-id address]]
   (assoc-in db [:suppliers supplier-id :address] address)))



