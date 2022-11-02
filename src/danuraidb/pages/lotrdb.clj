(in-ns 'danuraidb.pages)

(def lotrdb-pretty-head
  (into pretty-head (h/include-css "/css/lotrdb-style.css?v=1.0")))
  
(def player_type_icons [
  {:name "Hero" :code "hero" :symbol [:span.lotr-type-hero]}
  {:name "Ally" :code "ally" :symbol [:span.lotr-type-ally]}
  {:name "Attachment" :code "attachment" :symbol [:span.lotr-type-attachment]}
  {:name "Event" :code "event" :symbol [:span.lotr-type-event]}])

(def type-icon (apply merge (map #(hash-map (:code %) (:symbol %)) player_type_icons)))
  
(def sphere_icons
  (map #(let [code (clojure.string/lower-case %)] 
          (hash-map :name % :code code :symbol [:span {:class (str "lotr-type-" code)}]))
    ["Leadership","Lore","Spirit","Tactics","Neutral"]))  ;,"Baggins","Fellowship","Boon"

(defn- lotrdb-markdown [ txt ]
  (if txt
    (->> (clojure.string/replace txt #"[A-Z]\w+\s\w+:|[A-Z]\w+:" #(str "<b>" %1 "</b>"))
          (re-seq #"\[\w+\]|\w+|." )
          (map #(model/convert "lotr-type-" %))
          model/makespan)
    txt))
              
(defn lotrdb-navbar [req]
  (navbar 
    "/img/lotrdb/icons/sphere_fellowship.png" 
    "LotR DB" 
    "lotrdb"
    ["decks" "packs" "scenarios" "search|physical|digital" "folders" "quest log" "solo"]
    req))
    
(defn lotrdb-home [ req ]
  (h/html5
    lotrdb-pretty-head
    [:body
      (lotrdb-navbar req)
      [:body
        [:div.container.my-3
          [:div.row
            [:a {:href "/lotrdb/decks"} "Login"] 
            [:span.ml-1 "to see your decks"]]]]]))
            

(defn- singletoast []
  [:div {:aria-live "polite" :aria-atomic "true" :style "position: relative"}
    [:div#toast.toast {:style "position: absolute; top: 10px; right: 10px; z-index: 1050; min-width: 200px;"}
      [:div.toast-header
        [:i.fas.fa-exclamation.text-warning.mr-2]
        [:div.toast-title.mr-auto ]
        [:button.ml-2.mb-2.close {:data-dismiss "toast" :type "button"} [:span {:aria-hidden "true"} "x"]]]
      [:div.toast-body "Sample Text"]]])
      
(defn- modal []
  [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header
          [:h5.modal-title.w-100 ""]
          [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
            [:span {:aria-hidden "true"} "\u00d7"]]]
        [:div.modal-body]]]])
       
(defn- loadmodal []
  [:div#loadmodal.modal {:tab-index -1 :role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header
          [:h5.modal-title "Load Deck"]
          [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
            [:span {:aria-hidden "true"} "\u00d7"]]]
        [:div.modal-body
          [:ul.list-group]]
        [:div.modal-footer
          [:button.btn.btn-outline-secondary {:data-dismiss "modal"} "Cancel"]]]]])     
        
(defn- deletegroupmodal []
  [:div#deletegroupmodal.modal {:tabindex -1 :role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header
          [:h5.modal-title "Confirm Delete"]
          [:button {:type "button" :class "close" :data-dismiss "modal"} 
            [:span "x"]]]
        [:div.modal-body]
        [:div.modal-footer
          [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
          [:form {:action "/decks/fellowship/delete" :method "post"}
            [:input#deletemodalfname {:name "name" :hidden true}]
            [:input#deletemodalfuid {:name "uid" :hidden true}]
            [:button.btn.btn-danger {:submit "true"} "OK"]]]]]])


(defn- lotrdb-card-icon [ card ]
  [:img.icon-sm.float-right {
    :src (str "/img/lotrdb/icons/"
              (if (some? (:sphere_name card)) 
                  (str "sphere_" (:sphere_code card))
                  (str "pack_" (:pack_code card)))
              ".png")}])
             
;;;;;;;;;;;;;             
; SCENARIOS ;
;;;;;;;;;;;;;

; SEARCH

