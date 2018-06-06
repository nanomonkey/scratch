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
                               :qty {"i6" {:whole 1 :numer 1 :denom 2}}
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
   :units {"beerbarrel" 
           {:id "beerbarrel" :name "Beerbarrel" :abbrev "bbl" :type "volume"}
           "centilitre" 
           {:id "centilitre" :name "Centilitre" :abbrev "L^3" :type "volume"}
           "cc" {:id "cc" :name "Cubic Centimeter" :abbrev "cm^3" :type "volume"}
           "cuft" {:id "cuft" :name "Cubic Foot" :abbrev "ft^3" :type "volume"}
           "cuin" {:id "cuin" :name "Cubic Inch" :abbrev "in^3" :type "volume"}
           "cuyd" {:id "cuyd" :name "Cubic Yard" :abbrev "yrd^3" :type "volume"}

           {:id "impcup" :name "Cup (Imperial)" :abbrev "cp" :type "volume"}
           {:id "uscup" :name "Cup (US Legal)" :abbrev "cp" :type "volume"}
           {:id "decalitre" :name "Decalitre" :abbrev "dal" :type "volume"}
           {:id "decilitre" :name "Decilitre" :abbrev "dL" :type "volume"}
           {:id "drop" :name "Drop" :abbrev "drp" :type "volume"}
           {:id "fluiddram" :name "Fluid Dram" :abbrev "fl.dr" :type "volume"}
           {:id "impfluidounce" :name "Fluid Ounce (Imperial)" :abbrev "fl.oz" :type "volume"}
           {:id "usflu:idounce" :name "Fluid Ounce (US)" :abbrev "fl.oz" :type "volume"}
           {:id "impgallon" :name "Gallon (Imperial)" :abbrev "gal" :type "volume"}
           {:id "usgallon" :name "Gallon (US)" :abbrev "gal" :type "volume"}
           {:id "gill" :name "Gill" :abbrev "gill" :type "volume"}
           {:id "hectolitre" :name "Hectolitre" :abbrev "HL" :type "volume"}
           {:id "hogshead" :name "Hogshead" :abbrev "Hhd" :type "volume"}
           {:id "litre" :name "Litre"  :abbrev "L" :type "volume"}
           {:id "millilitre" :name "Millilitre"  :abbrev "mL" :type "volume"}
           {:id "minim" :name "Minim"  :abbrev "minim" :type "volume"}
           {:id "oilbarrel" :name "Oilbarrel" :abbrev "Obbl" :type "volume"}
           {:id "pints" :name "Pint (US)" :abbrev "pt" :type "volume"}
           {:id "impquart" :name "Quart (Imperial)" :abbrev "qrt" :type "volume"}
           {:id "usquart" :name "Quart (US)" :abbrev "qt" :type "volume"}
           {:id "imptbsp" :name "Tablespoon (Imperial)" :abbrev "tbls" :type "volume"}
           {:id "ustbsp" :name "Tablespoon (US)" :abbrev "tbls" :type "volume"}
           {:id "imptsp" :name "Teaspoon (Imperial)" :abbrev "tsp" :type "volume"}
           {:id "ustsp" :name "Teaspoon (US)" :abbrev "tsp" :type "volume"}
           {:id "ea" :name "Each" :abbrev "-" :type "count"}
           {:id "dozen" :name "Dozen" :abbrev "doz" :type "count"}
           {:id "atomicmassunit" :name "Atomic Mass Unit" :abbrev "u" :type "mass"}
           {:id "carat" :name "Carat" :abbrev "ct" :type "mass"}
           {:id "centigram" :name "Centigram" :abbrev "cg"  :type "mass"}
           {:id "decigram" :name "Decigram" :abbrev "dg" :type "mass"}
           {:id "dekagram" :name "Dekagram" :abbrev "dag" :type "mass"}
           {:id "dram" :name "Dram" :abbrev "dr" :type "mass"}
           {:id "grain" :name "Grain" :abbrev "gr" :type "mass"}
           {:id "gram" :name "Gram" :abbrev "g" :type "mass"}
           {:id "kilogram" :name "Kilogram" :abbrev "kg" :type "mass"}
           {:id "longton" :name "Long Ton" :abbrev "LT" :type "mass"}
           {:id "metricton" :name "Metric Ton" :abbrev "MT" :type "mass"}
           {:id "microgram" :name "Microgram" :abbrev "mcg" :type "mass"}
           {:id "milligram" :name "Milligram" :abbrev "mg" :type "mass"}
           {:id "ounce" :name "Ounce" :abbrev "oz" :type "mass"}
           {:id "picogram" :name "Picogram" :abbrev "pg" :type "mass"}
           {:id "poundmass" :name "Pound" :abbrev "lbm" :type "mass"}
           {:id "stone" :name "Stone" :abbrev "st" :type "mass"}
           {:id "tola" :name "Tola" :abbrev "tola" :type "mass"}
           {:id "ton" :name "Ton" :abbrev "T" :type "mass"}
           {:id "troyounce" :name "Troy Ounce" :abbrev "t oz" :type "mass"}
           {:id "cm" :name "Centimeter" :abbrev "cm" :type "length"}
           {:id "chains" :name "Chains" :type "length"}
           {:id "foot" :name "Feet" :abbrev "ft" :type "length"}
           {:id "inch" :name "Inch" :abbrev "in" :type "length"}
           {:id "kilometer" :name "Kilometer" :abbrev "km" :type "length"}
           {:id "link" :name "Link" :abbrev "lnk" :type "length"}
           {:id "meter" :name "Meter" :abbrev "m" :type "length"}
           {:id "micrometer" :name "Micrometer" :abbrev "mcm" :type "length"}
           {:id "mil" :name "Mil" :abbrev "mil" :type "length"}
           {:id "mile" :name "Mile" :abbrev "mi" :type "length"}
           {:id "millimeter" :name "Millimeter" :abbrev "mm" :type "length"}
           {:id "nanometer" :name "Nanometer" :abbrev "nm" :type "length"}
           {:id "picometer" :name "Picometer" :abbrev "pm" :type "length"}
           {:id "rod" :name "Rod" :abbrev "rod" :type "length"}
           {:id "yard" :name "Yard" :abbrev "yrd" :type "length"}}})


;; useful fn's for later
(defn index-by [key-fn coll]
    (into {} (map (juxt key-fn identity) coll)))
