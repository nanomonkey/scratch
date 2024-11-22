(ns client.schema
  (:require [clojure.spec.alpha :as s]))


(s/def ::id (s/and string? #(= (count %) 44)))     ;;better hash spec?
(s/def ::non-blank-string (s/and string? (complement str/blank?)))
(s/def ::name (s/and string? #(<= 1 (count %) 64)))
(s/def ::description string?)
(s/def ::dirty? boolean?)
(s/def ::tags (s/coll-of string?))


(s/def ::abbrev (s/and string? #(<= 1 (count %) 6)))
(def unit-types #{"weight" "volume" "distance" "area"})  ;;revisit...
(s/def ::type unit-types)
(s/def ::unit (s/keys :req [::id ::name ::abbrev ::type]))

(s/def ::item (s/keys :req [::id ::name ::description]
                     :opt [::tags]))

(s/def ::items (s/map-of ::id ::item :distinct true))
(s/def ::qty (s/map-of ::id int? :distinct true))
(s/def ::units (s/map-of ::id ::id :distinct true))

(s/def ::line-item (s/map-of ::item ::qty ::unit))
(s/def ::equipment (s/coll-of ::line-item))
(s/def ::ingredients (s/coll-of ::line-item))
(s/def ::steps (s/coll-of string?))
(s/def ::yields (s/coll-of ::line-item))

(s/def ::task (s/keys :req [::id ::name]
                      :opt [::equipment 
                            ::ingredients 
                            ::steps
                            ::yields]))

(s/def ::tasks (s/map-of ::id ::tasks))

(s/def ::task-list (s/coll-of ::id :distinct true))
(s/def ::recipe (s/keys :req [::name ::description ::task-list]
                        :opt [::tags]))

(s/def ::recipes (s/map-of ::id ::recipe))

;; Person example, likely won't use
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)

(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))


;;TODO figure out how to test these correctly
(defn ed25519? [hash] (and (string? hash) (= (count hash) 80)))
(defn sha256? [hash] (and (string? hash) (= (count hash) 44)))
(defn json-object? [x] true)

;; From https://conan.is/blogging/clojure-spec-tips.html
;;
;; (s/def ::json-string (s/with-gen
;;                        (s/and string? #(try (json/decode %) true
;;                                             (catch Exception _ false)))
;;                        #(gen/fmap json/encode (s/gen any?))))
                       
;; (s/def ::uuid-string (s/with-gen
;;                        (s/and string? #(try (UUID/fromString %) true
;;                                             (catch Exception _ false)))
;;                        #(gen/fmap str (s/gen uuid?))))
                       
;; ;; Using https://github.com/dm3/clojure.java-time                       
;; (s/def ::date-string (s/with-gen
;;                        (s/and string? #(try (java-time/local-date-time %)
;;                                             (catch Exception _ false)))
;;                        #(gen/fmap (comp str (partial apply java-time/local-date-time))
;;                           (s/gen (s/tuple (s/int-in 1970 2050)
;;                                           (s/int-in 1 12)
;;                                           (s/int-in 1 29)
;;                                           (s/int-in 0 23)
;;                                           (s/int-in 0 59)
;;                                           (s/int-in 0 59))))))


(s/def ::hash-id ed25519?)
(s/def ::feed-id sha256?)
(s/def ::signature sha256?)

(s/def ::content (s/or string? json-object?))
(s/def ::message (s/map-of :previous ::hash-id 
                           :author ::feed-id
                           :sequence int?
                           :timestamp int?
                           :hash "sha256"
                           :content ::content
                           :signature ::signature))


;; Database Validation (WIP)

(defn validate
  "Returns nil if the db conforms to the spec, throws an exception otherwise"
  [spec db]
  (when goog.DEBUG
    (when-let [error (s/explain-data spec db)]
      (throw (ex-info 
               (str "DB spec validation failed: " 
                    (expound/expound-str spec db)) 
               error)))))

(def validate-interceptor (rf/after (partial validate :my/db)))

;; To use add validator to events:
;;
;; (rf/reg-event-db :some/event
;;   [validate-interceptor]
;;   (fn [db [event]]
;;     ;; handle event
;;     ))
