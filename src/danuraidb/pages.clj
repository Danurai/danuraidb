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
                  [:a.btn.btn-primary {:href "/lotrdb/decks/fellowship/new" :title "New Deck"} [:i.fas.fa-plus]]]]
              [:div.d-flex
                [:ul#fellowshiplist.list-group.w-100
                  (for [f fellowships]
                    [:li.list-group-item {:key (:uid f)}
                      [:div.d-flex.justify-content-between
                        [:h5 (:name f)]
                        [:a.btn.btn-primary {:href (str "/lotrdb/decks/fellowship/" (:uid f))} "Edit"]]])
                  ]]]]]
        (deletemodal)
        (importallmodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/lotrdb/lotrdb_decklist.js?v=1.0")])))

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
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.mb-2
            [:div.col-12
              [:form#saveform.d-flex.mb-2
                [:h5.my-auto.mr-2 "Fellowship"]
                [:input#fellowshipname.form-control.mr-2 {:type "text" :name "name" :value (:name fdata) :required true}]  
                [:input#fellowshipid {:hidden true :value (:uid fdata) :name "data"}]
                [:button#savefellowship.btn.btn-warning.mr-2 {:role "submit" :title "Save Fellowship" :disabled true} [:i.fas.fa-feather]]
                [:a.btn.btn-outline-secondary {:formnovalidate true :title "Cancel Edits" :href "/lotrdb/decks"} "Cancel"]]
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
    (h/include-js  "/js/externs/typeahead.js")
    (h/include-js  "/js/lotrdb/lotrdb_tools.js")
    (h/include-js  "/js/lotrdb/lotrdb_fellowship.js"))))
        
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
                  
                  
                  