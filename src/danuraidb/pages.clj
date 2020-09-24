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
(load "pages/aosc")    
(load "pages/whuw")

(defn whuw-mortis-decks [ req ]
  (let [decks [] ;(db/get-user-decks 2 (-> req model/get-authentications (get :uid 1002)))
        card-data (model/whuw_fullcards)]
    (h/html5
      whuw-pretty-head
      [:body
        (whuw-navbar req)
        [:div.container-fluid.my-3
          [:div.col
            [:div.row-fluid.d-flex.justify-content-between.mb-2
              [:div.h4 (str "Decks (" (count decks) ")")]
              [:div 
                [:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
                [:a.btn.btn-primary {:href "/whuw/mortis/new" :title "New Deck"} [:i.fas.fa-plus]]]]
            ;[:div.row-fluid
            ;  [:div#decklists.w-100
            ;    [:ul.list-group
            ;      (map (fn [d] (whuw-deck-card d card-data)) decks)]]]
                  ]]
        (deletemodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/whuw/whuw_mortis_decklist.js?v=1.0")])))
  
(defn whuw-mortis-deckbuilder [ req ]
  (let [deck (model/get-deck-data req)]
    (h/html5
      whuw-pretty-head
      [:body 
        (whuw-navbar req)
        [:div.container.my-2
          [:div.col
            [:div.row.my-1
              [:div.col-sm-6
                [:div.pt-3  ;.sticky-top
                  [:div.row-fluid.mb-3
                    [:form#save_form.form.needs-validation {:method "post" :action "/decks/save" :role "form" :novalidate true}
                      [:div.d-flex.justify-content-around
                        [:img#deckicon.icon-sm.my-auto.mr-2 {:src (str whuw_icon_path "Shadespire-Library-Icons-Universal.png")}]
                        [:div.flex-grow-1.mr-2
                          [:input#deck-name.form-control {:type "text" :name "name" :placeholder "New Deck" :required true :value (:name deck) :data-lpignore "true"}]
                          [:div.invalid-feedback "You must name your deck"]]
                        [:button.btn.btn-warning.mr-2 {:role "submit"} 
                          [:i.far.fa-save]
                          [:span.d-none.d-lg-inline.ml-2 "Save"]]
                        [:a.btn.btn-secondary.mr-2 {:href "/whuw/decks" :title "Cancel"} [:i.fa.fa-times]]]
                      [:input#deck-id      {:type "text" :name "id"      :value (:uid deck) :readonly true :hidden true}]
                      [:input#deck-system  {:type "text" :name "system"   :value 2 :readonly true :hidden true}]
                      [:input#deck-alliance {:type "text" :name "alliance" :value (:alliance deck) :readonly true :hidden true}]
                      [:input#deck-content {:type "text" :name "data" :value (:data deck)  :readonly true :hidden true}]
                      [:input#deck-tags    {:type "text" :name "tags"    :value (:tags deck) :readonly true :hidden true}]
                      [:input#deck-notes   {:type "text" :name "notes"   :value (:notes deck) :readonly true :hidden true}]]]
                  [:div.row-fluid.mb-2
                    [:div.d-flex
                      [:span.mr-2.my-auto "Champion"]
                      [:select#selectchamp.selectpicker.flex-grow-1
                        (for [champ model/whuwchamps]
                          [:option {:key (gensym) :data-content (str "<div><img class=\"icon-sm mr-2\" src=\"" whuw_icon_path (:icon champ) "\">" (:name champ))} (:name champ)])
                        ]]]
                  [:div#decklist.row-fluid]
              ]]
              [:div.col-sm-6
                [:div.row.mb-2
                  [:div.mr-2.my-auto "Set filter"]
                  [:select#selectset.selectpicker {:multiple true :data-width "auto"}
                    (for [item (sorted_vec gw_sets (:sets ordered_lists))]
                      (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                      ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]
                  [:button#championstoggle.ml-auto.btn.btn-outline-warning.active {:data-toggle "button" :title "Championship Legal" :aria-pressed "true"} [:i.far.fa-bookmark]]
                  ]
                [:div.row.mb-2
                  ;[:span.mr-2.my-auto "Warband"]
                  ;[:select#selectwarband.selectpicker.mr-2 {:multiple true :data-width "fit"}
                  ;  (for [item (sorted_vec gw_warbands (:warbands ordered_lists))]
                  ;    (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                  ;    ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]
                  [:span.mr-2.my-auto "Type filter"]
                  [:select#selecttype.selectpicker {:multiple true :data-width "auto"}
                    (for [item (sorted_vec gw_card-types (:card-types ordered_lists))]
                      (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                      ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]]                
                [:div.row 
                  [:div.col-md-12
                    [:div.row 
                      [:input#filtertext.form-control.w-100 {:type "text"}]]]]
                [:div#info.row]
                [:div.row
                  [:table.table.table-sm.table-hover
                    [:thead
                      [:tr 
                        [:td.sortable {:data-field "name"} "Name"]
                        [:td.sortable.text-center {:data-field "card_type_id"} "Type"]
                        [:td.sortable.text-center {:data-field "set_id"} "Set"]
                        [:td.sortable.text-center {:data-field "warband_id"} "Warband"]
                        [:td.sortable.text-center {:data-field "glory"} "Glory"]
                        [:td]]]
                    [:tbody#cardtbl]]]
              ]]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-sm {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]]]]
      (h/include-js "/js/externs/typeahead.js")
      (h/include-js "/js/whuw/whuw_mortisdb.js")])))

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