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

(defn parse-json [msg] 
  (js->clj msg :keywordize-keys true))

(defn remove-lineitem [id list] 
  (vec (remove #(= id (:item %)) list)))

(defn pluralize-keyword [keyword]
  (keyword (str (symbol keyword) "s")))

(defn remove-nils [record]
  "Removes nil values from map, but allows for false values"
  (into {} (remove (comp nil? second) record)))

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
 :ssb/whoami
 (fn []
   (ws/chsk-send! [:ssb/whoami] 10000
                  (fn [reply] 
                    (if (cb-success? reply)
                      (rf/dispatch [:save-whoami (:id reply)])
                      (rf/dispatch [:error (str reply)]))))))

(rf/reg-fx
 :ssb/lookup-name
 (fn [id]
   (ws/chsk-send! [:ssb/lookup-name id] 1500
                  (fn [reply]
                    (if (cb-success? reply)
                      (rf/dispatch [:contact/name id (:name reply)])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/publish
 (fn [content]
   (ws/chsk-send! [:ssb/publish content] 15000
                  (fn [reply] 
                    (if (cb-success? reply) 
                      (rf/dispatch [:published reply])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/create-record
 (fn [content created-fn]
   (ws/chsk-send! [:ssb/create content] 55000
                  (fn [reply] 
                    (if (cb-success? reply) 
                      (created-fn reply)
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/update-record
 (fn [{:keys [id updates]}]
   (ws/chsk-send! [:ssb/update {:id id :updates updates}] 5000
                  (fn [reply] 
                    (if (cb-success? reply) 
                      (rf/dispatch [:clear-updates id])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx 
 :ssb/tombstone-record
 (fn [{:keys [id reason]}]
   (ws/chsk-send! [:ssb/tombstone {:id id :reason reason}] 5000
                  (fn [reply] 
                      (if (cb-success? reply) 
                        (rf/dispatch [:tombstoned id])
                        (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/query
 (fn [query]
   (ws/chsk-send! [:ssb/query {:msg query}] 8000
                  (fn [reply] 
                    (if (cb-success? reply) 
                      (rf/dispatch [:feed (parse-json reply)])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/get-thread 
 (fn [root]
   (ws/chsk-send! [:ssb/query {:msg [{:$filter {:value {:content {:type "post" :root root}}}}]}] 8000
                  (fn [reply]
                    (if (cb-success? reply)
                      (rf/dispatch [:comments [root reply]])
                      (rf/dispatch [:error reply]))))))

(rf/reg-fx
 :ssb/get-recipes
 (fn [topic]
   (ws/chsk-send! [:ssb/query-collect {:msg [{:$filter {:value {:content {:type topic}}}}]}
                   8000
                   (fn [reply]
                     (if (cb-success? reply)
                       (rf/dispatch [:add-recipes (:recipes reply)])
                       (rf/dispatch [:error reply])))])))

(rf/reg-fx
 :ssb/serve-blob
 (fn [blob-id] (ws/chsk-send! [:ssb/serve-blob blob-id]))) ; response comes back through [:ssb/blob {:message }]

(rf/reg-fx
 :ssb/get-blob-url
 (fn [blob-id] (ws/chsk-send! [:ssb/blob-url  blob-id] 5000
                             (fn [reply]
                               (if (cb-success? reply)
                                 (rf/dispatch [:blob/add-url blob-id reply])
                                 (rf/dispatch [:error reply]))))))  


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
 (fn [cofx [_ query]]
   {:db (:db cofx)        ;set a spinner?
    :ssb/query query}))

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
 :ssb/whoami
 (fn [cofx [_ username password]]
   {:ssb/whoami []}))

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
   (update-in db [:errors] (fnil conj []) error)))

(rf/reg-event-db
 :save-whoami 
 (fn [db [_ id]]
   (assoc-in db [:server :whoami] id)))

(rf/reg-event-db
 :feed
 (fn [db [_ message]]
   (update-in db [:feed] (fnil conj []) message)))

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
 :add-recipes
 (fn [db [_ recipes]] 
   (update-in db [:recipes] (merge (:recipes db) (index-by :id recipes)))))

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
   (update-in db [:recipes recipe-id :tasks] (fnil conj []) task-id)))

(rf/reg-event-db
 :recipe/move-task-up
 (fn [db [_ recipe-id task-id]]
   (let [tasklist @(rf/subscribe [:recipe/tasks recipe-id])
         task-pos @(rf/subscribe [:recipe/task-pos recipe-id task-id])]
     (update-in db [:recipes recipe-id] assoc :tasks (vec-swap tasklist task-pos)))))

(rf/reg-event-db
 :recipe/move-task-down
 (fn [db [_ recipe-id task-id]]
   (let [tasklist @(rf/subscribe [:recipe/tasks recipe-id])
         task-pos @(rf/subscribe [:recipe/task-pos recipe-id task-id])]
     (update-in db [:recipes recipe-id] assoc :tasks (vec-swap tasklist (inc task-pos))))))

(rf/reg-event-db
 :recipe/remove-task
 (fn [db [_ recipe-id task-id]]
   (let [tasklist @(rf/subscribe [:recipe/tasks recipe-id])
         task-pos @(rf/subscribe [:recipe/task-pos recipe-id task-id])]
     (update-in db [:recipes recipe-id] assoc :tasks (vec-remove tasklist task-pos)))))

(rf/reg-event-fx
 :recipe/new-task
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ recipe name]]
   (let [id (:temp-id cofx)]
     {:db (update (:db cofx) :tasks assoc id {:id id :name name})
      :dispatch [:recipe/add-task recipe id]})))

(rf/reg-event-fx
 :recipe/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name]]
   (let [id (:temp-id cofx)]
     {:db (update id :recipes assoc id {:id id
                                        :statis :new
                                        :name name
                                        :description "..."
                                        :tags #{}
                                        :tasks []})
      :dispatch [:load-recipe id]})))


;;;;;;;;;;;
;  Items  ;
;;;;;;;;;;;

(rf/reg-event-fx
 :item/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name description tags]]
   (let [id (:temp-id cofx)]
     {:db (update (:db cofx) :items assoc id {:id id 
                                              :name name 
                                              :description description
                                              :tags tags})})))

;;;;;;;;;;;
;  Units  ;
;;;;;;;;;;;

(rf/reg-event-fx
 :unit/new
 [(rf/inject-cofx :temp-id)]
 (fn [cofx [_ name abbrev type]]
   (let [id (:temp-id cofx)]
     {:db (update (:db cofx) :units assoc id {:id id 
                                              :name name 
                                              :abbrev abbrev 
                                              :type type})})))

;;;;;;;;;;;
;  Tasks  ;
;;;;;;;;;;;

(rf/reg-event-fx
 :task/save
 (fn [cofx [_ id]]
   (let [status  @(rf/subscribe [:task/status id])
         record (dissoc @(rf/subscribe [:task id]) :id)]
     (if (= :new status)
       {:db (:db cofx)
        :ssb/create-record [(merge {:type "task"} record)
                            (fn [reply] (let [new-id (:id reply)]
                                          (rf/dispatch [:task/updated id new-id])))]}
       (println "Trying to save" id "with status" status)))))

(rf/reg-event-fx
 :task/update
 (fn [cofx id]
   (let [update-keys @(rf/subscribe [:updates id])
         changes (select-keys update-keys @(rf/subscribe [:task id]))]
     {:db (:db cofx) 
      :ssb/update-record {:id id
                          :content changes}})))

(rf/reg-event-db
 :task/updated
 (fn [db [_ old-id new-id]]
   (let [recipe @(rf/subscribe [:loaded-recipe])
         task-list @(rf/subscribe [:recipe/tasks recipe])
         task-pos (.indexOf task-list old-id)]
     (-> db
         (update-in [:tasks] assoc new-id (merge @(rf/subscribe [:task old-id]) {:id new-id}))
         (update-in [:tasks] dissoc old-id)
         (update-in [:recipes recipe :tasks] assoc (vec-replace task-list task-pos new-id))))))

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
   (update-in db [:tasks task-id :ingredients] (fnil conj []) {:item item-id
                                                               :qty qty
                                                               :unit unit})))

(rf/reg-event-db
 :task/remove-ingredient
 (fn [db [_ task-id item-id]]
   (update-in db [:tasks task-id :ingredients] #(remove-lineitem item-id %))))

(rf/reg-event-db
 :task/add-product
 (fn [db [_ task-id item-id qty unit]]
   (update-in db [:tasks task-id :yields] (fnil conj []) {:item item-id
                                                          :qty qty
                                                          :unit unit})))

(rf/reg-event-db
 :task/remove-product
 (fn [db [_ task-id item-id]]
   (update-in db [:tasks task-id :yields] #(remove-lineitem item-id %))))

(rf/reg-event-db
 :task/add-equipment
 (fn [db [_ task-id item-id qty unit]]
   (update-in db [:tasks task-id :equipment] (fnil conj []) {:item item-id
                                                             :qty qty
                                                             :unit unit})))

(rf/reg-event-db
 :task/remove-equipment
 (fn [db [_ task-id item-id]]
   (update-in db [:tasks task-id :yields] #(remove-lineitem item-id %))))


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


;;;;;;;;;;;;;;
;; Contacts ;;  TODO: should this just be about?
;;;;;;;;;;;;;;

(rf/reg-event-db
 :contact/name
 (fn [db [_ id name]]
   (assoc-in db [:contacts id :name] name)))

(rf/reg-event-fx
 :contact/save-name
 (fn [cofx [_ id name]]
   {:ssb/create-record [{:type "about" :about id :name name}
                        (fn [reply] (rf/dispatch [:contact/name id name]))]}))

(rf/reg-event-db
 :contact/image             ; Save image url instead?
 (fn [db [_ id image-id]]
   (assoc-in db [:contacts id :image] image-id)))

(rf/reg-event-fx
 :contact/save-image
 (fn [cofx [_ id blob-id]]
   {:ssb/create-record [{:type "about" :about id :image blob-id}
                        (fn [reply] (rf/dispatch [:contact/name id name]))]}))

(rf/reg-event-fx
 :contact/follow
 (fn [cofx [_ id]]
   {:ssb/create-record [{:type "contact" :contact id :following true :blocking false}
                        (fn [reply] (rf/dispatch [:feed reply]))]}))

(rf/reg-event-fx
 :contact/unfollow
 (fn [cofx [_ id]]
   {:ssb/create-record [{:type "contact" :contact id :following false}
                        (fn [reply] (rf/dispatch [:feed reply]))]}))

(rf/reg-event-fx
 :contact/block
 (fn [cofx [_ id]]
   {:ssb/create-record [{:type "contact" :contact id :following false :blocking true}
                        (fn [reply] (rf/dispatch [:feed reply]))]}))

(rf/reg-event-fx
 :contact/unblock
 (fn [cofx [_ id]]
   {:ssb/create-record [{:type "contact" :contact id :blocking false}
                        (fn [reply] (rf/dispatch [:feed reply]))]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Comments, Posts and Replies  ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(rf/reg-event-fx
 :create-transaction
 (fn [cofx [_ type content]]
   {:ssb/create-record [(merge {:type type} content) (fn [reply] (rf/dispatch [:feed reply]))]}))


(rf/reg-event-db
 :add-post
 (fn [db [_ post]]
   (update-in db [:posts] (fnil conj []) (index-by :id post))))

(rf/reg-event-fx
 :post
 (fn [cofx [_ text]]
   {:ssb/create-record {:type "post" :text text}}
   (fn [reply] 
     (rf/dispatch [:add-post reply]))))

(rf/reg-event-fx
 :reply
 (fn [cofx [_ reply-to text]]
   {:ssb/create-record (remove-nils {:type "post" 
                                     :root (rf/subscribe [:post/root reply-to])
                                     :branch (rf/subscribe [:post/branch reply-to])
                                     :text text})}))

(rf/reg-event-fx
 :private-post
 (fn [cofx [_ text recps root branch channel mentions]]
   (let [content (remove-nils {:type "post"
                               :text text
                               :root root
                               :branch branch
                               :channel channel
                               :mentions mentions
                               :recps (conj (set recps) @(rf/subscribe [:server/id]))})] ; add self
     {:ssb/private-post [content 
                         (fn [reply] (rf/dispatch [:add-post reply]))]
      :db (:db cofx)})))   

(rf/reg-event-fx
 :get-comments
 (fn [cofx [_ root]]
   {:ssb/get-thread root})) 

(rf/reg-event-db
 :comments
 (fn [db [_ target-id comment]]
   (update-in db [:comments target-id] (fnil conj []) comment)))

;;;;;;;;;;;
;; Blobs ;;
;;;;;;;;;;;

(rf/reg-event-db
 :blob/add-url
 (fn [db [_ blob-id url]]
   (assoc-in db [:blobs blob-id :url] url)))