(defn lotrdb-search-digital [ req ]
  (let [q (or (-> req :params :q) "")
       view (or (-> req :params :view) "")
       sort (or (-> req :params :sort) "code")
       sortfield (case sort
                  "sphere" :sphere_code
                  "type"   :type_code
                  (keyword sort))
       sortfn (case sort
                ("sphere" "type" "code" "name") #(compare %1 %2)
                #(compare %2 %1))
       results (sort-by sortfield sortfn (model/cardfilter q (model/get-lotracg-cards) :lotrdb))]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:form {:action "/lotrdb/search/digital" :method "GET"}
            [:div.row
              [:div.col-sm-4.mb-2
                [:div.input-group
                  [:input.form-control.search-info {:type "text" :name "q" :value q}]
                  [:div.input-group-append
                    [:button.btn.btn-primary.mr-2 {:role "submit"} "Search"]]]]
              [:div.col-sm-4.mb-2
                [:select.form-control {:type "select" :name "view"}
                  [:option {:selected (= view "list") :value "list"} "View as list"]
                  [:option {:selected (= view "cards") :value "cards"} "View as cards"]]]
              [:div.col-sm-4.mb-2
                [:select.form-control {:type "select" :name "sort"}
                  (for [s ["code" "name" "type" "sphere" "threat" "willpower" "attack" "defense" "health"]]
                    [:option {:value s :selected (= (name sort) s)} (str "Sort by " s)])]]]]
          [:div.row
            (if (= view "cards")
              (for [card results :let [q (:quantity card) d (:difficulty card 0)]]
                [:div.col-4
                  [:div.mb-2 {:style "position: relative; display: inline-block;" :title "Quantity (normal/difficulty)"}
                    [:img.img-fluid.card-link {:data-code (:code card) :src (str "https://digital.ringsdb.com/" (:imagesrc card))}]
                    [:div.px-2 {:style "font-size: 1.25em; color: black; position: absolute; right: 5%; bottom: 5%; opacity: 0.7; background-color: white; border-radius: 15%;"}
                      (if (contains? #{"enemy" "treachery" "location"} (:type_code card))
                          (str (- q d) "/" d)
                          q)]
                    ]]) ;(or (:cgdbimgurl card) (model/get-card-image-url card))}]])
              [:div.col
                [:table#tblresults.table.table-sm.table-hover
                  [:thead [:tr  
                    [:th.sortable.d-none.d-md-table-cell "Code"]
                    [:th.sortable "Name"]
                    [:th.sortable.text-center {:title "Threat/Cost"} [:span.lotr-type-threat] "/C." ]
                    [:th.sortable.text-center {:title "Willpower"} [:span.lotr-type-willpower]]
                    [:th.sortable.text-center {:title "Attack"} [:span.lotr-type-attack]]
                    [:th.sortable.text-center {:title "Defense"} [:span.lotr-type-defense]]
                    [:th.sortable.text-center {:title "Health"} [:span.lotr-type-health]]
                    [:th.sortable "Type"]
                    [:th.sortable "Sphere"]
                    [:th.sortable.d-none.d-md-table-cell "Set"]
                    [:th.text-center {:title "Quantity (Normal/Difficulty)"} "Qty."]]]
                  [:tbody#bodyresults
                    (for [card results]
                      [:tr
                        [:td.d-none.d-md-table-cell (:code card)]
                        [:td [:a.card-link-digital {:data-code (:code card) :href (str "/lotrdb/card/digital/" (:code card))} (:name card)]]
                        [:td.text-center (or (:threat card) (:cost card))]
                        [:td.text-center (:willpower card)]
                        [:td.text-center (:attack card)]
                        [:td.text-center (:defense card)]
                        [:td.text-center (:health card)]
                        [:td (:type_name card)]
                        [:td (:sphere_name card)]
                        [:td.d-none.d-md-table-cell (str (:pack_name card) " #" (:position card))]
                        [:td.text-center 
                          (if (contains? #{"enemy" "treachery" "location"} (:type_code card))
                              (let [q (:quantity card) d (:difficulty card 0)]
                                (str (- q d) "/" d))
                              (:quantity card))]])]]])]]
      (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")])))

      
; CARD PAGE ;
;;;;;;;;;;;;;

(defn- prev-card [ cards code ]
  (->> cards  
      (sort-by :code)
      (take-while #(not= (:code %) code))
      (take-last 2)
      last))
      
(defn- next-card [ cards code ]
  (->> cards 
      (sort-by :code #(compare %2  %1))
      (take-while #(not= (:code %) code))
      (take-last 2)
      last))

;; CARD PAGE ;;
;;;;;;;;;;;;;;;

(defn lotrdb-card-page [ id ]
  (let [cards  (model/get-cards)
        card   (->> cards (filter #(= (:code %) id)) first)
        prc    (prev-card cards id)
        nxc    (next-card cards id)]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar nil)
        [:div.container.my-3
          [:div.row.mb-2
            [:div.col
              (if (some? prc)
                [:a.btn.btn-primary.card-link
                  {:href (str "/lotrdb/card/" (:code prc)) :data-code (:code prc)}
                  [:div (:name prc)][:small (str (:pack_name prc) " #" (:position prc))]]
                [:button.btn.button-primary])
              (if (some? nxc)
                [:a.btn.btn-primary.card-link.float-right
                  {:href (str "/lotrdb/card/" (:code nxc)) :data-code (:code nxc)}
                  [:div (:name nxc)][:small (str (:pack_name nxc) " #" (:position nxc))]])]]
          [:div.row
            [:div.col-sm-6
              [:div.card 
                [:div.card-header
                  [:span.h3.card-title 
                    (if (:is_unique card)
                      [:i.lotr-type-unique.unique-icon])
                    (:name card)]
                  (lotrdb-card-icon card)]
                [:div.card-body
                  [:div.text-center [:b (:traits card)]]
                  [:div {:style "white-space: pre-wrap;"} (-> card :text lotrdb-markdown)]
                  [:div.mt-1.pl-2.my-2.text-right	 [:em {:style "white-space: pre-wrap;"} (:flavor card)]]
                  [:div.d-flex
                    [:div [:small.text-muted (str (:pack_name card) " #" (:position card))]]
                    [:div.ml-auto [:i.fa.fa-solid.fa-code {:title (str card) :onclick (str "function e() {alert('" card "')};e()")}]]]
                    ]]]
            [:div.col-sm-6
              [:img {:src (:cgdbimgurl card)}]]]] ;(or (:cgdbimgurl card) (model/get-card-image-url card))}]]]]
      (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")])))


(defn lotrdb-digital-card-page [ id ]
  (let [cards  (model/get-lotracg-cards)
        card   (->> cards (filter #(= (:code %) id)) first)
        prc    (prev-card cards id)
        nxc    (next-card cards id)
        ]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar nil)
        [:div.container.my-3
          [:div.row.mb-2
            [:div.col
              (if (some? prc)
                [:a.btn.btn-primary.card-link
                  {:href (str "/lotrdb/card/digital/" (:code prc)) :data-code (:code prc)}
                  [:div (:name prc)][:small (str (:pack_name prc) " #" (:position prc))]]
                [:button.btn.button-primary])
              (if (some? nxc)
                [:a.btn.btn-primary.card-link.float-right
                  {:href (str "/lotrdb/card/digital/" (:code nxc)) :data-code (:code nxc)}
                  [:div (:name nxc)][:small (str (:pack_name nxc) " #" (:position nxc))]])]]
          [:div.row
            [:div.col-sm-6
              [:div.card 
                [:div.card-header
                  [:span.h3.card-title 
                    (if (:is_unique card)
                      [:i.lotr-type-unique.unique-icon])
                    (:name card)]
                  (lotrdb-card-icon card)]
                [:div.card-body
                  [:div.text-center [:b (:traits card)]]
                  [:div {:style "white-space: pre-wrap;"} (-> card :text lotrdb-markdown)]
                  [:div.mt-1	 [:em {:style "white-space: pre-wrap;"} (:flavor card)]]
                  [:div [:small.text-muted (str (:pack_name card) " #" (:position card))]]]]]
            [:div.col-sm-6
              [:img {:src (str "/img/lotrdb/digital/" (:code card) ".webp")}]]]]
      (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")])))

;;;;;;;;;;;;;
; DECK LIST ;
;;;;;;;;;;;;;

(defn- code-to-name [ c ]
  (-> c
      (clojure.string/replace "_" " ")
      clojure.string/capitalize))
      
(defn- lotrdb-export-string [ d deck-cards ]
  (clojure.string/join "\n"
    (reduce concat
      [(:name d)
       (str "\nTotal Cards: (" (->> deck-cards (filter #(not= (:type_code %) "hero")) (map :qty) (apply +)) ")")]
      (mapv (fn [tc]
        (let [tc-cards (filter #(= (:type_code %) tc) deck-cards)]
          (reduce conj 
            [(str "\n" (code-to-name tc) ": (" (->> tc-cards (map :qty) (apply +)) ")")]
            (mapv #(str (:qty %) "x " (:name %) " (" (:pack_name %) ")") tc-cards))
          )) ["hero" "ally" "attachment" "event" "player-side-quest"]))))

(defn- lotrdb-deck-card-list-by-type [type_code cards-in-deck]
  (let [cid-by-type (->> cards-in-deck (filter #(= (:type_code %) type_code)) (sort-by :normalname))
        cid-qty (->> cid-by-type (map :qty) (reduce +))]
    (if (< 0 cid-qty)
      [:div.decklist-section.mb-2
        [:div [:b (str (-> cid-by-type first :type_name) " (" cid-qty ")")]]
        (map (fn [r] 
              [:div (str (:qty r) "x ")
                [:a.card-tooltip {:href (str "/lotrdb/card/" (:code r)) :data-code (:code r) :class (:faction_code r)}
                  [:span 
                    (if (:is_unique r) [:span.lotr-type-unique.fa-xs.mr-1]) 
                    (:name r)]
                  [:span {:class (str "lotr-type-" (:sphere_code r) " fa-xs ml-1")}]]]
              ) cid-by-type)])))
            
(defn- lotrdb-deck-card [d card-data]
  (let [deck-cards (map (fn [[k v]] (assoc (first (filter #(= (:code %) k) card-data)) :qty v)) (json/read-str (:data d)))
        heroes     (filter #(= (:type_code %) "hero") deck-cards)]
    [:li.list-group-item.list-deck-card
      [:div.py-1 {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} 
        [:div.d-flex.justify-content-between
          [:div
            [:div.h4.mt-2 (:name d)] 
            [:div.text-muted.mb-2
              (str "Cards " (->> deck-cards (filter #(not= "hero" (:type_code %))) (map :qty) (reduce +)) "/50")]
            [:div 
              (if (:tags d)
                (map (fn [x] [:a.badge.badge-secondary.text-light.mr-1 x]) (re-seq #"\w+" (get d :tags))))]
            ]
          [:div.d-none.d-sm-flex
            (for [h heroes] 
              [:span 
                [:div.ml-1 {
                  :class (if (-> d :system (= "0")) "deckhero" "digideckhero")
                  :style (str "background-image: url(" (if (-> d :system (= "0")) (:cgdbimgurl h) (str "/img/lotrdb/digital/" (:code h) ".webp")) "); position: relative;") 
                  :title (:name h)}
                  [:span {:style "position: absolute; right: 2px; bottom: 2px;"}
                    [:img {:style "width: 35px" :src (str "/img/lotrdb/icons/sphere_" (:sphere_code h) ".png")}]]]]
                )]]]
      [:div.collapse.mb-2 {:id (str "deck_" (:uid d))}  
        [:div.mb-2.decklist
          (map #(lotrdb-deck-card-list-by-type % deck-cards) ["hero" "ally" "attachment" "event" "player-side-quest"])]
        [:div.mb-2
          [:div.small.col-sm-12.text-muted (str "Created on " (-> d :created tc/from-long))]
          [:div.small.col-sm-12.text-muted (str "Updated on " (-> d :updated tc/from-long))]]
        [:div
          [:button.btn.btn-sm.btn-danger.mr-1 {:data-toggle "modal" :data-target "#deletemodal" :data-name (:name d) :data-uid (:uid d)} [:i.fas.fa-times.mr-1] "Delete"]
          [:button.btn.btn-sm.btn-success.mr-1 {:data-toggle "modal" :data-target "#exportdeck" :data-export (lotrdb-export-string d deck-cards) :data-deckname (:name d)} [:i.fas.fa-file-export.mr-1] "Export"]
          [:a.btn.btn-sm.btn-primary.mr-1 {:href (str "/lotrdb/decks/" (if (-> d :system (= "0.1")) "digital/") "edit/" (:uid d))} [:i.fas.fa-edit.mr-1] "Edit"
          [:a.btn.btn-sm.btn-primary {:href (str "/lotrdb/decks/download/" (:uid d)) :download (-> d :name model/o8dname)} [:i.fas.fa-download.mr-1] "OCTGN File"]]]]
    ]))
       

(defn lotrdb-decks [req]
  (let [decks (reverse (sort-by :updated (concat
                (db/get-user-decks 0 (-> req model/get-authentications (get :uid 1002)))
                (db/get-user-decks 0.1 (-> req model/get-authentications (get :uid 1002)))
                )))
        fellowships (db/get-user-deckgroups 0 (-> req model/get-authentications (get :uid 1002)))
        card-data (model/get-cards)
        digi-card-data (model/get-lotracg-cards)]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:ul.nav.nav-tabs.nav-fill {:role "tablist"}
            [:li.h4.nav-item {:style "margin-bottom: 0px;"} [:a.nav-link.active {:href "#decktab" :data-toggle "tab" :role "tab"} "Decks"]]
            [:li.h4.nav-item {:style "margin-bottom: 0px;"} [:a.nav-link {:href "#fellowshiptab" :data-toggle "tab" :role "tab"} "Fellowships"]]]
          [:div.tab-content
            [:div#decktab.tab-pane.fade.show.active.my-3 {:role "tabpanel"}
              [:div.d-flex.justify-content-between
                [:div.h3 (str "Saved Decks (" (count decks) ")")]
                [:div 
                  [:button.btn.btn-warning.mr-2 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
                  [:a.btn.btn-primary.mr-1 {:href "/lotrdb/decks/new" :title "New Deck"} [:i.fas.fa-plus]]
                  [:a.btn.btn-primary.mr-2 {:href "/lotrdb/decks/digital/new" :title "New Digital Deck"} [:i.far.fa-plus-square]]]]
              [:div.d-flex
                [:div#decklists.w-100
                  [:ul.list-group
                    (for [d decks :let [cd (if (-> d :system (= "0")) card-data digi-card-data)]]
                      (lotrdb-deck-card d cd))]]]]
            [:div#fellowshiptab.tab-pane.fade.my-3 {:role "tabpanel"}
              [:div.d-flex.justify-content-between
                [:div.h3 (str "Saved Fellowships (" (count fellowships) ")")]
                [:div 
                  [:a.btn.btn-primary {:href "/lotrdb/decks/fellowship/new" :title "New Fellowship"} [:i.fas.fa-plus]]]]
              [:div.d-flex
                [:ul#fellowshiplist.list-group.w-100
                  (for [f fellowships]
                    [:li.list-group-item {:key (:uid f)}
                      [:div.d-flex.justify-content-between
                        [:h5 (:name f)]
                        [:span  
                          [:button.btn.btn-sm.btn-danger.mr-1 {:data-name (:name f) :data-target "#deletegroupmodal" :data-toggle "modal" :data-uid (:uid f)} [:i.fas.fa-times.mr-1] "Delete"]
                          [:a.btn.btn-sm.btn-primary {:href (str "/lotrdb/decks/fellowship/" (:uid f))} [:i.fas.fa-edit.mr-1] "Edit"]
                          ]]])
                  ]]]]]
        (deletemodal)
        (deletegroupmodal)
        (importallmodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/lotrdb/lotrdb_decklist.js?v=1.0")
        (h/include-css "/css/lotrdb-icomoon-style.css?v=1")])))

; Deckbuilder - single deck ;
; Re-write using datatables...? Performance? ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

; FELLOWSHIP Builder ;
;;;;;;;;;;;;;;;;;;;;;;

(def player-cards 
  (filter #(and (contains? #{"hero" "ally" "attachment" "event"} (:type_code %))
               (nil? (:encounter_name %))
               (not= "Starter" (:pack_code %)))
          (model/get-cards)))
(def packs (model/get-packs))
(def cycles (model/get-cycles))

(defn- filter-buttons []
  [:div.d-flex.justify-content-between.mb-2
    [:div#typefilter.btn-group.btn-group-sm.btn-group-toggle {:data-toggle "buttons"}
      (for [type player_type_icons]
        [:label.btn.btn-outline-secondary {:key (gensym (:code type)) :title (:name type)}
          [:input {:type "checkbox" :name (:code type)} (:symbol type)]])]
    [:div#spherefilter.btn-group.btn-group-sm.btn-group-toggle {:data-toggle "buttons"}
      (for [sphere sphere_icons]
        [:label.btn.btn-outline-secondary {:key (gensym (:code sphere)) :title (:name sphere)}
          [:input {:type "checkbox" :name (:code sphere)}
            [:img.icon-xs {:src (:img sphere)}]]])]])

(defn- buttons [ crd ]
  (let [maxindeck (if (= (:type_code crd) "hero") 2 4)]
    [:div.btn-group.btn-group-xs.btn-group-toggle {:key (gensym)}
      (for [x (range maxindeck)]
        [:button.btn.btn-outline-secondary {:class (if (zero? x) "active") :value x} x])]))
        
(defn- get-dupe-name [ crd player-cards ]
  (let [dupes (filter #(= (:name %) (:name crd)) player-cards)
        normalname (model/normalise (:name crd))
        packname (str " (" (:pack_code crd) ")")]
    (hash-map :dupename    (if (= 1 (count dupes))
                               (:name crd) 
                               (str (:name crd) packname))
              :dupefilter (if (= 1 (count dupes))
                               normalname
                               (str normalname packname))
              :dupesort (str normalname (:code crd)))))
        
(defn- table []
  [:table#dt.table.table-sm.table-hover.w-100
    [:thead
      [:tr
        [:th "#1"]
        [:th "Name"]
        [:th.text-center {:title "Sphere"} "Sp."]
        [:th.text-center {:title "Type"} "Ty."]
        [:th.text-center.d-none.d-sm-table-cell {:title "Cost/Threat"} "C."]
        [:th.text-center.d-none.d-sm-table-cell {:title "Attack"} "A."]
        [:th.text-center.d-none.d-sm-table-cell {:title "Defense"} "D."]
        [:th.text-center.d-none.d-sm-table-cell {:title "Willpower"} "W."]
        [:th.text-center.d-none.d-sm-table-cell {:title "Health"} "H."]
        [:th "#2"]
        [:th.d-none "pack_code"]
        [:th.d-none "traits"]
      ]]
    [:tbody
      (for [crd player-cards
        :let [cost (or (:cost crd) (:threat crd))
              btns (buttons crd)
              {:keys [dupename dupefilter dupesort]} (get-dupe-name crd player-cards)]]
        [:tr {:key (:code crd)}
          [:td {:data-code (:code crd) :data-deck 0}
            btns]
          [:td {
            :data-sort dupesort
            :data-filter dupefilter}
            [:a {:href "#" :data-target "#cardmodal" :data-toggle "modal" :data-code (:code crd)} dupename]]
          [:td.text-center {
            :data-sort (:sphere_code crd) 
            :data-filter (:sphere_code crd) 
            :title (:sphere_name crd)} 
            [:img.icon-sm {:src (str "/img/lotrdb/icons/sphere_" (:sphere_code crd) ".png")}]]
          [:td.text-center.text-secondary {
            :data-sort (:type_code crd) 
            :data-filter (:type_code crd)
            :title (:type_name crd)}
            (get type-icon (:type_code crd) (:type_name crd))]
          [:td.text-center.d-none.d-sm-table-cell {
            :data-sort (or cost -1)
            :data-filter (or cost -1)}
            cost]
          [:td.text-center.d-none.d-sm-table-cell {
            :data-sort (or (:attack crd) -1)
            :data-filter (or (:attack crd) -1)}
            (:attack crd)]
          [:td.text-center.d-none.d-sm-table-cell {
            :data-sort (or (:defense crd) -1)
            :data-filter (or (:defense crd) -1)}
            (:defense crd)]
          [:td.text-center.d-none.d-sm-table-cell {
            :data-sort (or (:willpower crd) -1)
            :data-filter (or (:willpower crd) -1)}
            (:willpower crd)]
          [:td.text-center.d-none.d-sm-table-cell {
            :data-sort (or (:health crd) -1)
            :data-filter (or (:health crd) -1)}
            (:health crd)]
          [:td {:data-code (:code crd) :data-deck 1}
            btns]
          [:td.d-none {:data-sort (:code crd) :data-filter (:pack_code crd)} (:pack_code crd)]
          [:td.d-none {:data-filter (model/normalise (or (:traits crd) ""))} (model/normalise (or (:traits crd) ""))]
        ])
    ]])
    
(defn- packsincycle [cycle-packs]
  [:div.my-2
    (for [pack cycle-packs]
      [:div.d-flex.justify-content-between {:key (gensym "pack")}
        (if (= 1 (count cycle-packs)) [:b (:name pack)] [:span  (:name pack)])
        [:input.my-auto {:type "checkbox" :data-type "pack" :data-code (:code pack)}]])])
    
(defn- packlist []
  [:div#packlist.list-group
    [:li.list-group-item
      [:div.d-flex.justify-content-between
        [:b "Core Sets"]
        [:div#coresets.btn-group.btn-group-sm.btn-group-toggle {:data-toggle "buttons"}
            [:label.btn.btn-outline-secondary.active [:input {:type "radio" :name "coresets" :value 1 :checked true}] 1]
            [:label.btn.btn-outline-secondary [:input {:type "radio" :name "coresets" :value 2}] 2]
            [:label.btn.btn-outline-secondary [:input {:type "radio" :name "coresets" :value 3}] 3]]]]
    
    (for [cycle (sort-by :cycle_position (rest cycles))
          :let [cycle-packs (->> packs (filter #(= (:cycle_position %) (:cycle_position cycle))) (sort-by :position))]]
      (if (= 1 (count cycle-packs))
          [:li.list-group-item {:key (gensym "cyc")}
            (packsincycle cycle-packs)]
          [:li.list-group-item {:key (gensym "cyc")}
            [:div.d-flex.justify-content-between
              [:b (:name cycle)]
              [:input.my-auto {:type "checkbox" :data-type "cycle" :data-code (:cycle_position cycle)}]]
            (packsincycle cycle-packs)]))])
            
(defn fellowship [ req ]
  (let [fdata (model/get-fellowship-data req)]
    (h/html5 
      (into 
        lotrdb-pretty-head [
          ;DataTables
          [:link {
            :rel "stylesheet" 
            :type "text/css" 
            :href "https://cdn.datatables.net/1.10.20/css/dataTables.bootstrap4.min.css"}]
          [:script {
            :type "text/javascript" 
            :src "https://cdn.datatables.net/1.10.20/js/jquery.dataTables.min.js"}]
          [:script {
            :type "text/javascript" 
            :src "https://cdn.datatables.net/1.10.20/js/dataTables.bootstrap4.min.js"}]])
      [:body
        (singletoast)
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.mb-2
            [:div.col-12
              [:form#saveform.d-flex.mb-2
                [:h5.my-auto.mr-2 "Fellowship"]
                [:input#fellowshipname.form-control.mr-2 {:type "text" :name "name" :value (:name fdata) :required true}]  
                [:input#fellowshipid {:hidden true :value (:uid fdata) :name "data"}]
                [:button#savefellowship.btn.btn-warning.mr-2 {:role "submit" :title "Save Fellowship" :disabled true} [:i.fas.fa-feather]]
                [:a.btn.btn-outline-secondary {:formnovalidate true :title "Cancel Edits" :href "/lotrdb/decks"} "Close"]]
              [:div.row.mb-2 {:style "min-height: 50px;"}
                [:div.col-sm-6 
                  [:b.my-auto.mr-2 "Deck #1"]
                  [:form#formdeck0.d-flex
                    [:input#deckname0.form-control.mr-2 {:type "text" :name "name" :value (-> fdata :d0 :name) :required true}]  
                    [:input#deckdata0 {:hidden true :value (-> fdata :d0 :data) :name "data"}]
                    [:input#deckid0 {:hidden true :value (-> fdata :d0 :uid) :name "id"}]
                    [:div.btn-group
                      [:button.btn.btn-secondary {:formnovalidate true :type "button" :title "Import Deck" :data-deckno 0 :data-toggle "modal" :data-target "#loadmodal"} [:i.fas.fa-file-import]]
                      [:button#savedeck0.btn.btn-warning {:role "submit" :title "Save Deck" :disabled true} [:i.fas.fa-feather]]]]
                  [:div#decklist0.mt-2]]
                [:div#deck2.col-sm-6
                  [:b.my-auto.mr-2 "Deck #2"]
                  [:form#formdeck1.d-flex
                    [:input#deckname1.form-control.mr-2 {:type "text" :name "name" :value (-> fdata :d1 :name)  :required true}]  
                    [:input#deckdata1 {:hidden true :value (-> fdata :d1 :data)  :name "data"}]
                    [:input#deckid1 {:hidden true :value (-> fdata :d1 :uid) :name "id"}]
                    [:div.btn-group
                      [:button.btn.btn-secondary {:formnovalidate true :type "button" :title "Import Deck" :data-deckno 1 :data-toggle "modal" :data-target "#loadmodal"} [:i.fas.fa-file-import]]
                      [:button#savedeck1.btn.btn-warning {:role "submit" :title "Save Deck" :disabled true} [:i.fas.fa-feather]]]]
                  [:div#decklist1.mt-2]]]]]
          [:div.row
            [:div.col-12
              [:ul.nav.nav-tabs
                [:li.nav-item [:a.nav-link.active {:href "#" :data-target "#cardlist" :data-toggle "tab"} "Cards"]]
                [:li.nav-item [:a.nav-link {:href "#" :data-target "#packlist" :data-toggle "tab"} "Packs"]]]
              [:div.tab-content
                [:div#cardlist.tab-pane.my-2.show.active {:role "tabpanel"}
                  (filter-buttons)
                  [:div [:input#search.form-control {:type "text" :placeholder "search"}]]
                  (table)
                ]
                [:div#packlist.tab-pane.my-2 (packlist)]]]]]
      (modal)
      (loadmodal)]
    (h/include-css  "/css/lotrdb-icomoon-style.css")
    (h/include-js  "/js/externs/typeahead.js")
    (h/include-js  "/js/lotrdb/lotrdb_tools.js")
    (h/include-js  "/js/lotrdb/lotrdb_popover.js")
    (h/include-js  "/js/lotrdb/lotrdb_fellowship.js"))))


(defn lotrdb-folders [ req ]
  (let [card_types ["hero" "ally" "attachment" "event"]
        cycles (drop-last (model/get-cycles))
        packs  (model/get-packs)
        cards  (model/get-cards) ;(filter #(contains (set card_types) (:type_code %) (model/get-cards)))
        spheres [{:name "Leadership" :col "purple"}{:name "Lore" :col "green"}{:name "Spirit" :col "blue"}{:name "Tactics" :col "darkred"}{:name "Neutral" :col "slategrey"}{:name "Baggins" :col "goldenrod"}]]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row
            [:div.col-lg-7 
              [:div.row
                [:nav
                  [:ol#spheres.breadcrumb
                    (for [s spheres]
                      [:li.breadcrumb-item {:data-code (-> s :name clojure.string/lower-case)  :style (str "cursor: pointer; color: " (:col s) ";") } (:name s)])]]]
              [:div.row
                [:nav [:ol#types.breadcrumb
                  (for [t card_types]
                    [:li.breadcrumb-item {:data-code (clojure.string/lower-case t):style "cursor: pointer;"} (clojure.string/capitalize t)])]]]
              [:div.row.d-flex.justify-content-between.mb-2
                [:div#pagetype] 
                [:div#pageno]
                [:div#pager.btn-group.btn-group-sm
                  [:button.btn.btn-outline-secondary {:value -1} "<<"]
                  [:button.btn.btn-outline-secondary {:value 1} ">>"]]]
              [:div#page.row.mb-3
                [:span "Loading..."]]]
            [:div.col-lg-5
              [:div#packs.list-group
                [:div.h4.text-center "Packs Owned"]
                (for [c cycles :let [cycle_id (str "cyc_" (:cycle_position c))]]
                  (if (= 1 (:cycle_position c)) ; Core
                    [:div.list-group-item
                      [:div.d-flex
                        [:span.h5.my-auto (:name c)]
                        [:div#coresets.ml-auto.btn-group.btn-group-sm.btn-group-toggle {:data-toggle "buttons"}
                          [:label#core1.btn.btn-outline-secondary
                            [:input {:name "corecount" :value "1" :type "radio"}] "1"]
                          [:label#core2.btn.btn-outline-secondary
                            [:input {:name "corecount" :value "2" :type "radio"}] "2"]
                          [:label#core3.btn.btn-outline-secondary
                            [:input {:name "corecount" :value "3" :type "radio"}] "3"]
                          ]]]
                    (let [pcks (->> packs (filter #(= (:cycle_position %) (:cycle_position c))))]
                      (if (= 1 (count pcks))
                        [:div.list-group-item 
                          [:div.d-flex
                            [:span.h5 (:name c)]
                            [:span.ml-auto [:input.pack {:type "checkbox" :id (str "pck_" (-> pcks first :id)) :data-code (-> pcks first :code)}]]]]
                        [:div.list-group-item 
                          [:div.d-flex
                            [:span.h5 (:name c)]
                            [:span.ml-auto [:input.cycle {:type "checkbox" :id cycle_id}]]]
                          (for [p pcks]                          
                            [:div.d-flex
                              [:span (:name p)]
                              [:span.ml-auto [:input.pack {:type "checkbox" :id (str "pck_" (:id p)) :data-code (:code p)}]]])]))))
                [:div.h5.text-center.my-2 "Card Counts"]
                [:div#cardcounts.mb-3]]]]]
      [:div#cardmodal.modal.fade {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h4#cardname.modal-title]
              [:button.close {:type "button" :data-dismiss "modal"} [:span "&times;"]]]
            [:div.modal-body
              [:div.row
                [:div#carddata.col-8]
                [:div.col-4 [:img#cardimg.img-fluid]]]]
            [:div#cardfooter.modal-footer.bg-light.p-1]]]]]
    (h/include-js "/js/lotrdb/lotrdb_folders.js?v=1")
    (h/include-css "/css/lotrdb-icomoon-style.css?v=1")
    )))
    
;; SAVE QUESTS ;;
(defn lotrdb-quest-page [ req ]
  (let [scenarios (model/get-scenarios) dateformatter (tf/formatter "yyyy-MM-dd")]
    (h/html5
      lotrdb-pretty-head
      [:body {:style "color: maroon"}
        (lotrdb-navbar req)
        [:div.container.my-3
          [:form.mb-3 {:action "/lotrdb/questlog/save" :method "POST"}
            [:div.form-row
              [:input#id {:name "id" :hidden true :readonly true}]
              [:div.form-group.col-4
                [:label "Quest"]
                [:input#questid {:name "questid" :hidden true :readonly true :value 1}]
                [:select#scenario.form-control
                  (for [s scenarios]
                    [:option (:name s)])]]
              [:div.form-group.col-2
                [:div [:label.mr-1 "Difficulty"] [:i.fas.fa-mountain]]
                [:select#difficulty.form-control {:name "difficulty"}
                  [:option "Easy"][:option {:selected true} "Normal"][:option "Nightmare"]]]
              [:div.form-group.col-1
                [:label "# Players"]
                [:select#players.form-control {:name "players"} (for [n (range 1 5)] [:option (str n)])]]
              [:div.form-group.col-2
                [:label "Date"]
                [:input#date.form-control {:name "date"   :type "Date" :value (tf/unparse dateformatter (time/now))}]]]
            [:div.form-row
              [:div.col-4 [:label "Player Deck Name"]]
              [:div.col-1 [:label "Spheres"]]
              [:div.col-2 [:label "Final Threat"]]
              [:div.col-2 [:label "Threat of Dead Heroes"]]
              [:div.col-2 [:label "Damage on Heroes"]]
              [:div.col-1 [:label "Subtotal"]]]
            [:div#plyrstats
              [:datalist#decklists] ;(for [dl decklists] [:option {:value (:name dl)}])]
              (for [n (range 1 5) :let [p (str "p" n)]]
                [:div.form-row.mb-1 {:hidden (> n 1) :id (str p "stats")}
                  [:div.col-4
                    [:div.input-group
                      [:input.form-control {:name (str p "deckname") :type "text" :list "decklists" :id (str p "deckname")}]
                      [:div.input-group-append
                        [:button.btn.btn-secondary {:type "button" :data-plyr-id n :data-toggle "modal" :data-target "#modaldecklist"} [:i.fas.fa-plus]]]]
                    [:input {:name (str p "decklist") :hidden true :readonly true :id (str p "decklist")}]]
                  [:div..col-1
                    [:div.pt-2 {:name (str p "spheres") :id (str p "spheres")}]]
                  [:div.col-2
                    [:input.form-control {:name (str p "threat") :type "number" :value 30 :min 0 :max 50 :id (str p "threat")}]]
                  [:div.col-2
                    [:input.form-control {:name (str p "deadh") :type "number" :value 0 :min 0 :id (str p "deadh")}]]
                  [:div.col-2
                    [:input.form-control {:name (str p "dmgh") :type "number" :value 0 :min 0 :id (str p "dmgh")}]]
                  [:div.col-1
                    [:input {:name (str p "score") :id (str p "score") :hidden true :readonly true :value 30}]
                    [:h5.text-center {:id (str p "scoreshown")} "30"]]
                ])]
            [:div.form-row
              [:div.form-group.col-2.offset-7
                [:div [:label.mr-2 "VP"] [:i.far.fa-star]]
                [:input#vp.form-control {:name "vp" :value 0 :min 0 :type "number"}]]
              [:div.form-group.col-2
                [:span [:label.mr-2 "# Turns"] [:i.far.fa-clock]]
                [:input#turns.form-control {:name "turns" :value 1 :min 1 :type "number"}]]
              [:div.form-group.col-1
                [:h5.text-center "Score"]
                [:input#score {:name "score" :hidden true :readonly true :value 40}]
                [:h3#scoreshown.pt-2.text-center "40"]]]
            [:div.d-flex.justify-content-end
              [:button#resetquest.btn.btn-secondary.mr-2 {:type "button"} [:i.fas.fa-eraser.mr-1] "New/Reset"]
              [:button#savequest.btn.btn-warning [:i.fas.fa-feather.mr-1] "Save"]]]
          [:div.row
            [:div.col
              [:div#savedquests.list-group
                (for [q (->> (-> req model/get-authentications :uid) db/get-quests (sort-by :id >) (sort-by :date >))
                      :let [questdecks (db/get-quest-decks (:id q))]]
                  [:li.list-group-item 
                    [:div.d-flex
                      [:h4.mr-3 (->> scenarios (filter #(= (:id %) (:questid q))) first :name)]
                      [:span.mt-1 
                        [:i.fas.fa-mountain.mr-1] [:span.mr-2 (:difficulty q)]
                        [:i.far.fa-clock.mr-1] [:span.mr-2 (:turns q)]
                        [:i.far.fa-star.mr-1] [:span (:vp q)]]
                      [:h4.ml-auto [:h4 (:score q)]]]
                    [:div.d-flex 
                      (for [d questdecks] 
                        [:div.mr-3 
                          [:span.mr-2 
                            {:data-decklist (:decklist d) :data-toggle "modal" :data-target "#qlmodal" :style "cursor: pointer;"}
                            (:deckname d)]
                          [:span (str "(" (:score d) ")")]])]
                    [:div.d-flex.justify-content-between
                      [:div.text-muted.mt-auto
                        [:small.mr-2 (tf/unparse (tf/formatter "dd-MMM-yyyy") (tc/from-long (:date q)))]
                        [:small (str "#" (:id q))]]
                      [:div 
                        [:button.btn.btn-primary.btn-sm.mr-1.btn-edit {:data-quest (str q) :data-questdecks (str questdecks) } [:i.fas.fa-edit.mr-1] "Edit"]
                        [:button.btn.btn-danger.btn-sm.mr-2.btn-delete {:data-qid (:id q) :data-toggle "modal" :data-target "#qlmodal"} [:i.fas.fa-times.mr-1] "Delete"]]]])]]]]
        [:div#qlmodal.modal.fade {:tab-index -1 :role "dialog"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4.modal-title ""]
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"} [:span {:aria-hidden "true"} "\u00d7"]]]
              [:div.modal-body]
              [:div.modal-footer]]]]
        [:div#modaldecklist.modal.fade {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-lg {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4.modal-title.w-100 "Decklist"]
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                  [:span {:aria-hidden "true"} "\u00d7"]]]
              [:div.modal-body
                [:div.row.mb-2
                  [:div.col
                    [:label.mr-1 "Deck Name"]
                    [:input#mdeckname.form-control.mb-2 {:type "text"}]
                    [:label "Add cards"]
                    [:div.d-flex.mb-2
                      [:input#mcardname.typeahead.form-control {:type "text"}]
                      [:div#mcardqty.btn-group.btn-group-sm.ml-2
                        (for [n (range 1 4)] [:button.btn.btn-outline-secondary.mqtybtn {:value n} n])]]
                    ;[:textarea#mdecklist.form-control {:rows 10}]
                    [:div#mdecklistpretty {:style "min-height:200px;"}]
                    [:input#mparsedecklist {:hidden true :readonly true}]]]]
              [:div.modal-footer
                [:input#mpnum {:hidden true :readonly true :value 0}]
                [:button.btn.btn-secondary {:data-dismiss "modal"} "Close"]
                ;[:button.btn.btn-Danger "Update Saved Deck"]
                [:button#mdecksave.btn.btn-primary "Save"]]]]]

        (h/include-js "/js/externs/typeahead.js")
        (h/include-css "/css/lotrdb-icomoon-style.css?v=1")
        (h/include-js "/js/lotrdb/lotrdb_questlog.js?v=0.1")
      ])))
      
;; DIGITAL DECKBUILDER ;;
(defn- code-order [ code ]
  (.indexOf ["hero" "ally" "attachment" "event" "leadership" "lore" "spirit" "tactics"] code))
      
(defn lotrdb-digideckbuilder [ req ]
  (let [deckdata (model/get-deck-data req)]
		(h/html5
			lotrdb-pretty-head
			[:body
				(lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.my-1
            [:div.col-lg-6
              [:div.sticky-top.pt-2
                [:div.row.mb-2
                  [:div.col
                    [:form {:action "/decks/save" :method "post"}
                      [:div.d-flex
                        [:input#deckname.form-control {:name "name" :placeholder "Deckname" :required true :value (:name deckdata)}]
                        [:button.btn.btn-warning.ml-1 {:submit "true" :title "Save"} 
                          [:div.d-flex
                            [:i.fas.fa-feather.my-auto] 
                            [:span.ml-1.d-none.d-xl-inline "Save"]]]
                        [:a.btn.btn-light.ml-1 {:href "/lotrdb/decks" :title "Cancel"}
                          [:div.d-flex
                            [:i.fas.fa-times.my-auto]
                            [:span.ml-1.d-none.d-xl-inline "Cancel"]]]]
                      [:div
                        [:input#deckdata   {:name "data"   :hidden true :value (:data deckdata)}]
                        [:input#deckuid    {:name "id"     :hidden true :value (:uid deckdata)}]
                        [:input#decksystem {:name "system" :hidden true :value 0.1}]
                        [:input#decktags   {:name "tags"   :hidden true :value (:tags deckdata)}]
                        [:input#decknotes  {:name "notes"  :hidden true :value (:notes deckdata)}]]]]]
                [:div.row [:div.col [:div#decklist]]]]]
            [:div.col-lg-6
              [:div.row
                [:ul.nav.nav-tabs.nav-fill.w-100 {:role "tablist"}
                  [:li.nav-item [:a.nav-link.active {:href "#buildtab" :data-toggle "tab" :role "tab" } "Build"]]
                  [:li.nav-item [:a.nav-link {:href "#notestab" :data-toggle "tab" :role "tab"} "Notes"]]
                  [:li.nav-item [:a.nav-link {:href "#testtab" :data-toggle "tab" :role "tab"} "Test"]]
                  [:li.nav-item [:a.nav-link {:href "#collectiontab" :data-toggle "tab" :role "tab"} "Collection"]]]
                [:div.tab-content.w-100
                  [:div#buildtab.tab-pane.fade.show.active.mt-2 {:role "tabpanel"} 
                    [:div.d-flex.mb-2.justify-content-between
                      (btngroup player_type_icons "type_code")
                      (btngroup sphere_icons "sphere_code")]
                    [:input#filtertext.search-digi.form-control {:type "text"}]
                    ;[:div#info.row]
                    [:table#dt.table.table-sm.table-hover
                      [:thead
                        [:tr 
                          [:th.d-none "code"]
                          [:th.text-center "Qty."]
                          [:th {:data-field "normalname"} "Name"]
                          [:th.text-center {:data-field "type_code" :title "Type"} "T."]
                          [:th.text-center {:data-field "sphere_code" :title "Sphere"} "S."]
                          [:th.text-center {:data-field "cost" :title "Cost/Threat"} [:span.lotr-type-threat]]
                          [:th.text-center {:data-field "willpower" :title "Willpower"} [:span.lotr-type-willpower]]
                          [:th.text-center {:data-field "attack" :title "Attack"} [:span.lotr-type-attack]]
                          [:th.text-center {:data-field "defense" :title "Defense"} [:span.lotr-type-defense]]
                          [:th.text-center {:data-field "health" :title "Health"} [:span.lotr-type-health]]
                          [:th.d-none "pack"]
                          [:th.d-none "traits"]
                          [:th.d-none "text"]
                          [:th.d-none "rarity"]
                          ]]
                      [:tbody
                        (for [crd (sort-by :code (model/get-lotracg-cards))]
                          [:tr 
                            [:td.d-none (:code crd)]
                            [:td {:data-code (:code crd) :data-type "qty"}
                              [:div.btn-group.btn-group-xs.btn-group-toggle 
                                (for [n (range (-> crd :deck_limit inc))] 
                                  [:button {:class (str "btn btn-outline-secondary" (if (zero? n) " active")) :value n} n])]]
                            [:td 
                              [:a.card-link-digital {:href "#" :data-code (:code crd) :data-toggle "modal" :data-target "#cardmodal"} (model/normalise (:name crd))]]
                            [:td.text-center {:title (:type_code crd) :data-order (-> crd :type_code code-order) :data-search (:type-code crd)} [:span {:class (str "lotr-type-" (:type_code crd) " text-secondary")}]]
                            [:td.text-center {:title (:sphere_code crd) :data-order (-> crd :sphere_code code-order) :data-search (:sphere-code crd)} [:span {:class (str "lotr-type-" (:sphere_code crd))}]]
                            [:td.text-center (or (:threat crd) (:cost crd))]
                            [:td.text-center (:willpower crd)]
                            [:td.text-center (:attack crd)]
                            [:td.text-center (:defense crd)]
                            [:td.text-center (:health crd)]
                            [:td.d-none (:pack_code crd)]
                            [:td.d-none (if (-> crd :traits nil?) "" (model/normalise (:traits crd)))]
                            [:td.d-none (:text crd)]
                            [:td.d-none (:rarity crd)]
                          ])
                      ]]]
                  [:div#notestab.tab-pane.fade.p-2 {:role "tabpanel"}
                    [:div.row-fluid.mb-2
                      [:span.h5.mr-2 "Deck Tags (separate with spaces)"]
                      [:input#tags.form-control]]
                    [:div.row-fluid.mb-2
                      [:span.h5.mr-2 "Deck Notes"]
                      [:small.ml-2  
                        [:a {
                          :href "https://github.com/showdownjs/showdown/wiki/Showdown's-Markdown-syntax" 
                          :target "_blank"}
                          "Use showdown's markdown syntax"]]]
                    [:div.row-fluid.mb-2
                      [:textarea#notes.form-control {:rows 10}]]
                    [:div#notesmarkdown.p-3 {:style "background-color: lightgrey;"}]]
                  [:div#testtab.tab-pane.fade.mt-2 {:role "tabpanel"}
                    [:div.row
                      [:div.col
                        [:div [:h5 "Test Draw"]]
                        [:div.btn-group
                          [:button#draw1.btn.btn-outline-secondary {:value 1 :title "Draw 1"} "1"]
                          [:button#draw1.btn.btn-outline-secondary {:value 2 :title "Draw 2"} "2"]
                          [:button#draw1.btn.btn-outline-secondary {:value 6 :title "Draw 6"} "6"]
                          [:button#reset.btn.btn-outline-secondary {:title "Reset"} "Reset"]]
                        [:div#drawcards.my-2]]]]
                  [:div#collectiontab.tab-pane.fade.mt-2 {:role "tabpanel"}
                    [:div
                      [:div [:span#allpacks.mx-2 [:i.fas.fa-tick.mr-1] "all"] [:span "/"] [:span#nopacks.mx-2 [:i.fas.fa-times.mr-1] "none"]]
                      (for [p (->> (model/get-lotracg-cards) (sort-by :code) (mapv #(select-keys % [:pack_name :pack_code])) distinct)]
                        [:div 
                          [:input.mr-2 {:type "checkbox" :data-type "pack" :data-id (:pack_code p)}]
                          [:span (:pack_name p)]])]
                  ]]]]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body.text-center]
              [:div.modal-footer]]]]
    ;(h/include-css "//cdn.datatables.net/1.10.23/css/jquery.dataTables.min.css")
    (h/include-css "//cdn.datatables.net/1.10.23/css/dataTables.bootstrap4.min.css")
    (h/include-js  "//cdn.datatables.net/1.10.23/js/jquery.dataTables.min.js")
    (h/include-js  "//cdn.datatables.net/1.10.23/js/dataTables.bootstrap4.min.js")
	  (h/include-css "/css/lotrdb-icomoon-style.css?v=1.1")
	  (h/include-js "/js/externs/typeahead.js")
	  (h/include-js "/js/lotrdb/lotrdb_tools.js?v=1.0")
	  (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1.0")
    (h/include-js "/js/lotrdb/lotrdb_digideckbuilder.js?v=1.2")
    ])))
    
(defn lotrdb-solo-page [ req ]
  (h/html5 
    lotrdb-pretty-head
    [:body
      (lotrdb-navbar req)
      [:div#lotrsolo]
      (h/include-js "/js/compiled/lotrsolo.js")
      (h/include-css "/css/lotrdb-icomoon-style.css")
      (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1")
      ]))
