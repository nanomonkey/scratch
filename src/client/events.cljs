(ns client.events
  (:require [re-frame.core :as rf]
            [client.db :as db]
            [client.ws :as ws]))


;;;;;;;;;;;;;
;; Effects ;;
;;;;;;;;;;;;;

(rf/reg-cofx          
 :now                
 (fn [coeffects _]   
   (assoc coeffects :now (js/Date.))))  

(rf/reg-fx
 :ssb-create-account
 (fn [{:keys [name password]}]
   (ws/ssb-create-account! name password)))

(rf/reg-fx
 :ssb-login
 (fn [{:keys [username password]}]             
   (ws/ssb-login! username password))) 

(rf/reg-fx
 :ssb-query
 (fn [user-id query]
   (ws/chsk-send! [:ssb/query {:msg query}] 8000
                  (fn [cb-reply] (rf/dispatch [:query-response cb-reply])))))

(rf/reg-fx
 :ssb-contact
 (fn [contact-id]
   (ws/chsk-send! [:ssb/contact {:msg contact-id}] 8000
                  (fn [cb-reply] (rf/dispatch [:contact cb-reply])))))

(rf/reg-cofx         
 :local-store
 (fn [cofx local-store-key]
   (assoc cofx
          :local-store
          (js->clj (.getItem js/localStorage local-store-key)))))

(rf/reg-fx
:set-local-store
(fn [local-store-key value]
  (.setItem js/localStorage local-store-key (str value))))

(defonce last-temp-id (atom 0))

(rf/reg-cofx
  :temp-id 
  (fn [cofx _]
    (assoc cofx :temp-id (str (swap! last-temp-id inc)))))

(rf/reg-fx
 :start-ws
 (fn []
   (ws/start!)))

;;;;;;;;;;;;;;;;;;;;;
;; Event Handlers  ;;
;;;;;;;;;;;;;;;;;;;;;

(rf/reg-event-fx         
 :load-defaults-localstore
 [ (rf/inject-cofx :local-store "defaults") ] 
 (fn [cofx  _data]          ;; cofx is a map containing inputs; _data unused
   (let [defaults (:local-store cofx)]  ;; <--  use it here
     {:db (assoc (:db cofx) :defaults defaults)})))  ;; returns effects mapW

(rf/reg-event-fx
 :create-account
 (fn [cofx [_ name password]]
   {:db (assoc (:db cofx) :account :creating)
    :ssb-create-account {:name name
                         :password password}}))

(rf/reg-event-fx
 :login
 (fn [cofx [_ username password]]
   {:db (assoc-in (:db cofx) [:server] "verifying")
    :ssb-login {:username username :password password}}))

(rf/reg-event-db
 :account
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

(rf/reg-event-fx
 :server/connect!
 (fn [cofx [_]]
   {:db (assoc-in (:db cofx) [:server :account] :connecting)
    :start-ws []}))

(rf/reg-event-fx
 :login-successful
 (fn [cofx [_ account ch-chsk]]
   {:db (assoc-in (:db cofx) [:server :account] account)
    :dispatch [:set-active-panel :recipe]}))

(rf/reg-event-fx
 :login-failed
 (fn [cofx [_]]
   {:db (assoc-in (:db cofx) [:server :account] :login-failed)}))

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
 ::initialize-db
 (fn  [_ _]
   db/recipe-db))

;; UI elements
(rf/reg-event-db
  :set-active-panel
  (fn [db [_ value]]
    (assoc db :active-panel value)))

(rf/reg-event-db
 :load-recipe 
 (fn [db [_ recipe-id]]
   (do (assoc-in db [:active-panel] :recipe)
       (assoc-in db [:loaded :recipe] recipe-id))))

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


;; Recipes
(rf/reg-event-db
 :recipe/update-name
 (fn [db [_ recipe-id name]]
   (assoc-in db [:recipes recipe-id :name] name)))

(rf/reg-event-db
 :recipe/update-description
 (fn [db [_  recipe-id description]]
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
 :task/sync
 (fn [cofx [_ task-id]]
   {:db (update-in (:db cofx) [:tasks task-id :sync] :syncing)
    :dispatch [:ssb/update task-id]}))

(rf/reg-event-fx
 :recipe/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name]]
   {:db
    (update (:db cofx) :recipes assoc
            (:temp-id cofx) {:id (:temp-id cofx)
                             :name name
                             :description "..."
                             :tags #{}
                             :task-list []})
    :dispatch [:load-recipe (:temp-id cofx)]}))

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

(defn vec-remove
  "remove element at pos(ition) in vector"
  [coll pos]
  (vec (concat (subvec coll 0 pos) (subvec coll (inc pos)))))

(defn vec-replace
  "replace element at pos(ition) in a vector with new-item"
  [coll new-item pos]
  (vec (concat (subvec coll 0 pos)  (vector new-item) (subvec coll (inc pos)))))

(rf/reg-event-db
 :task/replace-step
 (fn [db [_ task-id text step-pos]]
   (update-in db [:tasks task-id :steps] #(vec-replace % text step-pos))))

(rf/reg-event-db
 :task/remove-step
 (fn [db [_ task-id step-pos]]
   (update-in db [:tasks task-id :steps] #(vec-remove % step-pos))))

;;Locations
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

;;Inventory
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
     (update-in db [:inventory location-id :qty item-id] + qty))))

;;Suppliers
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



