(in-ns 'danuraidb.pages)

(def lotrdb-pretty-head
  (into pretty-head (h/include-css "/css/lotrdb-style.css?v=1.0")))
  
(def player_type_icons [
  {:name "Hero" :code "hero" :symbol [:i {:class "fas fa-user"}]}
  {:name "Ally" :code "ally" :symbol [:i {:class "fas fa-user-friends"}]}
  {:name "Attachment" :code "attachment" :symbol [:i {:class "fas fa-user-plus"}]}
  {:name "Event" :code "event" :symbol [:i {:class "fas fa-bolt"}]}])

(def type-icon (apply merge (map #(hash-map (:code %) (:symbol %)) player_type_icons)))
  
(def sphere_icons
  (map #(let [code (clojure.string/lower-case %)] 
          (hash-map :name % :code code :img  (str "/img/lotrdb/icons/sphere_" code ".png")))
    ["Leadership","Lore","Spirit","Tactics","Neutral"]))  ;,"Baggins","Fellowship","Boon"
  
              
(defn lotrdb-navbar [req]
  (navbar 
    "/img/lotrdb/icons/sphere_fellowship.png" 
    "LotR DB" 
    "lotrdb"
    ["decks" "packs" "scenarios" "search" "folders" "solo"]
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
                
(defn lotrdb-packs-page [ req ]
	(let [cards (model/get-cards-with-cycle)]
		(h/html5 
			lotrdb-pretty-head
			[:body  
				(lotrdb-navbar req)
				[:div.container.my-3
					[:ul.list-group
						(for [cycle (sort-by :cycle_position (model/get-cycles))]
							(let [packs (->> (model/get-packs) (filter #(= (:cycle_position %) (:cycle_position cycle))))]
								[:li.list-group-item.list-group-item-action 
									[:div.mb-2.d-flex.justify-content-between 
										[:a {:href (str "/lotrdb/cycle/" (:cycle_position cycle))} (:name cycle)]
										[:span
											[:span.mr-1 
												(str (->> cards (filter #(= (:cycle_position %) (:cycle_position cycle))) (map :quantity) (reduce +))
												" cards")]]]
									(if (< 1 (count packs))
										[:ul.list-group
											(for [pack packs]
												[:li.list-group-item
													[:div.d-flex.justify-content-between
														[:a {:href (str "/lotrdb/pack/" (:code pack))} (:name pack)]
														[:span (str
															(->> cards (filter #(= (:pack_code %) (:code pack))) (map :quantity) (reduce +))
															" cards")]]])])]))]]])))

(defn- lotrdb-card-icon [ card ]
  [:img.icon-sm.float-right {
    :src (str "/img/lotrdb/icons/"
              (if (some? (:sphere_name card)) 
                  (str "sphere_" (:sphere_code card))
                  (str "pack_" (:pack_code card)))
              ".png")}])
                              
(defn lotrdb-scenarios-page [ req ]
	(let [cards (model/get-cards-with-cycle)]
		(h/html5
			lotrdb-pretty-head
			[:body  
				(lotrdb-navbar req)
				[:body
					[:div.container.my-3
						[:ul.list-group
							(for [s (model/get-scenarios)]
								(let [quests (->> cards (filter #(= (:type_code %) "quest")) (filter #(= (:encounter_name %) (:name s))))] 
									[:li.list-group-item 
										[:div.row.justify-content-between
											[:span.h4 (:name s)]
											[:span
												[:span.mr-2.align-middle (-> quests first :pack_name)]
												(lotrdb-card-icon (first quests))]]
										[:div.row
											[:div.col-sm-6
												[:h5 "Quests"]
												(for [q quests]
													[:div [:a.card-link {:href (str "/lotrdb/card/" (:code q)) :data-code (:code q)} (:name q)]])]
											[:div.col-sm-6 
												[:h5 "Encounter Sets"]
												; assumed Encounter set always includes encounter pack with a matching name
												[:div [:a {:href (str "/lotrdb/search?q=n:" (clojure.string/replace (:name s) " " "+"))} (:name s)]]
												(for [e (sort-by :id (:encounters s))]
													(if (not= (:name s) (:name e))
														[:div [:a {:href (str "/lotrdb/search?q=n:" (clojure.string/replace (:name e) " " "+"))} (:name e)]]))]]]))]]]])))

(defn lotrdb-search-page [ req ]
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
       results (sort-by sortfield sortfn (model/cardfilter q (model/get-cards-with-cycle) :lotrdb))]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container-fluid.my-3
          [:form {:action "/lotrdb/search" :method "GET"}
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
              (for [card results]
                [:div.col-auto
                  [:img.img-fluid.card-link {:data-code (:code card) :src (or (:cgdbimgurl card) (model/get-card-image-url card))}]])
              [:div.col
                [:table#tblresults.table.table-sm.table-hover
                  [:thead [:tr  
                    [:th.sortable.d-none.d-md-table-cell "Code"]
                    [:th.sortable "Name"]
                    [:th.sortable.text-center {:title "Threat/Cost"} "T/C"]
                    [:th.sortable.text-center {:title "Willpower"} "W."]
                    [:th.sortable.text-center {:title "Attack"} "A."]
                    [:th.sortable.text-center {:title "Defense"} "D."]
                    [:th.sortable "Type"]
                    [:th.sortable "Sphere"]
                    [:th.sortable.d-none.d-md-table-cell "Set"]
                    [:th.text-center "qty"]]]
                  [:tbody#bodyresults
                    (for [card results]
                      [:tr
                        [:td.d-none.d-md-table-cell (:code card)]
                        [:td [:a.card-link {:data-code (:code card) :href (str "/lotrdb/card/" (:code card))} (:name card)]]
                        [:td.text-center (or (:threat card) (:cost card))]
                        [:td.text-center (:willpower card)]
                        [:td.text-center (:attack card)]
                        [:td.text-center (:defense card)]
                        [:td (:type_name card)]
                        [:td (:sphere_name card)]
                        [:td.d-none.d-md-table-cell (str (:pack_name card) " #" (:position card))]
                        [:td.text-center (:quantity card)]])]]])]]
      (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")
      ])))
      
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
             
(defn- lotrdb-markdown [ txt ]
  (->> txt
      (re-seq #"\[\w+\]|\w+|." )
      (map #(model/convert "lotr-type-" %))
      model/makespan))
      
(defn lotrdb-card-page [ id ]
  (let [cards  (model/get-cards-with-cycle)
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
                  [:div.mt-1	 [:em {:style "white-space: pre-wrap;"} (:flavor card)]]
                  [:div [:small.text-muted (str (:pack_name card) " #" (:position card))]]]]]
            [:div.col-sm-6
              [:img {:src (or (:cgdbimgurl card) (model/get-card-image-url card))}]]]]
      (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")])))                            
 
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
  (let [cid-by-type (filter #(= (:type_code %) type_code) cards-in-deck)]
    [:div.decklist-section
      [:div [:b (str (-> cid-by-type first :type_name) " (" (->> cid-by-type (map :qty) (reduce +)) ")")]]
      (map (fn [r] 
            [:div (str (:qty r) "x ")
              [:a.card-tooltip {:href (str "/lotrdb/card/" (:code r)) :data-code (:code r) :class (:faction_code r)} (if (:unique r) [:i.fas.fa-skull.fa-xs.mr-1]) (:name r)]
            ]) cid-by-type)]))
            
(defn- lotrdb-deck-card [d card-data]
  (let [deck-cards (map (fn [[k v]] (assoc (first (filter #(= (:code %) k) card-data)) :qty v)) (json/read-str (:data d)))
        heroes     (filter #(= (:type_code %) "hero") deck-cards)]
    [:li.list-group-item.list-deck-card
      [:div.py-1 {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} 
        [:div.d-flex.justify-content-between
          [:div
            [:div.h4.mt-2 (:name d)]
            [:div (map (fn [x] [:a.badge.badge-secondary.text-light.mr-1 x]) (re-seq #"\w+" (:tags d)))]]
          [:div.d-none.d-sm-flex
            (for [h heroes] 
              [:span 
                [:div.deckhero.ml-1 {:style (str "background-image: url(" (:cgdbimgurl h) "); position: relative;") :title (:name h)}
                  [:span {:style "position: absolute; right: 2px; bottom: 2px;"}
                    [:img {:style "width: 35px" :src (str "/img/lotrdb/icons/sphere_" (:sphere_code h) ".png")}]]]]
                )]]]
      [:div.collapse.mb-2 {:id (str "deck_" (:uid d))}   
        [:div.text-muted.mb-2
          (str "Cards " (->> deck-cards (filter #(not= "hero" (:type_code %))) (map :qty) (reduce +)) "/50")]
        [:div.mb-2.decklist
          (map #(lotrdb-deck-card-list-by-type % deck-cards) ["hero" "ally" "attachment" "event" "player-side-quest"])]
        [:div.mb-2
          [:div.small.col-sm-12.text-muted (str "Created on " (-> d :created tc/from-long))]
          [:div.small.col-sm-12.text-muted (str "Updated on " (-> d :updated tc/from-long))]]
        [:div
          [:button.btn.btn-sm.btn-danger.mr-1 {:data-toggle "modal" :data-target "#deletemodal" :data-name (:name d) :data-uid (:uid d)} [:i.fas.fa-times.mr-1] "Delete"]
          [:button.btn.btn-sm.btn-success.mr-1 {:data-toggle "modal" :data-target "#exportdeck" :data-export (lotrdb-export-string d deck-cards) :data-deckname (:name d)} [:i.fas.fa-file-export.mr-1] "Export"]
          [:a.btn.btn-sm.btn-primary {:href (str "/lotrdb/decks/edit/" (:uid d))} [:i.fas.fa-edit.mr-1] "Edit"]]]
    ]))
          

; Deckbuilder - single deck ;
; Re-write using datatables ;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
(defn lotrdb-deckbuilder [ req ]
  (let [deckdata (model/get-deck-data req)]
		(h/html5
			lotrdb-pretty-head
			[:body
				(lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.my-1
            [:div.col-sm-6
              [:div.sticky-top.pt-2
                [:form.form.w-100 {:action "/decks/save" :method "post"}
                  [:div.row.py-1.mr-1
                    [:div.col-7 
                      [:input#deckname.form-control {:name "name" :placeholder "Deckname" :required true :value (:name deckdata)}]
                      [:div.invalid-feedback "You must name your fellowship"]]
                    [:div.col-5
                      [:input#deckdata   {:name "data"   :hidden true :value (:data deckdata)}]
                      [:input#deckuid    {:name "id"     :hidden true :value (:uid deckdata)}]
                      [:input#decksystem {:name "system" :hidden true :value 0}]
                      [:input#decktags   {:name "tags"   :hidden true :value (:tags deckdata)}]
                      [:input#decknotes  {:name "notes"  :hidden true :value (:notes deckdata)}]
                      [:button.btn.btn-warning.mr-2 {:submit "true"} 
                        [:i.fas.fa-bookmark.mr-1] 
                        [:span.ml-1.d-none.d-sm-inline-block"Save"]]
                      [:a.btn.btn-light.mr-2 {:href "/lotrdb/decks"} "Cancel"]]]]
                [:div#decklist]]]
            [:div.col-sm-6
              [:div.row
                [:ul.nav.nav-tabs.nav-fill.w-100 {:role "tablist"}
                  [:li.nav-item [:a.nav-link.active {:href "#buildtab" :data-toggle "tab" :role "tab" } "Build"]]
                  [:li.nav-item [:a.nav-link {:href "#notestab" :data-toggle "tab" :role "tab"} "Notes"]]
                  [:li.nav-item [:a.nav-link {:href "#statstab" :data-toggle "tab" :role "tab"} "Stats"]]
                  [:li.nav-item [:a.nav-link {:href "#collectiontab" :data-toggle "tab" :role "tab"} "Collection"]]]
                [:div.tab-content.w-100
                  [:div#buildtab.tab-pane.fade.show.active.mt-2 {:role "tabpanel"} 
                    [:div.d-flex.mb-2.justify-content-between
                      (btngroup player_type_icons "type_code")
                      (btngroup sphere_icons "sphere_code")]
                    [:input#filtertext.form-control {:type "text"}]
                    ;[:div#info.row]
                    [:table.table.table-sm.table-hover
                      [:thead
                        [:tr 
                          [:th "Qty."]
                          [:th.sortable {:data-field "name"} "Name"]
                          [:th.sortable.text-center {:data-field "type_code"} "Type"]
                          [:th.sortable.text-center {:data-field "sphere_code"} "Sphere"]
                          [:th.sortable.text-center {:data-field "cost" :title "Cost/Threat"} "C."]
                          [:th.sortable.text-center {:data-field "attack" :title "Attack"} "A."]
                          [:th.sortable.text-center {:data-field "defense" :title "Defense"} "D."]
                          [:th.sortable.text-center {:data-field "willpower" :title "Willpower"} "W."]
                          ]]
                      [:tbody#cardtbl]]]
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
                  [:div#statstab.tab-pane.fade.mt-2 {:role "tabpanel"}
                  ]
                  [:div#collectiontab.tab-pane.fade.mt-2 {:role "tabpanel"}
                    (let [cycles (model/get-cycles) packs (model/get-packs)]
                      [:ul.list-group
                        (for [c (sort-by :cycle_position cycles)]
                          (if (= (:name c) "Core")
                            ^{:key (gensym)}[:li.list-group-item
                              [:div.d-flex
                                [:span.font-weight-bold "Core Sets"]
                                [:div#coresets.btn-group.btn-group-sm.btn-group-toggle.ml-auto {:data-toggle "buttons"}
                                  [:label.btn.btn-outline-secondary
                                    [:input#1core {:type "radio" :name "coresets" :value 1} "1"]]
                                  [:label.btn.btn-outline-secondary
                                    [:input#2core {:type "radio" :name "coresets" :value 2} "2"]]
                                  [:label.btn.btn-outline-secondary
                                    [:input#3core {:type "radio" :name "coresets" :value 3} "3"]]]]]
                            (let [cpacks (sort-by :position (filter #(= (:cycle_position %) (:cycle_position c)) packs))]
                              (if (> (count cpacks) 1)
                                ^{:key (gensym)}[:li.list-group-item
                                  [:div
                                    [:input.mr-2 {:type "checkbox" :data-type "cycle" :data-id (:id c)}]
                                    [:span.font-weight-bold (:name c)]]
                                    (for [cp cpacks]
                                      ^{:key (gensym)}[:div
                                        [:input.mr-2 {:type "checkbox" :data-type "pack" :data-id (:code cp)}]
                                        [:span (:name cp)]])]
                                ^{:key (gensym)}[:li.list-group-item
                                  [:div 
                                    [:input.mr-2 {:type "checkbox" :data-type "pack" :data-id (-> cpacks first :code)}]
                                    [:span.font-weight-bold (:name c)]]]))
                        ))])]]]]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-sm {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]]]]
	  (h/include-css "/css/lotrdb-icomoon-style.css?v=1.0")
	  (h/include-js "/js/externs/typeahead.js")
	  (h/include-js "/js/lotrdb/lotrdb_tools.js?v=1.0")
	  (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1.0")
    (h/include-js "/js/lotrdb/lotrdb_deckbuilder.js?v=1.1")])))
    
    
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

(defn lotrdb-decks [req]
  (let [decks (db/get-user-decks 0 (-> req model/get-authentications (get :uid 1002)))
        fellowships (db/get-user-deckgroups 0 (-> req model/get-authentications (get :uid 1002)))
        card-data (model/get-cards-with-cycle)]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:ul.nav.nav-tabs.nav-fill {:role "tablist"}
            [:li.h4.nav-item [:a.nav-link.active {:href "#decktab" :data-toggle "tab" :role "tab"} "Decks"]]
            [:li.h4.nav-item [:a.nav-link {:href "#fellowshiptab" :data-toggle "tab" :role "tab"} "Fellowships"]]]
          [:div.tab-content
            [:div#decktab.tab-pane.fade.show.active.my-3 {:role "tabpanel"}
              [:div.d-flex.justify-content-between
                [:div.h3 (str "Saved Decks (" (count decks) ")")]
                [:div 
                  [:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
                  [:a.btn.btn-primary {:href "/lotrdb/decks/new" :title "New Deck"} [:i.fas.fa-plus]]]]
              [:div.d-flex
                [:div#decklists.w-100
                  [:ul.list-group
                    (map (fn [d] (lotrdb-deck-card d card-data)) decks)]]]]
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
        (h/include-js "/js/lotrdb/lotrdb_decklist.js?v=1.0")])))    
    
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
