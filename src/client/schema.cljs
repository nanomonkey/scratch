(ns client.schema
  (:require [clojure.spec.alpha :as s]))


(s/def ::id (s/and string? #(= (count %) 44)))     ;;better hash spec?
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
(s/def ::line-items (s/keys* :req [::items ::qty ::units]))
(s/def ::equipment ::line-items)
(s/def ::ingredients ::line-items)
(s/def ::steps (s/coll-of string?))
(s/def ::yields ::line-items)

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
