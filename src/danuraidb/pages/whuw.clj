'(in-ns danuraidb.pages)

(def whuw-pretty-head
  (into pretty-head (h/include-css "/css/whuw-style.css?v=1.0")))
  
(def gw_metadata
  (-> (io/resource "private/whuw_data_r2.json")
      slurp 
      (json/read-str :key-fn keyword)))
(def gw_card-types (:card-types gw_metadata))
(def gw_sets      (:sets gw_metadata))
(def gw_warbands  (:warbands gw_metadata))
(def ordered_lists {
  :card-types [20 21 22 150]
  :warbands [35 31 34 33 32 93 107 119 120 162 168 211 203 257 258 259 260]
  :sets [30 24 25 92 106 121 122 143 156 218 225 233 232 257 258 259 260]})

(defn sorted_vec [vec order]
  (->> vec  
      (map #(let [pos (.indexOf order (:id %))]
              (assoc % :position (if (>= pos 0) pos 999))))
      (sort-by :position)))
  
(defn whuw-navbar [req]
  (navbar 
    "/img/whuw/icons/Shadespire-Library-Icons-Universal.png" 
    "WHUW DB" 
    "whuw"
    ["decks" "cards"]
    req))
                  
(defn whuw-home [ req ]
  (h/html5
    whuw-pretty-head
    [:body
      (whuw-navbar req)
      [:body
        [:div.container.my-3
          [:div.row
            [:a {:href "/whuw/decks"} "Login"] 
            [:span.ml-1 "to see your decks"]]]]]))
            


(defn- whuw-deck-card [deck]
  [:a.list-group-item.list-group-item-action {:href (str "/deck/edit/" (:uid deck))} 
    [:div.d-flex.justify-content-between
      [:span (:name deck)]
      [:form {:action "/whuw/deck/delete" :method "post"}
        [:input#deletedeckuid {:name "deletedeckuid" :hidden true :data-lpignore "true" :value (:uid deck)}]
        [:button.btn.btn-danger.btn-sm {:type "submit" :title "Delete Deck"} "x"]]]])
        
(defn whuw-decks [ req ]
  (let [user-decks (db/get-user-decks 2 (-> req get-authentications :uid) )]
    (h/html5
      whuw-pretty-head
      [:body
        (whuw-navbar req)
        [:div.toaster.ddb-toaster]
        [:div.container.my-2
          [:div.row.my-1
            [:a.btn.btn-secondary {:href "/whuw/decks/new"} "New Deck"]]
          [:div.row.my-1
            [:div.col-md-6
              [:div.row.mb-2 (str "Saved decks (" (count user-decks) ")")]
              [:div.list-group
                (map #(whuw-deck-card %) user-decks)]]]]])))

(defn whuw-deckbuilder [req]
  (let [deck (model/get-deck-data req)]
    (h/html5
      whuw-pretty-head
      [:body 
        (whuw-navbar req)
        [:div.container.my-2
          [:div.row.my-1
            [:div.col-sm-6
              [:div.sticky-top.pt-3
                [:div.row-fluid.mb-3
                  [:form#save_form.form.needs-validation {:method "post" :action "/deck/save" :role "form" :novalidate true}
                    [:div.form-row.align-items-center
                      [:div.col-auto
                        [:img#deckicon.icon-sm {:src "/img/whuw/icons/Shadespire-Library-Icons-Universal.png"}]]
                      [:div.col-auto
                        [:label.sr-only {:for "#deck-name"} "Deck Name"]
                        [:input#deck-name.form-control {:type "text" :name "deck-name" :placeholder "New Deck" :required true :value (:name deck) :data-lpignore "true"}]
                        [:div.invalid-feedback "You must name your deck"]]
                      [:div.col-auto
                        [:button.btn.btn-warning.mr-2 {:role "submit"} "Save"]
                        [:a.btn.btn-light.mr-2 {:href "/"} "Cancel Edits"]]]
                    [:input#deck-id      {:type "text" :name "deck-id"      :value (:uid deck) :readonly true :hidden true}]
                    [:input#deck-content {:type "text" :name "deck-content" :value (:data deck)  :readonly true :hidden true}]
                    [:input#deck-tags    {:type "text" :name "deck-tags"    :value (:tags deck) :readonly true :hidden true}]
                    [:input#deck-notes   {:type "text"  :name "deck-notes"  :value (:notes deck) :readonly true :hidden true}]]]
                [:div#decklist.row-fluid]
            ]]
            [:div.col-sm-6
              [:div.row.mb-2
                [:div.mr-2.my-auto "Set filter"]
                [:select#selectset.selectpicker {:multiple true :data-width "fit"}
                  (for [item (sorted_vec gw_sets (:sets ordered_lists))]
                    (let [imgtag (str "<img class=\"icon-sm\" src=\"/img/whuw/icons/" (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                    ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]]
              [:div.row.mb-2
                [:span.mr-2.my-auto "Warband"]
                [:select#selectwarband.selectpicker.mr-2 {:multiple true :data-width "fit"}
                  (for [item (sorted_vec gw_warbands (:warbands ordered_lists))]
                    (let [imgtag (str "<img class=\"icon-sm\" src=\"/img/whuw/icons/" (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                    ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]
                [:span.mr-2.my-auto "Type filter"]
                [:select#selecttype.selectpicker {:multiple true :data-width "fit"}
                  (for [item (sorted_vec gw_card-types (:card-types ordered_lists))]
                    (let [imgtag (str "<img class=\"icon-sm\" src=\"/img/whuw/icons/" (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                    ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]]
              [:div.row 
                [:div.col-md-12
                  [:div.row 
                    [:input#filtertext.form-control {:type "text"}]]]]
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
      (h/include-js "/js/whuw_deckbuilder.js")])))