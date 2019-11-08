'(in-ns danuraidb.pages)

(def ^:const whuw_icon_path "/img/whuw/icons/")

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
    (str whuw_icon_path "Shadespire-Library-Icons-Universal.png")
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
            
(defn- whuw-export-string [ deck-cards ]
  (clojure.string/join "\n"
    (map (fn [id]
      (if-let [cards (->> deck-cards (filter #(= (:card_type_id %) id)))]
        (clojure.string/join "\n" (concat [(-> cards first :card_type_name)] (map :name cards)))))
      [20,21,150,22])))



(defn- whuw-deck-card [ d card-data ]
  (let [cardlist (-> d :data json/read-str set)
        deck-cards (filter #(some (partial = (:code %)) cardlist) card-data)]
    [:li.list-group-item.list-deck-card
      [:div.d-flex.justify-content-between {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))}
        [:img.icon-sm.mr-2 {:src (str whuw_icon_path (get (->> deck-cards (filter #(not= 35 (:warband_id %))) first)  :warband_icon "Shadespire-Library-Icons-Universal.png"))}]
        [:span.h4 (:name d)]
        [:div.ml-auto
          (map (fn [id]
            (if-let [img (->> deck-cards (filter #(= (:card_type_id %) id)) first :card_type_icon)]
              [:div
                [:span.mr-1 [:img.icon-xs {:src (str whuw_icon_path img)}]]
                [:span.align-bottom.mr-2 (->> deck-cards (filter #(= id (:card_type_id %))) count)]]))
            [20 21 150 22])]
        [:span {:style "position: absolute; top: 0px; right: 0px;"}
          [:button.btn.btn-sm {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} [:i.fas.fa-xs.fa-plus]]]]
      [:div.collapse {:id (str "deck_" (:uid d))} 
        [:div.row.mb-2
          (map (fn [id]
            (let [type (->> deck-cards (filter #(= (:card_type_id %) id)) (sort-by :name))]
              (if (not-empty type)
                [:div.col-sm-3
                  [:div 
                    [:img.icon-xs.mr-1 {:src (str whuw_icon_path (-> type first :card_type_icon))}]
                    [:span.h5.align-bottom (-> type first :card_type_name)]]
                  (for [t type]
                    [:div.cardlink {:data-code (:code t)} (:name t)])])))
            [20 21 150 22])]
        [:div
          [:button.btn.btn-sm.btn-danger.mr-1 {:data-toggle "modal" :data-target "#deletemodal" :data-name (:name d) :data-uid (:uid d)} [:i.fas.fa-times.mr-1] "Delete"]
          [:button.btn.btn-sm.btn-success.mr-1 {:data-toggle "modal" :data-target "#exportdeck" :data-export (whuw-export-string deck-cards) :data-deckname (:name d)} [:i.fas.fa-file-export.mr-1] "Export"]
          [:a.btn.btn-sm.btn-primary {:href (str "/whuw/decks/edit/" (:uid d))} [:i.fas.fa-edit.mr-1] "Edit"]]]]))
          

(defn whuw-decks [req]
  (let [decks (db/get-user-decks 2 (-> req model/get-authentications (get :uid 1002)))
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
                [:a.btn.btn-primary {:href "/whuw/decks/new" :title "New Deck"} [:i.fas.fa-plus]]]]
            [:div.row-fluid
              [:div#decklists.w-100
                [:ul.list-group
                  (map (fn [d] (whuw-deck-card d card-data)) decks)]]]]]
        (deletemodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/whuw_decklist.js?v=1.0")])))
        
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
                  [:form#save_form.form.needs-validation {:method "post" :action "/decks/save" :role "form" :novalidate true}
                    [:div.form-row.align-items-center
                      [:div.col-auto
                        [:img#deckicon.icon-sm {:src (str whuw_icon_path "Shadespire-Library-Icons-Universal.png")}]]
                      [:div.col-auto
                        [:label.sr-only {:for "#deck-name"} "Deck Name"]
                        [:input#deck-name.form-control {:type "text" :name "name" :placeholder "New Deck" :required true :value (:name deck) :data-lpignore "true"}]
                        [:div.invalid-feedback "You must name your deck"]]
                      [:div.col-auto
                        [:button.btn.btn-warning.mr-2 {:role "submit"} "Save"]
                        [:a.btn.btn-light.mr-2 {:href "/whuw/decks"} "Cancel Edits"]]]
                    [:input#deck-id      {:type "text" :name "id"      :value (:uid deck) :readonly true :hidden true}]
                    [:input#deck-system  {:type "text" :name "system"   :value 2 :readonly true :hidden true}]
                    [:input#deck-alliance {:type "text" :name "alliance" :readonly true :hidden true}]
                    [:input#deck-content {:type "text" :name "data" :value (:data deck)  :readonly true :hidden true}]
                    [:input#deck-tags    {:type "text" :name "tags"    :value (:tags deck) :readonly true :hidden true}]
                    [:input#deck-notes   {:type "text" :name "notes"   :value (:notes deck) :readonly true :hidden true}]]]
                [:div#decklist.row-fluid]
            ]]
            [:div.col-sm-6
              [:div.row.mb-2
                [:div.mr-2.my-auto "Set filter"]
                [:select#selectset.selectpicker {:multiple true :data-width "fit"}
                  (for [item (sorted_vec gw_sets (:sets ordered_lists))]
                    (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                    ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]]
              [:div.row.mb-2
                [:span.mr-2.my-auto "Warband"]
                [:select#selectwarband.selectpicker.mr-2 {:multiple true :data-width "fit"}
                  (for [item (sorted_vec gw_warbands (:warbands ordered_lists))]
                    (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                    ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]
                [:span.mr-2.my-auto "Type filter"]
                [:select#selecttype.selectpicker {:multiple true :data-width "fit"}
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
      

(defn whuw-cards [ req ]
  (h/html5
    whuw-pretty-head
    [:body
      (whuw-navbar req)
      [:div.container.my-3
        [:div.row 
          [:div.mr-2.my-auto "Set filter"]
          [:select#selectset.selectpicker.mr-2 {:multiple true :data-width "fit"}
            (for [item (sorted_vec gw_sets (:sets ordered_lists))]
              (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
              ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]
          [:span.mr-2.my-auto "Warband"]
          [:select#selectwarband.selectpicker.mr-2 {:multiple true :data-width "fit"}
            (for [item (sorted_vec gw_warbands (:warbands ordered_lists))]
              (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
              ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]
          [:span.mr-2.my-auto "Type filter"]
          [:select#selecttype.selectpicker {:multiple true :data-width "fit"}
            (for [item (sorted_vec gw_card-types (:card-types ordered_lists))]
              (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
              ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]]
        [:div#results.row]]
      (h/include-js "/js/whuw_cards.js?v=1")]))