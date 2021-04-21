'(in-ns danuraidb.pages)

(def ^:const whuw_icon_path "/img/whuw/icons/")

(def whuw-pretty-head
  (into pretty-head (h/include-css "/css/whuw-style.css?v=1.0")))
  
(def gw_metadata
  (-> (io/resource "private/whuw/whuw_data_r2.json")
      slurp 
      (json/read-str :key-fn keyword)))
(def gw_card-types (:card-types gw_metadata))
(def gw_sets      (:sets gw_metadata))
(def gw_warbands  (:warbands gw_metadata))
(def ordered_lists {
  :card-types [20 21 150 22]
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
    ["decks" "mortis" "cards" "boards" "collection"]  ;"champions"
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
        deck-cards (filter #(some (partial = (:code %)) cardlist) card-data)
        warband_exemplar (->> deck-cards (filter #(not= 35 (:warband_id %))) first)]
    [:li.list-group-item.list-deck-card
      [:div {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))}
        [:div.d-flex.justify-content-between
          [:span {:style "position: absolute; top: 0px; right: 0px;"}
            [:button.btn.btn-sm {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} [:i.fas.fa-xs.fa-plus]]]
          [:img.icon-sm.mr-2 {:src (str whuw_icon_path (get warband_exemplar :warband_icon "Shadespire-Library-Icons-Universal.png"))}]
          [:div.h4 (:name d)]
          [:div.ml-auto
            [:span.d-none.d-sm-block
              (map (fn [id]
                (if-let [img (->> deck-cards (filter #(= (:card_type_id %) id)) first :card_type_icon)]
                  [:span
                    [:span.mr-1 [:img.icon-xs {:src (str whuw_icon_path img)}]]
                    [:span.align-bottom.mr-2 (->> deck-cards (filter #(= id (:card_type_id %))) count)]]))
                [20 21 150 22])]]]
        [:div (get warband_exemplar :warband_name "Unknown Warband")]]
      [:div.collapse {:id (str "deck_" (:uid d))} 
        [:div.row.my-2
          (map (fn [id]
            (let [type (->> deck-cards (filter #(= (:card_type_id %) id)) (sort-by :name))]
              (if (not-empty type)
                [:div.col-sm-3
                  [:div 
                    [:img.icon-xs.mr-1 {:src (str whuw_icon_path (-> type first :card_type_icon))}]
                    [:span.mt-auto
                      [:b (-> type first :card_type_name)] [:span (str " (" (count type) ")")]]]
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
        (h/include-js "/js/whuw/whuw_decklist.js?v=1.0")])))
        
(defn whuw-deckbuilder [req]
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
                      [:div.d-flex
                        [:img#deckicon.icon-sm.my-auto.mr-1 {:src (str whuw_icon_path "Shadespire-Library-Icons-Universal.png")}]
                        [:label.sr-only {:for "#deck-name"} "Deck Name"]
                        [:div.flex-grow-1.mr-2
                          [:input#deck-name.form-control {:type "text" :name "name" :placeholder "New Deck" :required true :value (:name deck) :data-lpignore "true"}]
                          [:div.invalid-feedback "You must name your deck"]]
                        [:div.ml-auto
                          [:button.btn.btn-warning.mr-2 {:role "submit"} "Save"]
                          [:a.btn.btn-light.mr-2 {:href "/whuw/decks"} [:i.fas.fa-times]]]]
                      [:input#deck-id      {:type "text" :name "id"      :value (:uid deck) :readonly true :hidden true}]
                      [:input#deck-system  {:type "text" :name "system"   :value 2 :readonly true :hidden true}]
                      [:input#deck-alliance {:type "text" :name "alliance" :value (:alliance deck) :readonly true :hidden true}]
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
                      ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]
                  [:button#championstoggle.mx-2.btn.btn-outline-warning.active.ml-auto {:data-toggle "button" :title "Championship Legal" :aria-pressed "true"} [:i.far.fa-bookmark]]
                  ]
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
              ]]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-sm {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]]]]]
      (h/include-css "/css/rpg-awesome.min.css")
      (h/include-js "/js/externs/typeahead.js")
      (h/include-js "/js/whuw/whuw_popups.js")
      (h/include-js "/js/whuw/whuw_deckbuilder.js")
    )))
      

(defn whuw-cards-old [ req ]
  (h/html5
    whuw-pretty-head
    [:body
      (whuw-navbar req)
      [:div.container.my-3
        [:div.col
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
                ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]
            [:span.mr-2.my-auto "Sort by"]
            [:div.d-flex [:select#sort.form-control
              [:option {:value "type"} "Sort by Card Type"]
              [:option {:value "set"} "Sort by Card Set"]
              [:option {:value "name"} "Sort by Card Name"]]]]
          [:div#results.row]]]
      (h/include-js "/js/whuw/whuw_cards.js?v=1")]))
      
(defn whuw-boards [ req ]
  (let [ formats (:formats model/whuwdata)]
    (h/html5
      whuw-pretty-head
      [:body.text-light {:style "background-color: #222;"}
        (whuw-navbar req)
        [:div.container.my-3
          [:div.row
            (for [b (-> model/whuwdata :boards)]
              [:div.col-sm-6.mb-2
                [:div.border.border-secondary.bg-dark
                  [:div.mb-1
                    [:img.img-fluid {:src (str "/img/whuw/boards/" (:filename b))}]]
                  [:div.ml-2.mb-1
                    [:div.mb-1
                      [:span.h5.mr-2 (:name b)]
                      [:span (:set_name b)]] 
                    [:div.d-flex.justify-content-between
                      (for [ [fk fv] formats ]
                        [:span.mr-2
                          (if (or (= (:boards fv) ["all"]) (contains? (-> fv :boards set) (:name b)))
                              [:span.badge.badge-success.py-1.px-2 fk]
                              [:span.badge.badge-danger.py-1.px-2 fk])])]]]])]]])))

(defn getfilename [ url ]
  (re-find #"[\w|\-]+\.png$" url))
  
(defn whuw-mortis-champs [ req ]
  (let [warbands (:warbands model/whuwdata)
        sets     (:sets model/whuwdata)
        champs   (:champions model/whuwchamps)]
    (h/html5
      whuw-pretty-head
      [:body
        (whuw-navbar req)
        [:div.container-fluid.my-3            
          [:div.row-fluid.mb-2 {:style "position: relative;"}
            [:div#fullimg {:style "position: fixed; right: 10px; top: 10px; width: 300px; z-index: 1040;"}]
            [:div.btn-group-toggle {:data-toggle "buttons"}
              [:label.btn.btn-secondary
                [:input#instoggle {:type "checkbox"}] "Inspired"]]]
          [:div.row-fluid
            [:ul.nav.nav-tabs {:role "tablist"}
              [:li.nav-item
                [:a.nav-link.active {:data-toggle "tab" :href "#championstab" :role "tab"} "Champions"]]
              [:li.nav-item
                [:a.nav-link {:data-toggle "tab" :href "#collectiontab" :role "tab"} "Collection"]]]
            [:div.tab-content.my-2
              [:div#championstab.tab-pane.fade.active.show  {:role "tabpanel"}
                [:div.list-group
                  (for [wb (filter #(-> % :members count (> 0)) warbands)]
                    [:div.list-group-item {:data-warband_id (:id wb)}
                      [:div.d-flex.mb-2
                        [:img.icon-sm.mr-2 {:src (str "/img/whuw/icons/" (-> wb :icon :filename))}]
                        [:b (:name wb)]]
                      [:div.d-flex.flex-wrap
                        (for [ch (filter #(= (:warband_id %) (:id wb)) champs)]
                          [:div.mr-2.mb-2
                            [:div
                              [:img.champ.normal {:style "width: 100px;" :src (str "/img/whuw/champs/" (-> ch :cards first :url getfilename))}]
                              [:img.champ.inspired {:hidden true :style "width: 100px;" :src (str "/img/whuw/champs/" (-> ch :cards last :url getfilename))}]]
                            [:a.btn.btn-sm.btn-warning.my-1.w-100 {:href (str "/whuw/mortis/new/" (:id ch))} "Create Deck"]])]])]]
              [:div#collectiontab.tab-pane.fade {:role "tabpanel"}
                [:div.list-group.list-group-small
                  (for [wb (filter #(-> % :members count (> 0)) warbands)]
                    [:div.list-group-item
                      [:input.mr-2 {:type "checkbox" :data-warband_id (:id wb)}]
                      [:img.icon-sm.mr-2  {:src (str "/img/whuw/icons/" (-> wb :icon :filename))}]
                      [:span (:name wb)]])]]]]]]
      (h/include-js "/js/whuw/whuw_champions.js"))))
      
;;; ARENA MORTIS
(defn whuw-mortis-deck-card [ d card-data ]
  (let [cardlist (-> d :data json/read-str set)
        deck-cards (filter #(some (partial = (:code %)) cardlist) card-data)
        warband_exemplar (->> deck-cards (filter #(not= 35 (:warband_id %))) first)]
    [:div.list-group-item
      [:div {:data-toggle "collapse" :data-target (str "#deck_" (:uid d))}
        [:div.d-flex
          [:img.my-auto.icon-sm.mr-2 {:src (str "/img/whuw/icons/" (:warband_icon warband_exemplar))}] 
          [:h5 (:name d)]
          [:span.ml-auto
            [:button.deletedl.btn.btn-sm.btn-danger.mr-1 {
              ;:data-toggle "modal" :data-target "#deletemodal" 
              :data-uid (:uid d) :data-name (:name d)} [:i.fa.fa-times.mr-1] "Delete"]
            ;[:button.btn.btn-sm.btn-success "Export"]
            [:a.btn.btn-sm.btn-primary {:href (str "/whuw/mortis/edit/" (:uid d)) :onclick "event.stopPropagation();"} [:i.fa.fa-edit.mr-1] "Edit"]]]]
      [:div.collapse {:id (str "deck_" (:uid d))}
        [:div.decklist
          (for [[cardtype cardids] [["Ploys",#{21,150}],["Upgrades",#{22}]]
                  :let [cards (->> deck-cards (filter #(contains? cardids (:card_type_id %))) (sort-by :name))]]
            [:div.decklist-section 
              [:b (str cardtype " (" (count cards) ")")]
              [:br]
              (for [c cards]
                [:span 
                  [:a.cardlink {:href "#" :data-code (:code c)} (:name c)]
                  [:br]])])]]]))

(defn whuw-mortis-decks [ req ]
  (let [decks (db/get-user-decks 4 (-> req model/get-authentications (get :uid 1002)))
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
                ;[:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
                [:a.btn.btn-primary {:href "/whuw/mortis/new" :title "New Deck"} [:i.fas.fa-plus]]]]
            [:div.row-fluid
              [:div#decklists.w-100
                [:ul.list-group
                  (map (fn [d] (whuw-mortis-deck-card d card-data)) decks)]]]
                  ]]
        (deletemodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/whuw/whuw_mortisdl.js?v=1.0")
        (h/include-js "/js/whuw/whuw_popups.js?v=1.0")
      ])))


(defn cardtbl [ mortis? ]
  [:div.w-100 [:table#cardtbl.table.table-sm.table-hover.w-100
    [:thead
      [:tr 
        [:td "#"]
        [:th "Name"]
        [:th.text-center "Type"]
        [:th.text-center "Set"]
        [:th.text-center "Warband"]
        [:th.text-center "Glory"]
        [:th.d-none "target"]
        [:th.d-none "legal"]]]
    [:tbody
        (for [crd (if mortis? (filter #(not= (:card_type_id %) 20) (model/whuw_fullcards)) (model/whuw_fullcards))]
          [:tr
            [:td.text-center [:i.far.fa-square.tblcheck {:data-code (:code crd)}]]
            [:td
              (into (if (:championship_legal crd) [:div] [:del])
                [[:a.cardlink.mr-1 {:href "#" :data-target "#cardmodal" :data-toggle "modal" :data-code (:code crd)}(:name crd)]
                (if (:reprint_set_name crd) [:sup {:title (str "Reprint: " (:reprint_set_name crd))} "r"])])]
            [:td.text-center {:data-sort (:card_type_id crd) :data-filter (:card_type_id crd)} [:img.icon-sm {:src (str "/img/whuw/icons/" (:card_type_icon crd)) :title (:card-type_name crd)}]]
            [:td.text-center {:data-sort (:set_id crd)       :data-filter (:set_id crd)}       [:img.icon-sm {:src (str "/img/whuw/icons/" (:set_icon crd))       :title (:set_name crd)}]]
            [:td.text-center {:data-sort (:warband_id crd)   :data-filter (:warband_id crd)}   [:img.icon-sm {:src (str "/img/whuw/icons/" (:warband_icon crd))   :title (:warband_name crd)}]]   
            [:td.text-center (:glory crd "")]
            [:td.d-none (:target crd "all")]
            [:td.d-none (if (:championship_legal crd) true false)]])]]])
  
(defn whuw-mortis-deckbuilder [ req ]
  (let [deck (model/get-deck-data req)
        champ (or (:alliance deck) (-> req :params :champ))
        warbands (:warbands model/whuwdata)]
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
                      [:input#deck-system  {:type "text" :name "system"   :value 4 :readonly true :hidden true}]
                      [:input#deck-alliance {:type "text" :name "alliance" :value champ :readonly true :hidden true}] ; Champion
                      [:input#deck-content {:type "text" :name "data" :value (:data deck)  :readonly true :hidden true}]
                      [:input#deck-tags    {:type "text" :name "tags"    :value (:tags deck) :readonly true :hidden true}]
                      [:input#deck-notes   {:type "text" :name "notes"   :value (:notes deck) :readonly true :hidden true}]]]
                  [:div.row.mb-2.mr-2
                    [:span.mr-2.my-auto "Champion"]
                    [:select#selectchamp.selectpicker.flex-grow-1 
                      (for [champ (:champions model/whuwchamps) :let [icon (->> warbands (filter #(= (:id %) (:warband_id champ))) first :icon :filename)]]
                          [:option {:key (gensym) :data-content (str "<div><img class=\"icon-sm mr-2\" src=\"" whuw_icon_path icon "\">" (:name champ) "</div>")} 
                            (:id champ)])]]
                  [:div.row 
                    [:div.col-4
                      [:div.row 
                        [:img#champimg.img-fluid {:data-toggle "modal" :data-target "#champmodal"}]]]
                    [:div.col-7
                      [:div#decklist.row-fluid]]]]]
              [:div.col-sm-6
                [:div.row.mb-2
                  [:div.d-flex
                    [:span.mr-1.my-auto "Set filter"]
                    [:select#selectset.selectpicker.mr-1 {:multiple true :data-width "fit"}
                      (for [item (sorted_vec gw_sets (:sets ordered_lists))]
                        (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                        ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]]
                  [:div.ml-auto [:button#championstoggle.btn.btn-outline-warning.active {:data-toggle "button" :title "Championship Legal" :aria-pressed "true"} [:i.far.fa-bookmark]]]]
                [:div.row.mb-2
                  [:div.d-flex 
                    [:span.mr-1.my-auto "Type filter"]
                    [:select#selecttype.selectpicker.mr-1 {:multiple true :data-width "fit"}
                      (for [item (sorted_vec (remove #(= (:id %) 20) gw_card-types) (:card-types ordered_lists))]
                        (let [imgtag (str "<img class=\"icon-sm\" src=\"" whuw_icon_path (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                        ^{:key (gensym)}[:option {:data-content imgtag} (:id item)]))]]
                  [:div.d-flex 
                    [:span.mr-1.my-auto "Warband Toggle"]
                    [:span#warbandfilter.my-auto [:span.text-muted "(All)"]]]]
                [:div.row.mb-2
                  [:input#search.form-control {:type "text" :placeholder "Search"}]]
                [:div.row (cardtbl true)]
              ]]]]
        [:div#champmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content
              [:div.modal-body
                [:img.img-fluid {:data-dismiss "modal"}]]]]]
        [:div#cardmodal.modal {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-sm {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title]
                [:span.buttons]
                [:button.close {:data-dismiss "modal"} "x"]]
              [:div.modal-body]]]]]
    (h/include-css "/css/rpg-awesome.min.css")
    (h/include-css "//cdn.datatables.net/1.10.20/css/dataTables.bootstrap4.min.css")
    (h/include-js "//cdn.datatables.net/1.10.22/js/jquery.dataTables.min.js")
    (h/include-js "//cdn.datatables.net/1.10.22/js/dataTables.bootstrap4.min.js")
	  (h/include-js "/js/externs/typeahead.js")
    (h/include-js "/js/whuw/whuw_popups.js")
    (h/include-js "/js/whuw/whuw_mortisdb.js"))))

  
(defn whuw-cards [ req ]  
  (let [ sets     (->> model/whuwdata2 :sets (map (fn [[k, v]] v)) (sort-by :id)) 
         factions (->> model/whuwdata2 :factions (map (fn [[k v]] v)) (sort-by :id)) 
         cards    (->> model/whuwdata2 :cards (map (fn [[k v]] v)) (sort-by :id))  ]
    (h/html5
      whuw-pretty-head
      [:body.text-light {:style "background-color: #222;"}
        (whuw-navbar req)
        [:div.container-fluid.py-3
          [:div#info.bg-dark.w-100 {:style "position: fixed; bottom: 0px; left: 0px; padding-left: 1rem; z-index: 99;"}
            [:small
              [:span.mr-1 "Warhammer Underworlds is &#169; "]
              [:a.mr-1 {:href="https://warhammerunderworlds.com/"} "Games Workshop."]
              [:span.mr-1 "Warband images and data courtesy of"] 
              [:a.mr-1 {:href "https://github.com/PompolutZ/yawudb"} "https://github.com/PompolutZ/yawudb"]
              [:a.mr-1 {:href "https://yawudb.com/"} "yawudb.com"]
             ]]
          [:div.container.mb-2
            [:div.d-flex
              [:div#factionset.input-group.mr-2
                [:div.input-group-prepend
                  [:button#fs-toggle.btn.btn-secondary.active {:data-toggle "button" :title "Faction / Set Toggle"} [:i.fas.fa-sync-alt]]
                  [:label#fs-label.input-group-text.bg-dark.text-light "Faction:"]]
                [:select#set.form-control.bg-dark.text-light.d-none {:style "border-radius: 0 .25rem .25rem 0;"}
                  (for [ set sets ]
                    [:option (:displayName set)])]
                [:select#faction.form-control.bg-dark.text-light
                  (for [ faction factions ]
                    [:option (:displayName faction)])]]
                [:label.my-auto.mr-2 "Layout"]
              [:select#display.form-control.bg-dark.text-light 
                (for [ display ["Formatted", "Image", "List"] ]
                  [:option display])]]]
          [:div#faction-members.mb-2]
          [:div#card-list.mb-2]
        [:div#card-modal.modal {:tabindex -1 :role "modal"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content {:style "border: none;"}
              [:div.modal-body.bg-dark.rounded]]]]]]
      [:script {:src "/js/whuw/whuw_yauwdbdata.js" :type "module"}]
      (h/include-css "/css/whuw-style.css")
      (h/include-css "/css/whuw-icomoon-style.css"))))

(defn whuw-collection [ req ]
  (let [sets (->> model/whuwdata2 :sets vals (sort-by :id))]
    (h/html5
      whuw-pretty-head
      [:body
        (whuw-navbar req)
          [:div.container.my-3
            [:div.row
              [:div#sets.col-sm-4
                [:h5#collection.text-center "Collection"]
                (for [v sets]
                  [:div.d-flex 
                    [:input.mr-2.my-auto {:type "checkbox" :data-setid (:id v) }]
                    [:span (:displayName v)]])
                [:div.mt-2 [:b "Add Cards"]
                [:input#addcards.form-control.typeahead]
                [:div#extracardlist]]]
              [:div.col-sm-8
                [:h5 "Card Search"]
                [:input#searchcards.form-control.typeahead]
                [:div#cardinfo]]]]]
        (h/include-js "/js/whuw/whuw_collection.js?v=0.1")
        (h/include-js "/js/externs/typeahead.js?v=1.0")
        (h/include-css "/css/whuw-style.css?v=1.0")
        (h/include-css "/css/whuw-icomoon-style.css?v=1.0"))))
