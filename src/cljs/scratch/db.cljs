(ns scratch.db)
(def recipe-db
  {:recipes {"r1" {:id "r1"
                   :name "Stone Soup"
                   :description "Soup made by friends"
                   :tags #{"soup"}
                   :task-list ["t1" "t2"]}}
   :tasks {"t1" {:id "t1"
                 :name "Fill pot"
                 :equipment ["i1"]
                 :ingredients {:items ["i2" "i3"]
                               :qty {"i2" 1
                                     "i3" 2}
                               :units {"i2" "u1"
                                       "i3" "u2"}}
                 :procedure "-fill pot with water"
                 :yields []}
           "t2" {:id "t2"
                 :name "Bring to Boil"
                 :equipment {:items ["i4"]
                             :qty {"i4" 1}
                             :unit {"i4" "u1"}}
                 :ingredients {:items ["i2"]
                               :qty {"i2" 1}
                               :units {"i2" "u1"}
                               :scaling {"i2" 1}}
                 :procedure "-turn stove on medium \n-leave until boiling"
                 :products {:items ["i4"]
                            :qty {"i4" 6}
                            :units {"i4" "u2"}}}}
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
                 :tags []}}
   :units {"u1" {:id "u1"
                 :name "each"
                 :abbrev "ea."
                 :type "volume"}
           "u2" {:id "u2"
                 :name "quarts"
                 :abbrev "qts"
                 :type "volume"}}})
