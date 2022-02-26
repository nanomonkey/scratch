(ns client.svg)

(defn active-time-icon [] 
  [:svg
   [:symbol {:viewBox "0 0 41 41" :id "active-time"}]
   [:circle {:cx "19.04" :cy "20.38" :r "17.04" :fill "none" :stroke "#cacaca" :stroke-width "4"}]
   [:path {:fill "#fff" :d "M19.04 20.38h21.97v19.28H19.04z"}]
   [:circle {:cx "19.55" :cy "21.44" :r "3.98" :fill "#cacaca"}]
   [:path {:d "M19 9.4h1v12h-1z" :fill "none" :stroke "#cacaca" :stroke-width "2.5"}]
   [:path {:d "M20.73 23.36l.46-.82 6.17 3.32-.46.82z" :fill "#fff" :stroke "#cacaca" :stroke-width "2.5"}]
   [:path {:d "M30 19.48l5.59 7.89 5.38-8z" :fill "#cacaca"}]])

(defn comment-icon []
  [:svg
   [:symbol {:viewBox "0 0 40 29" :id "comment"}] 
   [:path {:d "M1.5 0h37A1.54 1.54 0 0 1 40 1.5v18.2a1.54 1.54 0 0 1-1.5 1.5h-37A1.54 1.54 0 0 1 0 19.7V1.5A1.54 1.54 0 0 1 1.5 0z" :fill "#cacaca"}]
   [:path {:d "M19.9 18.8L6.9 29 4.1 13.2z" :fill "#cacaca"}]])

(defn heart-icon []
  [:svg
   [:symbol {:viewBox "0 0 38.72 35.83" :id "heart"}] 
   [:path {:class "cheart-path" :d "M26.8 2a9.89 9.89 0 0 0-7.44 3.37A9.91 9.91 0 0 0 2 11.92C2 23.08 15.64 29.28 19.36 33c3.72-3.72 17.36-9.92 17.36-21.08A9.92 9.92 0 0 0 26.8 2z"}]])

(defn image-icon []
  [:svg
   [:symbol {:viewBox "0 0 38.18 30" :id "image-icon"}]
   [:path {:d "M24.54 10.91a2.73 2.73 0 1 0-2.73-2.73 2.73 2.73 0 0 0 2.73 2.73zM25.91 15l-4.09 5.45L15 12.27 5.45 24.55h27.28zm9.54-15H2.73A2.73 2.73 0 0 0 0 2.73v24.54A2.73 2.73 0 0 0 2.73 30h32.72a2.73 2.73 0 0 0 2.73-2.73V2.73A2.73 2.73 0 0 0 35.45 0zm0 27.27H2.73V2.73h32.72v24.54z"}]])

(defn print-icon []
  [:svg
   [:symbol {:viewBox "0 0 30 30" :id "print"}]
   [:path {:d "M7.46 2.75h15.08a.5.5 0 0 1 .5.5v4.22a.5.5 0 0 1-.5.5H7.46a.5.5 0 0 1-.46-.5V3.25a.5.5 0 0 1 .46-.5z" 
           :fill "#a7a9ac" :fill-rule "evenodd"}]
   [:path {:d "M8.54 15.27h12.92a.5.5 0 0 1 .5.5v10.48a.5.5 0 0 1-.5.5H8.54a.5.5 0 0 1-.5-.5V15.77a.5.5 0 0 1 .5-.5z" 
           :fill "none" :stroke "#a7a9ac" :stroke-miterlimit "10"}]
   [:path {:d "M1.5 9h27a1.5 1.5 0 0 1 1.5 1.5V19a1.5 1.5 0 0 1-1.5 1.5h-27A1.5 1.5 0 0 1 0 19v-8.49A1.5 1.5 0 0 1 1.5 9z" 
           :fill "#a7a9ac" :fill-rule "evenodd"}]
   [:path {:d "M27.29 11.1a1 1 0 1 1-1.07 1 1.06 1.06 0 0 1 1.07-1z" :fill "#fff" :fill-rule "evenodd"}]])

(defn servings-icon []
  [:svg
   [:symbol {:viewBox "0 0 40 39.99" :id "servings"}]
   [:circle {:cx "20.12" :cy "20.07" :r "17.1" 
             :fill "#cacaca" :stroke "#cacaca" :stroke-width "4"}]
   [:path {:d "M35 37.07l-.8.6-12.9-17.1.8-.6zM26.82 2l.9.2-6 18-1-.2zM19.82 18.87l.2 1-4.95 1-13.25 2.8-.2-1z" 
           :fill "none" :stroke "#fff" :stroke-width "3"}]])

(defn share-icon []
  [:svg
   [:symbol {:viewBox "0 0 38 36" :id "share"}]
   [:circle {:cx "20.5" :cy "19.5" :r "14.5" :fill "none" :stroke "#cacaca" :stroke-width "3"}]
   [:circle {:cx "23.5" :cy "7.5" :r "6.5" :fill "#cacaca" :stroke "#f8f8f8" :stroke-width "2"}]
   [:circle {:cx "7.5" :cy "21.5" :r "6.5" :fill "#cacaca" :stroke "#f8f8f8" :stroke-width "2"}]
   [:circle {:cx "30.5" :cy "28.5" :r "6.5" :fill "#cacaca" :stroke "#f8f8f8" :stroke-width "2"}]])

(defn total-time-icon []
  [:svg
   [:symbol {:viewBox "0 0 36 36" :id "total-time"}]
   [:path {:d "M18 4A14 14 0 1 1 4 18 14 14 0 0 1 18 4m0-4a18 18 0 1 0 18 18A18 18 0 0 0 18 0z" 
           :fill "#cacaca"}]
   [:path {:d "M18.5 28.5h-1V16.6h1z" :fill "#cacaca" :stroke "#cacaca" :stroke-width "2.5"}]
   [:path {:d "M18 16l-.46.81-6.1-3.33.46-.81z" :fill "#cacaca" :stroke "#cacaca" :stroke-width "2.5"}]
   [:circle {:cx "18" :cy "17.06" :r "3.97" :fill "#cacaca"}]])

(defn trash-icon []
  [:svg
   [:symbol {:viewbox "0 0 36 36" :id "trash"}]
   [:path {:d "M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"}]])

(defn clear-list-icon []
  [:svg 
   [:symbol {:viewBox "0 0 24 24" :id "clear-list"}] 
   [:path {:d "M15 16h4v2h-4zm0-8h7v2h-7zm0 4h6v2h-6zM3 18c0 1.1.9 2 2 2h6c1.1 0 2-.9 2-2V8H3v10zM14 5h-3l-1-1H6L5 5H2v2h12z"}]])

(defn undo-icon []
  [:svg 
   [:symbol {:viewBox "0 0 24 24"}]
   [:path {:d "M12.5 8c-2.65 0-5.05.99-6.9 2.6L2 7v9h9l-3.62-3.62c1.39-1.16 3.16-1.88 5.12-1.88 3.54 0 6.55 2.31 7.6 5.5l2.37-.78C21.08 11.03 17.15 8 12.5 8z"}]]]

(defn redo-icon []
  [:svg
   [:symbol {:viewBox "0 0 24 24"}] 
   [:path {:d "M18.4 10.6C16.55 8.99 14.15 8 11.5 8c-4.65 0-8.58 3.03-9.96 7.22L3.9 16c1.05-3.19 4.05-5.5 7.6-5.5 1.95 0 3.73.72 5.12 1.88L13 16h9V7l-3.6 3.6z"}]]))
