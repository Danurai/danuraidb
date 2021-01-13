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
     
(defn- dropdown-link [ req & args ]
  (let [subroot (str "/" (clojure.string/join "/" args))]
    [:a.dropdown-item {:href subroot}  (-> args last clojure.string/capitalize)]))
        
(defn navbar 
  ([ iconsrc title root links req & {:keys [style]}]
    [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark {:style style}
      [:div.container-fluid
      ;; Home Brand with Icon
        [:div.nav-item.dropdown
          [:a..navbar-brand.h1.mb-0.dropdown-toggle {:href "#" :role "button" :data-toggle "dropdown"} 
            [:img.mr-1 {:src iconsrc :style "width: 1em;"}] title]
          [:div.dropdown-menu
            (map (fn [s]
              [:a.dropdown-item {:href (str "/" (:code s) "/decks")} [:img.mr-1 {:src (:icon s) :style "width: 1em;"}] (:desc s)]) model/systems)
            [:a.dropdown-item {:href "/staging"} [:i.fas.fa-file-upload.text-primary.mr-2] "Staging"]]]
      ;; Collapse Button for smaller viewports
        [:button.navbar-toggler {:type "button" :data-toggle "collapse" :data-target "#navbarSupportedContent" 
                              :aria-controls "navbarSupportedContent" :aria-label "Toggle Navigation" :aria-expanded "false"}
          [:span.navbar-toggler-icon]]
      ;; Collapsable Content
        [:div#navbarSupportedContent.collapse.navbar-collapse
      ;; List of Links
          [:ul.navbar-nav.mr-auto
            (for [link links :let [dd (clojure.string/split link #"\|")]]
              (if (-> dd count (> 1))
                  [:li.nav-item.dropdown
                    [:a.nav-link.dropdown-toggle {:href "#" :data-toggle "dropdown"} (-> dd first clojure.string/capitalize)]
                    [:div.dropdown-menu
                      (for [sublink (rest dd) :let [subroot (str "/" root "/" (first dd) "/" sublink)]]
                        (dropdown-link req root (first dd) sublink))]]
                  (nav-link req root (clojure.string/lower-case link))))]
      ;; Login Icon
            [:span.nav-item.dropdown
              [:a#userDropdown.nav-link.dropdown-toggle.text-white {:href="#" :role "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
                [:i.fas.fa-user]
                (if-let [identity (friend/identity req)]
                  [:span.h5.mx-2 (:current identity)])]
                (if-let [identity (friend/identity req)]
                  [:div.dropdown-menu {:aria-labelledby "userDropdown"}
                    (if (friend/authorized? #{::db/admin} (friend/identity req))
                      [:a.dropdown-item {:href "/admin"} "Admin Console"])
                    [:a.dropdown-item {:href "/logout"} "Logout"]]
                  [:div.dropdown-menu {:aria-labelledby "userDropdown"}
                    [:a.dropdown-item {:href "/login"} "Login"]])]]]])
  ([req] (navbar "/img/danuraidb.png" "DanuraiDB" "/" [] req)))
  
  
  
(load "pages/lotrdb") 


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

;(defn lotrdb-deckbuilder2 [ req ]
;  (let [deckdata (model/get-deck-data req)]
;		(h/html5
;			lotrdb-pretty-head
;			[:body
;				(lotrdb-navbar req)
;        [:div.container.my-3
;          [:div.row.my-1
;            [:div.col-lg-6
;              [:div.sticky-top.pt-2
;                [:div.row.mb-2
;                  [:div.col
;                    [:form {:action "/decks/save" :method "post"}
;                      [:div.d-flex
;                        [:input#deckname.form-control {:name "name" :placeholder "Deckname" :required true :value (:name deckdata)}]
;                        [:button.btn.btn-warning.ml-1 {:submit "true" :title "Save"} 
;                          [:div.d-flex
;                            [:i.fas.fa-feather.my-auto] 
;                            [:span.ml-1.d-none.d-xl-inline "Save"]]]
;                        [:a.btn.btn-light.ml-1 {:href "/lotrdb/decks" :title "Cancel"}
;                          [:div.d-flex
;                            [:i.fas.fa-times.my-auto]
;                            [:span.ml-1.d-none.d-xl-inline "Cancel"]]]]
;                      [:div
;                        [:input#deckdata   {:name "data"   :hidden true :value (:data deckdata)}]
;                        [:input#deckuid    {:name "id"     :hidden true :value (:uid deckdata)}]
;                        [:input#decksystem {:name "system" :hidden true :value 0}]
;                        [:input#decktags   {:name "tags"   :hidden true :value (:tags deckdata)}]
;                        [:input#decknotes  {:name "notes"  :hidden true :value (:notes deckdata)}]]]]]
;                [:div.row [:div.col [:div#decklist]]]]]
;            [:div.col-lg-6
;              [:div.row
;                [:ul.nav.nav-tabs.nav-fill.w-100 {:role "tablist"}
;                  [:li.nav-item [:a.nav-link.active {:href "#buildtab" :data-toggle "tab" :role "tab" } "Build"]]
;                  [:li.nav-item [:a.nav-link {:href "#notestab" :data-toggle "tab" :role "tab"} "Notes"]]
;                  [:li.nav-item [:a.nav-link {:href "#testtab" :data-toggle "tab" :role "tab"} "Test"]]
;                  [:li.nav-item [:a.nav-link {:href "#collectiontab" :data-toggle "tab" :role "tab"} "Collection"]]]
;                [:div.tab-content.w-100
;                  [:div#buildtab.tab-pane.fade.show.active.mt-2 {:role "tabpanel"} 
;                    [:div.d-flex.mb-2.justify-content-between
;                      (btngroup player_type_icons "type_code")
;                      (btngroup sphere_icons "sphere_code")]
;                    [:input#filtertext.search-info.form-control {:type "text"}]
;                    ;[:div#info.row]
;                    [:table.table.table-sm.table-hover
;                      [:thead
;                        [:tr 
;                          [:th "Qty."]
;                          [:th.sortable {:data-field "normalname"} "Name"]
;                          [:th.sortable.text-center {:data-field "type_code" :title "Type"} "T."]
;                          [:th.sortable.text-center {:data-field "sphere_code" :title "Sphere"} "S."]
;                          [:th.sortable.text-center {:data-field "cost" :title "Cost/Threat"} [:span.lotr-type-threat]]
;                          [:th.sortable.text-center {:data-field "willpower" :title "Willpower"} [:span.lotr-type-willpower]]
;                          [:th.sortable.text-center {:data-field "attack" :title "Attack"} [:span.lotr-type-attack]]
;                          [:th.sortable.text-center {:data-field "defense" :title "Defense"} [:span.lotr-type-defense]]
;                          [:th.sortable.text-center {:data-field "health" :title "Health"} [:span.lotr-type-health]]
;                          ]]
;                      [:tbody#cardtbl]]]
;                  [:div#notestab.tab-pane.fade.p-2 {:role "tabpanel"}
;                    [:div.row-fluid.mb-2
;                      [:span.h5.mr-2 "Deck Tags (separate with spaces)"]
;                      [:input#tags.form-control]]
;                    [:div.row-fluid.mb-2
;                      [:span.h5.mr-2 "Deck Notes"]
;                      [:small.ml-2  
;                        [:a {
;                          :href "https://github.com/showdownjs/showdown/wiki/Showdown's-Markdown-syntax" 
;                          :target "_blank"}
;                          "Use showdown's markdown syntax"]]]
;                    [:div.row-fluid.mb-2
;                      [:textarea#notes.form-control {:rows 10}]]
;                    [:div#notesmarkdown.p-3 {:style "background-color: lightgrey;"}]]
;                  [:div#testtab.tab-pane.fade.mt-2 {:role "tabpanel"}
;                    [:div.row
;                      [:div.col
;                        [:div [:h5 "Test Draw"]]
;                        [:div.btn-group
;                          [:button#draw1.btn.btn-outline-secondary {:value 1 :title "Draw 1"} "1"]
;                          [:button#draw1.btn.btn-outline-secondary {:value 2 :title "Draw 2"} "2"]
;                          [:button#draw1.btn.btn-outline-secondary {:value 6 :title "Draw 6"} "6"]
;                          [:button#reset.btn.btn-outline-secondary {:title "Reset"} "Reset"]]
;                        [:div#drawcards.my-2]]]]
;                  [:div#collectiontab.tab-pane.fade.mt-2 {:role "tabpanel"}
;                    (let [cycles (model/get-cycles) packs (model/get-packs)]
;                      [:ul.list-group
;                        (for [c (sort-by :cycle_position cycles)]
;                          (if (= (:name c) "Core")
;                            ^{:key (gensym)}[:li.list-group-item
;                              [:div.d-flex
;                                [:span.font-weight-bold "Core Sets"]
;                                [:div#coresets.btn-group.btn-group-sm.btn-group-toggle.ml-auto {:data-toggle "buttons"}
;                                  [:label.btn.btn-outline-secondary
;                                    [:input#1core {:type "radio" :name "coresets" :value 1} "1"]]
;                                  [:label.btn.btn-outline-secondary
;                                    [:input#2core {:type "radio" :name "coresets" :value 2} "2"]]
;                                  [:label.btn.btn-outline-secondary
;                                    [:input#3core {:type "radio" :name "coresets" :value 3} "3"]]]]]
;                            (let [cpacks (sort-by :position (filter #(= (:cycle_position %) (:cycle_position c)) packs))]
;                              (if (> (count cpacks) 1)
;                                ^{:key (gensym)}[:li.list-group-item
;                                  [:div
;                                    [:input.mr-2 {:type "checkbox" :data-type "cycle" :data-id (:id c)}]
;                                    [:span.font-weight-bold (:name c)]]
;                                    (for [cp cpacks]
;                                      ^{:key (gensym)}[:div
;                                        [:input.mr-2 {:type "checkbox" :data-type "pack" :data-id (:code cp)}]
;                                        [:span (:name cp)]])]
;                                ^{:key (gensym)}[:li.list-group-item
;                                  [:div 
;                                    [:input.mr-2 {:type "checkbox" :data-type "pack" :data-id (-> cpacks first :code)}]
;                                    [:span.font-weight-bold (:name c)]]]))
;                        ))])]]]]]]
;        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
;          [:div.modal-dialog {:role "document"}
;            [:div.modal-content
;              [:div.modal-header
;                [:h5.modal-title]
;                [:span.buttons]
;                [:button.close {:data-dismiss "modal"} "x"]]
;              [:div.modal-body]
;              [:div.modal-footer]]]]
;	  (h/include-css "/css/lotrdb-icomoon-style.css?v=1.1")
;	  (h/include-js "/js/externs/typeahead.js")
;	  (h/include-js "/js/lotrdb/lotrdb_tools.js?v=1.0")
;	  (h/include-js "/js/lotrdb/lotrdb_popover.js?v=1.0")
;    (h/include-js "/js/lotrdb/lotrdb_deckbuilder.js?v=1.2")])))
    
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