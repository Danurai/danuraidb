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
    ["Leadership","Lore","Spirit","Tactics","Neutral"]))  ;,"Baggins","Fellowship"
  
              
(defn lotrdb-navbar [req]
  (navbar 
    "/img/lotrdb/icons/sphere_fellowship.png" 
    "LotR DB" 
    "lotrdb"
    ["decks" "packs" "scenarios" "search" "solo"]
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
      (h/include-js "/js/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")
      ])))
      
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
             
(defn lotrdb-markdown [ txt ]
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
      (h/include-js "/js/lotrdb_popover.js?v=1")
      (h/include-css "/css/lotrdb-icomoon-style.css?v=1")])))                            
                         

(defn lotrdb-solo-page [ req ]
  (h/html5 
    lotrdb-pretty-head
    [:body
      (lotrdb-navbar req)
      [:div#lotrsolo]
      (h/include-js "/js/compiled/lotrsolo.js")
      (h/include-css "/css/lotrdb-icomoon-style.css")
      (h/include-js "/js/lotrdb_popover.js?v=1")
      ]))