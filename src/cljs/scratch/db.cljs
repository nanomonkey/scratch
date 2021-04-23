(ns scratch.db)

;; useful fn's for later
(defn index-by [key-fn coll]
    (into {} (map (juxt key-fn identity) coll)))

(def recipe-db
  {:loaded "r1"
   :active-panel :recipe
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
                 :duration "M1"
                 :equipment {:items ["i1"]
                             :qty {"i1" 1}
                             :units {"i1" "ea"}}
                 :ingredients {:items ["i3"]
                               :qty {"i3" 2}
                               :units {"i3" "usquart"}}
                 :steps ["fill pot with water"]
                 :yields {:items ["i5"]
                          :qty {"i5" 1}
                          :units {"i5" "ea"}}}
           "t2" {:id "t2"
                 :name "Bring to Boil"
                 :duration "M12S45"
                 :equipment {:items ["i2"]
                             :qty {"i2" 1}
                             :units {"i2" "ea"}}
                 :ingredients {:items ["i5"]
                               :qty {"i5" 1}
                               :units {"i5" "ea"}
                               :scaling {"i5" 1}}
                 :steps ["Turn stove on medium" "Leave until boiling"]
                 :yields {:items ["i4" "i5"]
                          :qty {"i4" 6
                                "i5" 1}
                          :units {"i4" "usquart"
                                  "i5" "usquart"}}}
           "t3" {:id "t3"
                 :name "Add Salt"
                 :ingredients {:items ["i6"]
                               :qty {"i6" {:whole 1 :numer 1 :denom 2}}
                               :units {"i6" "ustbsp"}
                               :scaling {"i6" 1}}
                 :optional {:items ["i7"]
                            :qty {"i7" 1}
                            :units {"i7" "ustbsp"}}
                 :steps ["Add salt and optional baking soda to boiling water, stir until dissolved." "Cool solution to body temperature."]
                 :yields {:items ["i4" "i5"]
                          :qty {"i4" 6
                                "i5" 1}
                          :units {"i4" "usquart"
                                  "i5" "usquart"}}}}
   :items {"i1" {:id "i1" :name "6 Qt. Pot" :description "" :tags []}
           "i2" {:id "i2" :name "stove" :description "" :tags []} 
           "i3" {:id "i3" :name "water" :description "" :tags []} 
           "i4" {:id "i4" :name "Boiled Water" :description "" :tags []} 
           "i5" {:id "i5" :name "full pot" :description "pot full of water" :tags []}
           "i6" {:id "i6" :name "Kosher Salt" :description "non-iodized kosher salt" :tags []}
           "i7" {:id "i7" :name "Baking Soda" :description "" :tags []}
           "i8" {:id "i8" :name "flour" :description "" :tags []}
           "i9" {:id "i9" :name "sugar" :description "" :tags []}
           "i10" {:id "i10" :name "yeast" :description "" :tags []}
           "i11" {:id "i11" :name "garlic" :description "" :tags []}
           "i12" {:id "i12" :name "red onions" :description "" :tags []}
           "i13" {:id "i13" :name "olive oil" :description "" :tags []}}
   :locations {"l1" {:id "l1" :name "Location 1" :description "Storage area in front of house" :address "123 My St."}
               "l2" {:id "l2" :name "Location 2" :description "Storage area in kitchen" :address "234 Yr Rd."}
               "l3" {:id "l3" :name "Location 3" :description "Walk in fridge" :address "345 My St."}}
   :inventory {"l1" {:items ["i5" "i6"]
                     :qty {"i5" 1
                           "i6" 4}
                     :units {"i5" "ea"
                             "i6" "uscup"}}
               "l2" {:items ["i2"] 
                     :qty {"i2" 1} 
                     :units {"i2" "ea"}}
               "l3" {:items ["i6"]  
                     :qty {"i6" {:whole 1 :numer 1 :denom 2}} 
                     :units {"i6" "uscup"}}}
   :suppliers {"s1" {:id "s1" 
                     :name "Supplier 1" 
                     :description "First supplier" 
                     :contact-info {:email "sup1@supplier.com"
                                    :address "123 Main St."}}}
   :cost-list {"s1" {:items ["i1"]
                     :price {"i1" {:denomination "2.50"
                                   :currency "usd"
                                   :qty {"i1" 6}
                                   :units {"i1" "bottle"}
                                   :duration {:start "2/1/2018"
                                              :end "2/28/2018"}}}}}
   :units {"beerbarrel" {:id "beerbarrel" :name "Beerbarrel" :abbrev "bbl" :type "volume"}
           "centilitre" {:id "centilitre" :name "Centilitre" :abbrev "L^3" :type "volume"}
           "cc" {:id "cc" :name "Cubic Centimeter" :abbrev "cm^3" :type "volume"}
           "cuft" {:id "cuft" :name "Cubic Foot" :abbrev "ft^3" :type "volume"}
           "cuin" {:id "cuin" :name "Cubic Inch" :abbrev "in^3" :type "volume"}
           "cuyd" {:id "cuyd" :name "Cubic Yard" :abbrev "yrd^3" :type "volume"}
           "bottle" {:id "bottle" :name "Bottle" :abbrev "btl" :type "count"}
           "impcup" {:id "impcup" :name "Cup (Imperial)" :abbrev "cp" :type "volume"}
           "uscup" {:id "uscup" :name "Cup (US Legal)" :abbrev "cp" :type "volume"}
           "decalitre" {:id "decalitre" :name "Decalitre" :abbrev "dal" :type "volume"}
           "decilitre" {:id "decilitre" :name "Decilitre" :abbrev "dL" :type "volume"}
           "drop" {:id "drop" :name "Drop" :abbrev "drp" :type "volume"}
           "fluiddram" {:id "fluiddram" :name "Fluid Dram" :abbrev "fl.dr" :type "volume"}
           "impfluidounce" {:id "impfluidounce" :name "Fluid Ounce (Imperial)" :abbrev "fl.oz" :type "volume"}
           "usflu:idounce" {:id "usflu:idounce" :name "Fluid Ounce (US)" :abbrev "fl.oz" :type "volume"}
           "impgallon" {:id "impgallon" :name "Gallon (Imperial)" :abbrev "gal" :type "volume"}
           "usgallon" {:id "usgallon" :name "Gallon (US)" :abbrev "gal" :type "volume"}
           "gill" {:id "gill" :name "Gill" :abbrev "gill" :type "volume"}
           "hectolitre" {:id "hectolitre" :name "Hectolitre" :abbrev "HL" :type "volume"}
           "hogshead" {:id "hogshead" :name "Hogshead" :abbrev "Hhd" :type "volume"}
           "litre" {:id "litre" :name "Litre"  :abbrev "L" :type "volume"}
           "millilitre" {:id "millilitre" :name "Millilitre"  :abbrev "mL" :type "volume"}
           "minim" {:id "minim" :name "Minim"  :abbrev "minim" :type "volume"}
           "oilbarrel" {:id "oilbarrel" :name "Oilbarrel" :abbrev "Obbl" :type "volume"}
           "pints" {:id "pints" :name "Pint (US)" :abbrev "pt" :type "volume"}
           "impquart" {:id "impquart" :name "Quart (Imperial)" :abbrev "qrt" :type "volume"}
           "usquart" {:id "usquart" :name "Quart (US)" :abbrev "qt" :type "volume"}
           "imptbsp" {:id "imptbsp" :name "Tablespoon (Imperial)" :abbrev "tbsp" :type "volume"}
           "ustbsp" {:id "ustbsp" :name "Tablespoon (US)" :abbrev "tbsp" :type "volume"}
           "imptsp" {:id "imptsp" :name "Teaspoon (Imperial)" :abbrev "tsp" :type "volume"}
           "ustsp" {:id "ustsp" :name "Teaspoon (US)" :abbrev "tsp" :type "volume"}
           "ea" {:id "ea" :name "Each" :abbrev "-" :type "count"}
           "part" {:id "part" :name "Part" :abbrev "part" :type "count"}
           "dozen" {:id "dozen" :name "Dozen" :abbrev "doz" :type "count"}
           "atomicmassunit" {:id "atomicmassunit" :name "Atomic Mass Unit" :abbrev "u" :type "mass"}
           "carat" {:id "carat" :name "Carat" :abbrev "ct" :type "mass"}
           "centigram" {:id "centigram" :name "Centigram" :abbrev "cg"  :type "mass"}
           "decigram" {:id "decigram" :name "Decigram" :abbrev "dg" :type "mass"}
           "dekagram" {:id "dekagram" :name "Dekagram" :abbrev "dag" :type "mass"}
           "dram" {:id "dram" :name "Dram" :abbrev "dr" :type "mass"}
           "grain" {:id "grain" :name "Grain" :abbrev "gr" :type "mass"}
           "gram" {:id "gram" :name "Gram" :abbrev "g" :type "mass"}
           "kilogram" {:id "kilogram" :name "Kilogram" :abbrev "kg" :type "mass"}
           "longton" {:id "longton" :name "Long Ton" :abbrev "LT" :type "mass"}
           "metricton" {:id "metricton" :name "Metric Ton" :abbrev "MT" :type "mass"}
           "microgram" {:id "microgram" :name "Microgram" :abbrev "mcg" :type "mass"}
           "milligram" {:id "milligram" :name "Milligram" :abbrev "mg" :type "mass"}
           "ounce" {:id "ounce" :name "Ounce" :abbrev "oz" :type "mass"}
           "picogram" {:id "picogram" :name "Picogram" :abbrev "pg" :type "mass"}
           "poundmass" {:id "poundmass" :name "Pound" :abbrev "lbs" :type "mass"}
           "stone" {:id "stone" :name "Stone" :abbrev "st" :type "mass"}
           "tola" {:id "tola" :name "Tola" :abbrev "tola" :type "mass"}
           "ton" {:id "ton" :name "Ton" :abbrev "T" :type "mass"}
           "troyounce" {:id "troyounce" :name "Troy Ounce" :abbrev "t oz" :type "mass"}
           "cm" {:id "cm" :name "Centimeter" :abbrev "cm" :type "length"}
           "chains" {:id "chains" :name "Chains" :type "length"}
           "foot" {:id "foot" :name "Feet" :abbrev "ft" :type "length"}
           "inch" {:id "inch" :name "Inch" :abbrev "in" :type "length"}
           "kilometer" {:id "kilometer" :name "Kilometer" :abbrev "km" :type "length"}
           "link" {:id "link" :name "Link" :abbrev "lnk" :type "length"}
           "meter" {:id "meter" :name "Meter" :abbrev "m" :type "length"}
           "micrometer" {:id "micrometer" :name "Micrometer" :abbrev "mcm" :type "length"}
           "mil" {:id "mil" :name "Mil" :abbrev "mil" :type "length"}
           "mile" {:id "mile" :name "Mile" :abbrev "mi" :type "length"}
           "millimeter" {:id "millimeter" :name "Millimeter" :abbrev "mm" :type "length"}
           "nanometer" {:id "nanometer" :name "Nanometer" :abbrev "nm" :type "length"}
           "picometer" {:id "picometer" :name "Picometer" :abbrev "pm" :type "length"}
           "rod" {:id "rod" :name "Rod" :abbrev "rod" :type "length"}
           "yard" {:id "yard" :name "Yard" :abbrev "yrd" :type "length"}}
   :events ["e1" {:id "e1"
                  :name "Reoccurring Event"
                  :location "l1"
                  :participants ["g1"]
                  :tasks ["r1"]
                  :duration {:start "yyyy-mm-ddTHH:MM:SS.sssZ."
                             :end "yyyy-mm-ddTHH:MM:SS.sssZ."
                             :reoccurring "weekly"}}
            "e2" {:id "e2"
                  :name "One-off Event"
                  :location "l1"
                  :participants ["g1"]
                  :tasks ["t1"]
                  :duration {:start "yyyy-mm-ddTHH:MM:SS.sssZ."}}]})


