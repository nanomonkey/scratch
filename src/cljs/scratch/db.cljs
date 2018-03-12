(ns scratch.db)
(def recipe-db
  {:recipes {"r1" {:id "r1"
                   :name "Stone Soup"
                   :description "Soup made by friends"
                   :tags #{"soup"}
                   :task-list ["t1" "t2"]}}
   :tasks {"t1" {:id "t1"
                 :name "Fill pot"
                 :equipment {:items ["i1"]
                             :qty {"i1" 1}
                             :units {"i1" "u1"}}
                 :ingredients {:items ["i3"]
                               :qty {"i3" 2}
                               :units {"i3" "u2"}}
                 :procedure ["fill pot with water"]
                 :yields {:items ["i5"]
                          :qty {"i5" 1}
                          :units {"i5" "u1"}}}
           "t2" {:id "t2"
                 :name "Bring to Boil"
                 :equipment {:items ["i2"]
                             :qty {"i2" 1}
                             :units {"i2" "u1"}}
                 :ingredients {:items ["i5"]
                               :qty {"i5" 1}
                               :units {"i5" "u1"}
                               :scaling {"i5" 1}}
                 :procedure ["Turn stove on medium" "Leave until boiling"]
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
                 :tags []}}
   :units {"u1" {:id "u1"
                 :name "each"
                 :abbrev ""
                 :type "volume"}
           "u2" {:id "u2"
                 :name "quarts"
                 :abbrev "qts"
                 :type "volume"}}})
