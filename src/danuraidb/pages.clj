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

(defn lotrdb-scenarios-page [ req ]
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
                          [:div [:a {:href (str "/lotrdb/search/physical?q=n:" (clojure.string/replace (:name e) " " "+"))} (:name e)]]))]]]))]]
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