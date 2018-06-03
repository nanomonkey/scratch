(ns scratch.db)

(def recipe-db
  {:loaded-recipe "r1"
   :temp-id 0
   :recipes {"r1" {:id "r1"
                   :name "Stone Soup"
                   :description "Soup made by friends"
                   :tags #{"soup"}
                   :task-list ["t1" "t2"]}
             "r2" {:id "r2"
                   :name "Another Recipe"
                   :description "Another description..."
                   :tags #{"test"}
                   :task-list ["t2"]}
             "r3" {:id "r3"
                   :name "Neti Pot solution"
                   :description "..."
                   :tags #{}
                   :task-list ["t1" "t2" "t3"]}}
   :tasks {"t1" {:id "t1"
                 :name "Fill pot"
                 :time {:min 4
                        :max 8
                        :units "min"}
                 :equipment {:items ["i1"]
                             :qty {"i1" 1}
                             :units {"i1" "u1"}}
                 :ingredients {:items ["i3"]
                               :qty {"i3" 2}
                               :units {"i3" "u2"}}
                 :steps ["fill pot with water"]
                 :yields {:items ["i5"]
                          :qty {"i5" 1}
                          :units {"i5" "u1"}}}
           "t2" {:id "t2"
                 :name "Bring to Boil"
                 :duration "M12S45"
                 :equipment {:items ["i2"]
                             :qty {"i2" 1}
                             :units {"i2" "u1"}}
                 :ingredients {:items ["i5"]
                               :qty {"i5" 1}
                               :units {"i5" "u1"}
                               :scaling {"i5" 1}}
                 :steps ["Turn stove on medium" "Leave until boiling"]
                 :yields {:items ["i4" "i5"]
                          :qty {"i4" 6
                                "i5" 1}
                          :units {"i4" "u2"
                                  "i5" "u2"}}}
           "t3" {:id "t3"
                 :name "Add Salt"
                 :equipment {:items ["i2"]
                             :qty {"i2" 1}
                             :units {"i2" "u1"}}
                 :ingredients {:items ["i6"]
                               :qty {"i6" {:numer 1 :denom 2}}
                               :units {"i6" "u3"}
                               :scaling {"i6" 1}}
                 :optional {:items ["i7"]
                            :qty {"i7" 1}
                            :units {"i7" "u3"}}
                 :steps ["Add salt and optional baking soda to boiling water, stir until dissolved." "Cool solution to body temperature."]
                 :yields {:items ["i4" "i5"]
                          :qty {"i4" 6
                                "i5" 1}
                          :units {"i4" "u2"
                                  "i5" "u2"}}}}
   :items {"i1" {:id "i1"
                 :name "6 Qt. Pot"
                 :description ""
                 :tags []}
           "i2" {:id "i2"
                 :name "stove"
                 :description ""
                 :tags []}
           "i3" {:id "i3"
                 :name "water"
                 :description ""
                 :tags []}
           "i4" {:id "i4"
                 :name "Boiled Water"
                 :description ""
                 :tags []}
           "i5" {:id "i5"
                 :name "full pot"
                 :description "pot full of water"
                 :tags []}
           "i6" {:id "i6"
                 :name "Kosher Salt"
                 :description "non-iodized kosher salt"
                 :tags []}
           "i7" {:id "i7"
                 :name "Baking Soda"
                 :description ""
                 :tags []}}
   :units {"u1" {:id "u1"
                 :name "each"
                 :abbrev ""
                 :type "volume"}
           "u2" {:id "u2"
                 :name "quarts"
                 :abbrev "qts"
                 :type "volume"}
           "u3" {:id "u3"
                 :name "teaspoon"
                 :abbrev "tsp"
                 :type "volume"}}})


;; useful fn's for later
(defn index-by [key-fn coll]
    (into {} (map (juxt key-fn identity) coll)))
