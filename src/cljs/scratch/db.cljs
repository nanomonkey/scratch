(ns scratch.db)

(def recipe-db
  {:id "r1234"
   :name "Stone Soup!"
   :description "Soup made by friends"
   :tags #{"soup"}
   :tasks {"task1" {:name "Fill pot"
                    :instructions "- fill pot with water"
                    :equipment [{:qty 1 
                                 :unit {"u123" {:id "u123"
                                            :name "each"}}
                                 :item {"i345" {:id "i345" 
                                                :name "pot"}}}]
                    :items [{:qty 1 
                             :unit {"u123" {:id "u123"
                                            :name "each"}}
                             :item {"i123" {:id "i123"
                                            :name "water"}}}]}
           "task2" {:name "Bring to Boil"
                    :instructions "- Turn on Stove and bring to boil."
                    :equipment []
                    :items []
                    :products [{:qty 1
                                :unit {"u123" {:id "u123"
                                               :name "each"}}
                                :item {"i234" {:id "i234"
                                               :name "boiled water"}}}]}}})
(comment
  (def db
    {:recipes {"r1" {:id "r1"
                     :name "Stone Soup"
                     :description "Soup made by friends"
                     :tags #{"soup"}
                     :tasks ["t1" "t2"]}}
     :tasks {"t1" {:id "t1"
                   :name "Fill pot"
                   :equipment ["i1"]
                   :items ["i2" "i3"]
                   :instructions "-fill pot with water"
                   :products []}
             "t2" {:id "t2"
                   :name "Bring to Boil"
                   :equipment ["i4"]
                   :items [{:qty 1
                            :unit "u1"
                            :item "i2"}]
                   :products ["i4"]}}}
    :items {"i1" {:id "i1"
                  :name "6 Qt. Pot"}
            "i2" {:id "i2"
                  :name "stove"}
            "i3" {:id "i3"
                  :name "water"}
            "i4" {:id "i4"
                  :name "Boiled Water"}}
    :units {"u1" {:id "u1"
                  :name "each"}
            "u2" {:id "u2"
                  :name "quarts"}}))
