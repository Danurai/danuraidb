(ns danuraidb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [clj-time.core :as time]
    [clj-time.coerce :as tc]
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
                  
(load "pages/common")
(load "pages/lotrdb")  

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
        [:div.container.my-3
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

(defn lotrdb-score-page [ req ]
  (h/html5
    lotrdb-pretty-head
    [:body
      (lotrdb-navbar req)
      [:div.container.my-3
        [:form
          [:div.form-row
            [:div.form-group.col-2
              [:label "Deck"]
              [:input.form-control {:type "text" :name "deck" :required true}]]
            [:div.form-group.col-2
              [:label "Scenario"]
              [:input.form-control {:type "text" :name "scenario" :required true}]]
            [:div.form-group.col-1
              [:label "Final Threat"]
              [:input#threat.form-control {:type "number" :name "threat" :required true}]]
            [:div.form-group.col-1
              [:label.small "Threat of all Dead Heros"]
              [:input#dthreat.form-control {:type "number" :name "dthreat" :required true}]]
            [:div.form-group.col-1
              [:label.small "Damage on all Heros"]
              [:input#dmg.form-control {:type "number" :name "dmg" :required true}]]
            [:div.form-group.col-1
              [:label "- Victory Points"]
              [:input#vps.form-control {:type "number" :name "vps" :required true}]]
            [:div.form-group.col-1
              [:label.h5 "Player Subtotal"]
              [:label#p1sub ]]
            [:div.form-group.col-1
              [:label "Turns (x10)"]
              [:input#turns.form-control {:type "number" :name "turns" :required true}]]
            [:div.form-group.col-1
              [:label.h5 "Total Score"]
              [:label#total]]
            ]]
        
          
      ]]))
      
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