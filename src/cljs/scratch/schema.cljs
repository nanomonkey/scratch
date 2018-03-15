(ns scratch.schema
  (:require [clojure.spec.alpha :as s]))
;; change [org.clojure/clojure "1.9.0"]
;; https://clojure.org/guides/spec

(s/def ::id 
(s/def ::name (s/and string? #(<= 1 (count %) 64)))
(s/def ::description string?)
(s/def ::dirty? boolean?)
(s/def ::tags (s/coll-of string?))


(s/def ::abbrev (s/and string? #(<= 1 (count %) 6)))
(def unit-types #{"weight" "volume" "distance" "area"})
(s/def ::type unit-types)
(s/def ::unit (s/keys :req [::name ::abbrev ::type]))

(s/def ::item (s/keys :req [::name ::description]
                     :opt [::tags]))

(s/def ::task (s/keys :req [::name ::description]
                     :opt [::tags]))

(s/def ::task-list (s/coll-of ::id :distinct true))
(s/def ::recipe (s/keys :req [::name ::description ::task-list]
                        :opt [::tags]))


;; Person
(def email-regex #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,63}$")
(s/def ::email-type (s/and string? #(re-matches email-regex %)))

(s/def ::first-name string?)
(s/def ::last-name string?)
(s/def ::email ::email-type)

(s/def ::person (s/keys :req [::first-name ::last-name ::email]
                        :opt [::phone]))
