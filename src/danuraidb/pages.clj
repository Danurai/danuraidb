(ns danuraidb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [clj-time.core :as time]
    [clj-time.format :as tf]
    [clj-time.coerce :as tc]
    [clj-http.client :as client]
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
  
(load "pages/common")
(load "pages/lotrdb") 

;; LOTR DB build placeholders

; PACKS 
;;;;;;;;;;;;;;;;;;
(defn pack-info [ pack ]
  [:small.ml-auto.mr-3.mt-auto
    [:a.mr-2 {:href (str "http://ffgapp.com/qr/" (:sku pack)) :target "_"} (-> pack :sku str)]
    [:span.mr-2 (-> pack :available str)]
    [:span 
      [:a {:href (-> pack :rulesheet str) :target "_"} "Rulesheet"]]])

(defn lotrdb-packs-page [ req ]
  (let [cards  (model/get-cards)
        packs  (model/get-packs)
        cycles (sort-by :cycle_position (model/get-cycles))]
    (h/html5 
      lotrdb-pretty-head
      [:body  
        (lotrdb-navbar req)
        [:div.container.my-3
          [:ul.list-group
            (for [cycle cycles]
              (let [packs-in-cycle (->> packs (filter #(= (:cycle_position %) (:cycle_position cycle))))
                    cards-in-packs (->> packs-in-cycle (mapv :cards) (remove nil?) (apply +))
                    cards-in-cycle (->> cards (filter #(= (:cycle_position %) (:cycle_position cycle))) (map :quantity) (reduce +))]
                [:li.list-group-item.list-group-item-action 
                  [:div.mb-2.d-flex 
                    [:a {:href (str "/lotrdb/cycle/" (:cycle_position cycle))} (:name cycle)]
                    (if (= 1 (count packs-in-cycle)) 
                        (pack-info (first packs-in-cycle))
                        [:span.ml-auto.mr-3])
                    [:span
                      [:span.mr-1 {:class (if (not= cards-in-packs cards-in-cycle) "text-danger" "")}  (str cards-in-cycle " cards")]]]
                  (if (< 1 (count packs-in-cycle))
                    [:ul.list-group
                      (for [pack packs-in-cycle :let [numcards (->> cards (filter #(= (:pack_code %) (:code pack))) (map :quantity) (reduce +))]]
                        [:li.list-group-item
                          [:div.d-flex
                            [:a {:href (str "/lotrdb/pack/" (:code pack))} (:name pack)]
                            (pack-info pack)
                            [:span {:class (if (not= numcards (:cards pack)) "text-danger" "")} (str numcards " cards")]]])])]))]]])))

; SCENARIOS ;
;;;;;;;;;;;;;

(defn lotrdb-scenarios-page [ req ]
	(let [cycles (model/get-cycles)
        packs  (model/get-packs)
        scens  (model/get-scenarios)
        cards  (model/get-cards)]
		(h/html5
			lotrdb-pretty-head
			[:body  
				(lotrdb-navbar req)
        [:div.container.my-3
          [:div#accordionparent.accordion 
            (for [c cycles] 
              [:div.card ;.accordion-item
                [:div.card-header {:id (str "heading" (:cycle_position c))}
                    [:button.btn.btn-block.text-left {:type "button" :data-toggle "collapse" :data-target (str "#collapse" (:cycle_position c))} 
                      [:h5.mb-0 (:name c)]]]
                [:div {:id (str "collapse" (:cycle_position c)) :data-parent "#accordionparent" :class (if (= (:cycle_position c) 1) "collapse show" "collapse")}
                  [:div.card-body
                    (let [packs_in_cycle (->> packs (filter #(= (:cycle_position c) (:cycle_position %))) (map :name))
                          scens_in_cycle (->> scens (filter (fn [q] (#(some #{(:pack q)} packs_in_cycle)))) (sort-by :id))]
                      (for [s scens_in_cycle]
                        
                        (let [quests (->> cards (filter #(= (:type_code %) "quest")) (filter #(= (:encounter_name %) (:name s))))] 
                          [:li.list-group-item 
                          [:div.row ;.justify-content-between
                            [:span.h4 
                              [:span.mr-2 (:name s)]
                              [:span.mr-2 [:small "icon"]]
                              [:i.fas.fa-chart-pie.ml-2.fa-xs.text-secondary {
                                :style "cursor: pointer;" 
                                :data-target "#modaldata" :data-toggle "modal"
                                :data-quest-id (:id s)
                                }]]]
                          [:div.row
                            [:div.col-sm-6
                              [:h5 [:a {:href (str "/lotrdb/search/physical?q=t:quest+n:" (:name s))} "Quests"]]
                              (for [q quests]
                                [:div [:a.card-link {:href (str "/lotrdb/card/" (:code q)) :data-code (:code q)} (:name q)]])]
                            [:div.col-sm-6 
                              [:h5
                                [:a {:href (str "/lotrdb/search/physical?q=n:" (->> s :encounters (map :name) (clojure.string/join "|")))}
                                "Encounter Sets"]]
                              ; assumed Encounter set always includes encounter pack with a matching name
                              [:div [:a {:href (str "/lotrdb/search/physical?q=n:" (clojure.string/replace (:name s) " " "+"))} (:name s)]]
                              (for [e (sort-by :id (:encounters s))]
                                (if (not= (:name s) (:name e))
                                  [:div [:a {:href (str "/lotrdb/search/physical?q=n:" (clojure.string/replace (:name e) " " "+"))} (:name e)]]))]]])))]]])]

          [:div#modaldata.modal.fade {:tab-index -1 :role "dialog"}
            [:div.modal-dialog.modal-xl {:role "document"}
              [:div.modal-content
                [:div.modal-header
                  [:h4.modal-title.w-100 ""]
                  [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                    [:span {:aria-hidden "true"} "\u00d7"]]]
                [:div.modal-body
                  [:div.d-flex.mb-3
                    [:h5.my-auto "Difficulty:"
                      [:span#difftxt.mx-2 "Normal"]]
                    [:div.ml-2.btn-group.btn-group-sm 
                      [:button#diffdown.btn.btn-outline-secondary {:style "line-height: 1em;"} [:i.fas.fa-caret-left.fa-xs]]
                      [:button#diffup.btn.btn-outline-secondary {:style "line-height: 1em;"} [:i.fas.fa-caret-right.fa-xs]]]]
                  [:div.row
                    [:div.col-4
                      [:canvas#piechart {:width "200" :height "200"}]
                      ]
                    [:div.col-4
                      [:canvas#barchart {:width "200" :height "200"}]
                      ]
                    [:div.col-4
                      [:canvas#diffchart {:width "200" :height "200"}]
                      ]
                  ]]]]]
          (h/include-js "https://cdn.jsdelivr.net/npm/chart.js@2.9.3/dist/Chart.min.js")
          (h/include-js "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@0.7.0")
          (h/include-js "/js/lotrdb/lotrdb_scenarios.js?v=0.1")
                    
                    ]])))

(defn lotrdb-scenarios-pagex [ req ]
	(let [cards (model/get-cards)]
		(h/html5
			lotrdb-pretty-head
			[:body  
				(lotrdb-navbar req)
        [:div.container.my-3
          [:ul.list-group
            (for [s (model/get-scenarios)]

              (let [quests (->> cards (filter #(= (:type_code %) "quest")) (filter #(= (:encounter_name %) (:name s))))] 
                [:li.list-group-item 
                  [:div.row.justify-content-between
                    [:span.h4 (:name s)
                      [:i.fas.fa-chart-pie.ml-2.fa-xs.text-secondary {
                        :style "cursor: pointer;" 
                        :data-target "#modaldata" :data-toggle "modal"
                        :data-quest-id (:id s)
                        }]] ;quests}]]
                    [:span
                      [:span.mr-2.align-middle (-> quests first :pack_name)]
                      (lotrdb-card-icon (first quests))]]
                  [:div.row
                    [:div.col-sm-6
                      [:h5 [:a {:href (str "/lotrdb/search/physical?q=t:quest+n:" (:name s))} "Quests"]]
                      (for [q quests]
                        [:div [:a.card-link {:href (str "/lotrdb/card/" (:code q)) :data-code (:code q)} (:name q)]])]
                    [:div.col-sm-6 
                      [:h5
                        [:a {:href (str "/lotrdb/search/physical?q=n:" (->> s :encounters (map :name) (clojure.string/join "|")))}
                        "Encounter Sets"]]
                      ; assumed Encounter set always includes encounter pack with a matching name
                      [:div [:a {:href (str "/lotrdb/search?q=n:" (clojure.string/replace (:name s) " " "+"))} (:name s)]]
                      (for [e (sort-by :id (:encounters s))]
                        (if (not= (:name s) (:name e))
                          [:div [:a {:href (str "/lotrdb/search/physical?q=n:" (clojure.string/replace (:name e) " " "+"))} (:name e)]]))]]]
                          
                ))]]
        [:div#modaldata.modal.fade {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-xl {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4.modal-title.w-100 ""]
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                  [:span {:aria-hidden "true"} "\u00d7"]]]
              [:div.modal-body
                [:div.d-flex.mb-3
                  [:h5.my-auto "Difficulty:"
                    [:span#difftxt.mx-2 "Normal"]]
                  [:div.ml-2.btn-group.btn-group-sm 
                    [:button#diffdown.btn.btn-outline-secondary {:style "line-height: 1em;"} [:i.fas.fa-caret-left.fa-xs]]
                    [:button#diffup.btn.btn-outline-secondary {:style "line-height: 1em;"} [:i.fas.fa-caret-right.fa-xs]]]]
                [:div.row
                  [:div.col-4
                    [:canvas#piechart {:width "200" :height "200"}]
                    ]
                  [:div.col-4
                    [:canvas#barchart {:width "200" :height "200"}]
                    ]
                  [:div.col-4
                    [:canvas#diffchart {:width "200" :height "200"}]
                    ]
                ]]]]]
        (h/include-js "https://cdn.jsdelivr.net/npm/chart.js@2.9.3/dist/Chart.min.js")
        (h/include-js "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@0.7.0")
        (h/include-js "/js/lotrdb/lotrdb_scenarios.js?v=0.1")]))) 

; SEARCH PAGE ;
;;;;;;;;;;;;;;;

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
       results (sort-by sortfield sortfn (model/cardfilter q (model/get-cards) :lotrdb))]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:form {:action "/lotrdb/search/physical" :method "GET"}
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
                    [:img.img-fluid.card-link {:data-code (:code card) :src (:cgdbimgurl card)}]
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
                        [:td [:a.card-link {:data-code (:code card) :href (str "/lotrdb/card/" (:code card))} (:name card)]]
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
   
; DECKBUILDER ;
;;;;;;;;;;;;;;;

(defn lotrdb-deckbuilder [ req ]
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
                        [:input#decksystem {:name "system" :hidden true :value 0}]
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
                    [:div.input-group.flex-nowrap
                      [:input#filtertext.form-control {:type "text"}]
                      [:div.input-group-append.search-info [:div.input-group-text [:i.fas.fa-info-circle]]]]
                    ;[:div#info.row]
                    [:table.table.table-sm.table-hover
                      [:thead
                        [:tr 
                          [:th "Qty."]
                          [:th.sortable {:data-field "normalname"} "Name"]
                          [:th.sortable.text-center {:data-field "type_code" :title "Type"} "T."]
                          [:th.sortable.text-center {:data-field "sphere_code" :title "Sphere"} "S."]
                          [:th.sortable.text-center {:data-field "cost" :title "Cost/Threat"} [:span.lotr-type-threat]]
                          [:th.sortable.text-center {:data-field "willpower" :title "Willpower"} [:span.lotr-type-willpower]]
                          [:th.sortable.text-center {:data-field "attack" :title "Attack"} [:span.lotr-type-attack]]
                          [:th.sortable.text-center {:data-field "defense" :title "Defense"} [:span.lotr-type-defense]]
                          [:th.sortable.text-center {:data-field "health" :title "Health"} [:span.lotr-type-health]]
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
                      [:a#selectall.mr-3 {:href "#"} "All"]
                      [:a#selectnon.mr-3 {:href "#"} "None"]]
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
          [:div.modal-dialog {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]
              [:div.modal-footer]]]]
    (h/include-css "/css/lotrdb-icomoon-style.css?v=1.1")
    (h/include-js "/js/externs/typeahead.js")
    (h/include-js "/js/lotrdb/lotrdb_tools.js?v=1.0")
    (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1.0")
    (h/include-js "/js/lotrdb/lotrdb_deckbuilder.js?v=1.2")])))

; FOLDERS ;
;;;;;;;;;;;

(defn lotrdb-folders [ req ]
  (let [card_types ["hero" "ally" "attachment" "event"]
        cycles (drop-last (model/get-cycles))
        packs  (model/get-packs)
        cards  (model/get-cards) ;(filter #(contains (set card_types) (:type_code %) (model/get-cards)))
        spheres [{:name "Leadership" :col "purple"}{:name "Lore" :col "green"}{:name "Spirit" :col "blue"}{:name "Tactics" :col "darkred"}{:name "Other" :col "slategrey"}]] ;{:name "Baggins" :col "goldenrod"}]]
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
                [:div
                  [:a.selectpacks.mr-2.me-2 {:href "#" :data-select "all"} "All"]
                  [:a.selectpacks.mr-2.me-2 {:href "#" :data-select "none"} "None"]]
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

; SAVE QUESTS ;
;;;;;;;;;;;;;;;

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

(load "pages/aosc")
(load "pages/whuw")
(load "pages/whconq")
(load "pages/admin")

(defn home [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        [:div.h5 "Deckbuilders"]
        (map (fn [{:keys [code desc icon]}]
          [:div [:a {:href (str "/" code "/decks") } [:img.mr-2 {:src icon :style "width: 1em"}] desc]]) model/systems)]]))
            
(defn staging-page [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container-fluid.my-3
        (toaster)
        [:div.row.h4 
          [:div.col "Staging"]]
        [:div.row.mb-2
          [:div.col
            [:div.btn-group.mr-2
              [:button#importselected.btn.btn-primary {:title "Import Selected"} [:i.fas.fa-file-upload]]
              [:button#deleteselected.btn.btn-danger {:title "Delete Selected" :data-toggle "modal" :data-target "#deletemodal"} [:i.fas.fa-trash-alt]]]]]
        [:div.row
          [:div.col
            [:table.table.table-sm {:style "table-layout: fixed;"}
              [:thead [:tr
                [:th {:style "width: 5%"} [:input#selectall.checkbox-md {:type "checkbox" :title "Select None/All"}]]
                [:th {:style "width: 10%"} "Type"]
                [:th {:style "width: 5%"} "Sys."]
                [:th {:style "width: 20%"} "Name"]
                [:th {:style "width: 60%"} "Data"]]]
              [:tbody
                (for [data (db/get-staged-data)]
                  [:tr
                    [:td.align-middle.align-center [:input.checkbox-md {:type "checkbox" :data-d (json/write-str data)}]]
                    [:td (-> data :type clojure.string/capitalize)]
                    [:td [:img.icon-sm {:src (->> model/systems (filter #(= (:id %) (-> data :system read-string))) first :icon)}]]
                    [:td (:name data)]
                    [:td {:style "overflow: hidden; white-space: nowrap;"} (-> data :decklist str)]])]]]]]
      [:div#deletemodal.modal {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h5.modal-title "Confirm Delete"]
              [:button {:type "button" :class "close" :data-dismiss "modal"} 
                [:span "x"]]]
            [:div.modal-body
              [:div.mb-2 "Are you sure you want to delete selected items?"]
              [:div.progress [:div.progress-bar {:role "progressbar"}]]]
            [:div.modal-footer
              [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
              [:button#deletedata.btn.btn-danger "Delete"]]]]]
      (h/include-js "/js/staging.js?v=0.1")
    ]))
                  
(defn testpage [ req ]
  (let [gw (-> "private/whuw/whuw_data_r2.json" io/resource slurp (json/read-str :key-fn keyword)) ]
    (h/html5
      pretty-head
      [:body 
        (navbar req)
        [:div.container.my-3
          [:div "Warbands"]
          [:table.table
            [:thead [:tr [:th "name"] [:th "filename"] [:th "icons"]]]
            [:tbody
              (for [s (:sets gw)]
                [:tr 
                  [:td (:name s)]
                  [:td (-> s :icon :filename)]
                  [:td [:img.icon-sm {:src (str "/img/whuw/icons/" (-> s :icon :filename))}]]])
            ]]]])))