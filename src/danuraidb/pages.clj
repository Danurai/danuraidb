(ns danuraidb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [clj-time.core :as time]
    [clj-time.coerce :as tc]
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
        
                  
(load "pages/common")
(load "pages/lotrdb")


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
          
(defn lotrdb-decks [req]
  (let [decks (db/get-user-decks 0 (-> req model/get-authentications (get :uid 1002)))
        card-data (model/get-cards-with-cycle)]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.d-flex.justify-content-between
            [:a.h3 {:href "/lotrdb/decks/fellowship"} "Fellowships"]
            [:div.h3 (str "Saved Decks (" (count decks) ")")]
            [:div 
              [:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
              [:a.btn.btn-primary {:href "/lotrdb/decks/new" :title "New Deck"} [:i.fas.fa-plus]]]]
          [:div.d-flex
            [:div#decklists.w-100
              [:ul.list-group
                (map (fn [d] (lotrdb-deck-card d card-data)) decks)]]]]
        (deletemodal)
        (importallmodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/lotrdb/lotrdb_decklist.js?v=1.0")])))

   
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
	  (h/include-js "/js/lotrdb_tools.js?v=1.0")
	  (h/include-js "/js/lotrdb_popover.js?v=1.0")
    (h/include-js "/js/lotrdb_deckbuilder.js?v=1.1")])))
    
(defn fellowship [ req ]  
  (h/html5 
    lotrdb-pretty-head
    [:body
      (lotrdb-navbar req)
      [:div#fellowship]
      (h/include-js "/js/compiled/fellowship.js")
      (h/include-css "/css/lotrdb-icomoon-style.css")
      (h/include-js "/js/lotrdb_popover.js?v=1")
      ]))
    
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
          
(defn testpage [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        (toaster)
        [:div.row
          [:div.col-12
            [:div (str @model/alert)]]]]]))
            
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
                  
                  
                  