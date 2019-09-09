(in-ns 'danuraidb.pages)

(def lotrdb-pretty-head
  (into pretty-head (h/include-css "/css/lotrdb-style.css?v=1.0")))
  
(def player_type_icons [
  {:name "Hero" :code "hero" :symbol [:i {:class "fas fa-user"}]}
  {:name "Ally" :code "ally" :symbol [:i {:class "fas fa-user-friends"}]}
  {:name "Attachment" :code "attachment" :symbol [:i {:class "fas fa-user-plus"}]}
  {:name "Event" :code "event" :symbol [:i {:class "fas fa-bolt"}]}])
  
(def sphere_icons
  (map #(let [code (clojure.string/lower-case %)] 
          (hash-map :name % :code code :img  (str "/img/lotrdb/icons/sphere_" code ".png")))
    ["Leadership","Lore","Spirit","Tactics","Baggins","Fellowship"]))
  
              
(defn lotrdb-navbar [req]
  (navbar 
    "/img/lotrdb/icons/sphere_fellowship.png" 
    "LotR DB" 
    "lotrdb"
    ["decks" "packs" "scenarios" "search"]
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
            
(defn- lotrdb-deck-card [deck]
  [:a.list-group-item.list-group-item-action {:href (str "/lotrdb/decks/edit/" (:uid deck))} 
    [:div.d-flex.justify-content-between
      [:span (:name deck)]
      [:form {:action "/decks/delete" :method "post"}
        [:input#deletedeckuid {:name "deletedeckuid" :hidden true :data-lpignore "true" :value (:uid deck)}]
        [:button.btn.btn-danger.btn-sm {:type "submit" :title "Delete Deck"} "x"]]]])
        
(defn lotrdb-decks [ req ]
  (let [user-decks (db/get-user-decks 0 (-> req get-authentications :uid))]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-2
          [:div.row.my-1
            [:a.btn.btn-secondary.mr-1 {:href "/lotrdb/decks/new"} "New Deck"]]
          [:div.row.my-1
            [:div.col-md-6
              [:div.row.mb-2 (str "Saved decks (" (count user-decks) ")")]
              [:div.list-group
                (map #(lotrdb-deck-card %) user-decks)]]]]])))
                
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

(defn lotrdb-card-page [ id ]
  (let [card (->> (model/get-cards-with-cycle) (filter #(= (:code %) id)) first)]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar nil)
        [:div.container.my-3
          [:div.row
            [:div.col-sm-6
              [:div.card 
                [:div.card-header
                  [:span.h3.card-title (:name card)]
                  (lotrdb-card-icon card)]
                [:div.card-body
                  [:div.text-center [:b (:traits card)]]
                  [:div {:style "white-space: pre-wrap;"} (:text card)]
                  [:div.mt-1	 [:em {:style "white-space: pre-wrap;"} (:flavor card)]]
                  [:div [:small.text-muted (str (:pack_name card) " #" (:position card))]]]]]
            [:div.col-sm-6
              [:img {:src (or (:cgdbimgurl card) (model/get-card-image-url card))}]]]]])))
            

(defn lotrdb-search-page [ req ]
  (let [q (or (-> req :params :q) "")
       view (or (-> req :params :view) "")]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container-fluid.my-3
          [:div.row.mb-2
            [:form.form-inline {:action "/lotrdb/search" :method "GET"}
              [:div.col-sm-12
                [:input.form-control.mr-2 {:type "text" :name "q" :value q}]
                [:button.btn.btn-primary.mr-2 {:role "submit"} "Search"]
                [:select.form-control {:type "select" :name "view"}
                  [:option {:selected (= view "list") :value "list"} "View as list"]
                  [:option {:selected (= view "cards") :value "cards"} "View as cards"]]]]]
          [:div.row
            (if (= view "cards")
              (for [card (model/cardfilter (or q "") (model/get-cards-with-cycle) :lotrdb)]
                [:div.col-auto
                  [:img.img-fluid {:src (or (:cgdbimgurl card) (model/get-card-image-url card))}]])
              [:table#tblresults.table.table-sm.table-hover
                [:thead [:tr [:th.sortable "Code"][:th.sortable "Name"][:th.sortable "Type"][:th.sortable "Sphere"][:th.sortable "Set"][:th.sortable "qty"]]]
                [:tbody#bodyresults
                  (for [card (model/cardfilter (or q "") (model/get-cards-with-cycle) :lotrdb)]
                    [:tr
                      [:td (:code card)]
                      [:td [:a.card-link {:data-code (:code card) :href (str "/lotrdb/card/" (:code card))} (:name card)]]
                      [:td (:type_name card)]
                      [:td (:sphere_name card)]
                      [:td (str (:pack_name card) " #" (:position card))]
                      [:td (:quantity card)]])]])]]])))
                  
(defn deckbuilder [ req ]
  (let [deck (model/get-deck-data req)]
		(h/html5
			lotrdb-pretty-head
			[:body
				(lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.my-1
            [:div.col-sm-6
              [:div.row.my-3
                [:form#save_form.form.needs-validation {:method "post" :action "/deck/save" :role "form" :novalidate true}
                  [:div.form-row.align-items-center
                    [:div.col-auto
                      [:label.sr-only {:for "#deck-name"} "Fellowship Name"]
                      [:input#fellowshipname.form-control {:type "text" :name "fellowshipname" :placeholder "New Fellowship" :required true :value (:name deck) :data-lpignore "true"}]
                      [:div.invalid-feedback "You must name your fellowship"]]
                    [:div.col-auto
                      [:button.btn.btn-warning.mr-2 {:role "submit"} "Save"]
                      [:a.btn.btn-light.mr-2 {:href "/lotrd/decks"} "Cancel Edits"]]]
                  [:input#deck-id      {:type "text" :name "deck-id"      :value (:uid deck) :readonly true :hidden true}]
                  [:input#deck-content {:type "text" :name "deck-content" :value (:data deck)  :readonly true :hidden true}]
                  [:input#deck-tags    {:type "text" :name "deck-tags"    :value (:tags deck) :readonly true :hidden true}]
                  [:input#deck-notes   {:type "text" :name "deck-notes"  :value (:notes deck) :readonly true :hidden true}]]]
              [:div#decklist.row]
            ]
            [:div.col-sm-6
              [:div.row.mb-2.justify-content-between
                (btngroup player_type_icons "type_code")
                (btngroup sphere_icons "sphere_code")]
              [:div.row 
                [:div.col-md-12
                  [:div.row 
                    [:input#filtertext.form-control {:type "text"}]]]]
              [:div#info.row]
              [:div.row
                [:table.table.table-sm.table-hover
                  [:thead
                    [:tr 
                      [:th "#"]
                      [:th.sortable {:data-field "name"} "Name"]
                      [:th.sortable.text-center {:data-field "type_code"} "Type"]
                      [:th.sortable.text-center {:data-field "sphere_code"} "Sphere"]
                      [:th.sortable.text-center {:data-field "cost" :title "Cost/Threat"} "C."]
                      [:th.sortable.text-center {:data-field "attack" :title "Attack"} "A."]
                      [:th.sortable.text-center {:data-field "defense" :title "Defense"} "D."]
                      [:th.sortable.text-center {:data-field "willpower" :title "Willpower"} "W."]
                      ]]
                  [:tbody#cardtbl]]]
            ]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-sm {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]]]]
	  (h/include-js "/js/externs/typeahead.js")
    (h/include-js "/js/lotrdb_deckbuilder.js?v=1.1")])))