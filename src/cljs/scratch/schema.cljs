(ns scratch.schema
  (:require [clojure.spec.alpha :as s]))
;; change [org.clojure/clojure "1.9.0"]
;; https://clojure.org/guides/spec

(s/def ::id (s/and string? #(= (count %) 64)))          ;;better hash spec?
(s/def ::name (s/and string? #(<= 1 (count %) 64)))
(s/def ::description string?)
(s/def ::dirty? boolean?)
(s/def ::tags (s/coll-of string?))


(s/def ::abbrev (s/and string? #(<= 1 (count %) 6)))
(def unit-types #{"weight" "volume" "distance" "area"})
(s/def ::type unit-types)
(s/def ::unit (s/keys :req [::id ::name ::abbrev ::type]))

(s/def ::item (s/keys :req [::id ::name ::description]
                     :opt [::tags]))

(s/def ::items (s/coll-of ::id))
(s/def ::qty (s/map-of ::id int?))
(s/def ::units (s/map-op ::id ::id))
(s/def ::equipment (s/keys* :req [::items ::qty ::units]))
(s/def ::ingredients (s/keys* :req [::items ::qty ::units]))
(s/def ::procedure (s/coll-of strings?))
(s/def ::yields (s/keys* :req [::items ::qty ::units]))

(s/def ::task (s/keys :req [::id ::name]
                      :opt [::equiptment 
                            ::ingredients 
                            ::procedure 
                            ::yields]))

(s/def ::tasks (s/map-of ::id ::tasks))

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
