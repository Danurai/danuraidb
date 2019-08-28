'(in-ns danuraidb.pages)

(def whuw-pretty-head
  (into pretty-head (h/include-css "/css/whuw-style.css?v=1.0")))
  
(defn whuw-navbar [req]
  (navbar 
    "/img/aosc/icons/quest_ability.png" 
    "WHUW DB" 
    "whuw"
    ["decks" "collection" "cards"]
    req
    :style "background-color: darkred !important;"))
                  
(defn whuw-home [ req ]
  (h/html5
    whuw-pretty-head
    [:body
      (whuw-navbar req)
      [:body
        [:div.container.my-3
          [:div.row
            [:a {:href "/whuw/decks"} "Login"] 
            [:span.ml-1 "to see your decks"]]]]]))