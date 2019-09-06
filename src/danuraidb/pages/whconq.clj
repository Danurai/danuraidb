'(in-ns danuraidb.pages)

(def whconq-pretty-head
  (into pretty-head (h/include-css "/css/whconq-style.css?v=1.0")))
  
(defn whconq-navbar [req]
  (navbar 
    "/img/whconq/icons/skull_white.png" 
    "WH40k DB" 
    "whconq"
    ["decks" "cards"]
    req
    :style "background-color: darkblue !important;"))
                  
(defn whconq-home [ req ]
  (h/html5
    whconq-pretty-head
    [:body
      (whconq-navbar req)
      [:body
        [:div.container.my-3
          [:div.row
            [:a {:href "/whconq/decks"} "Login"] 
            [:span.ml-1 "to see your decks"]]]]]))
