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


(defn- deck-export-string [deck]
  (clojure.string/join "\n"
    (apply concat 
      (map (fn [category] 
        (concat [category] 
                (->> deck 
                    (filter #(= (-> % :category :en) category)) 
                    (map #(str (:count %) "x " (:name %))))))
        ["Champion" "Blessing" "Unit" "Spell" "Ability"]))))
        
(defn- write-whconq-deck-list [ cards ]
  nil)
          
(defn- whconq-deck-card [d card-data]
  (let [deck-cards nil] ;(get-aosc-deck-cards (-> d :data model/parsedeck :cards) aosc-card-data)]
    [:li.list-group-item.list-deck-card
      [:div.d-flex.justify-content-between {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} 
        [:div
          [:div.h3.mr-2 (:name d)]]]
        ;[:div.bold.text-center.cardcounts
        ;  [:div
        ;    [:img.deck-icon.mr-1 {:src (str aosc_icon_path "quest_unit.png")}]
        ;    [:span.mr-1 (->> deck-cards (filter #(= (-> % :category :en) "Unit")) (map :count) (reduce +) )]]
        ;  [:div 
        ;    [:img.deck-icon.mr-1 {:src (str aosc_icon_path "quest_spell.png")}]
        ;    [:span.mr-1 (->> deck-cards (filter #(= (-> % :category :en) "Spell")) (map :count) (reduce +) )]]
        ;  [:div 
        ;    [:img.deck-icon.mr-1 {:src (str aosc_icon_path "quest_ability.png")}]
        ;    [:span.mr-1 (->> deck-cards (filter #(= (-> % :category :en) "Ability")) (map :count) (reduce +) )]]]]
      [:div.collapse.mb-2 {:id (str "deck_" (:uid d))} 
        (write-whconq-deck-list deck-cards)
        [:div
          [:button.btn.btn-sm.btn-danger.mr-1 {:data-toggle "modal" :data-target "#deletemodal" :data-name (:name d) :data-uid (:uid d)} [:i.fas.fa-times.mr-1] "Delete"]
          [:button.btn.btn-sm.btn-success.mr-1 {:data-toggle "modal" :data-target "#exportdeck" :data-export (deck-export-string deck-cards) :data-deckname (:name d)} [:i.fas.fa-file-export.mr-1] "Export"]
          [:a.btn.btn-sm.btn-primary {:href (str "/aosc/decks/edit/" (:uid d))} [:i.fas.fa-edit.mr-1] "Edit"]]]]))  
          
(defn whconq-decks [req]
  (let [decks (db/get-user-decks 3 (-> req get-authentications (get :uid 1002)))
        card-data nil]
    (h/html5
      whconq-pretty-head
      [:body
        (whconq-navbar req)
        [:div.container.my-3
          [:div.row.justify-content-between
            [:div.h3 (str "Decks (" (count decks) ")")]
            [:div 
              ;[:button#exportall.btn.btn-secondary.mr-1 {:title "Export to JSON" :data-export (json/write-str (map #(select-keys % [:name :data]) decks))} [:i.fas.fa-clipboard]]
              ;[:button#importall.btn.btn-secondary.mr-1 {:title "Import from JSON" :data-toggle "modal" :data-target "#importallmodal"} [:i.fas.fa-paste]] 
              [:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
              [:a.btn.btn-primary {:href "/whconq/decks/new" :title "New Deck"} [:i.fas.fa-plus]]]]
          [:div.row
            [:div#decklists.w-100
              [:ul.list-group
                (map (fn [d] (whconq-deck-card d card-data)) decks)]]]]
        (deletemodal)
        (importallmodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/whconq_decklist.js?v=1.0")])))
        
(defn whconq-deckbuilder [ req ]
  nil)
  
(defn whconq-cards [ req ]
  (h/html5
    whconq-pretty-head
    [:body
      [:div.container.my-3
        [:div "cards"]]]))