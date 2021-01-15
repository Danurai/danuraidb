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
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
  
(load "pages/common")
(load "pages/lotrdb") 

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
                            [:td.d-none (:traits crd)]
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
              [:div.modal-body]
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
    
(load "pages/aosc")    
(load "pages/whuw")
(load "pages/whconq")
(load "pages/admin")

(defn home [req]
  (h/html5
    pretty-head
    [:body;
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
; lotr card img urls
  (let [crds (model/get-cards)]
    (h/html5
      lotrdb-pretty-head
      [:style ".card {width: 200px;}"]
      [:body 
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.mb-2
            [:div.col
              [:div#types.btn-group.btn-group-toggle {:data-toggle "buttons"}
                (for [t (->> crds (map :type_code) distinct sort)]
                  [:label.btn.btn-outline-primary {:class (if (= t "quest") "active")}
                    [:input {:type "radio" :data-type_code t :name "typecode" }]
                    t])]]]
          [:div#cards.row.mb-2]
        ]
        [:div#cardmodal.modal.fade {:tabindex -1 :role "dialog"}
          [:div.modal-dialog.modal-lg {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4#cardname.modal-title]
                [:button.close {:type "button" :data-dismiss "modal"} [:span "&times;"]]]
              [:div#cardimg.modal-body]]]]
      (h/include-js "/js/lotrdb/cardurltest.js?v=1")])))