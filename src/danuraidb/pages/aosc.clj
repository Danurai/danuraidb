(in-ns 'danuraidb.pages)

(def aosc-pretty-head
  (into pretty-head (h/include-css "/css/aosc-style.css?v=1.0")))
  
(defn aosc-navbar [req]
  (navbar 
    "/img/aosc/icons/quest_ability.png" 
    "AoSC DB" 
    "aosc"
    ["decks" "collection" "cards"]
    req
    :style "background-color: darkblue !important;"))
                  
(defn aosc-home [ req ]
  (h/html5
    aosc-pretty-head
    [:body
      (aosc-navbar req)
      [:body
        [:div.container.my-3
          [:div.row
            [:a {:href "/aosc/decks"} "Login"] 
            [:span.ml-1 "to see your decks"]]]]]))
            
(defn aosc-collection [ req ]
  (h/html5
    aosc-pretty-head
    [:body
      (aosc-navbar req)
      [:div.container-fluid.my-2
        [:div.row
          [:span#stats.mx-auto]
          [:div.btn-group.ml-auto
            [:a.btn.btn-warning {:title "Download Data" :href "/aosc/api/private/collection"} [:i.fas.fa-file-download]]
            [:btn.btn.btn-danger {:title "Upload Data" :data-target "#importmodal" :data-toggle "modal"} [:i.fas.fa-file-upload]]]]
        [:div.row-fluid.d-flex.justify-content-center.mb-1
          [:span.mr-2 [:input#filter.form-control.search-info {:placeholder "Filter"}]]
          (optgroup-togglenone model/aosc-alliances "alliance" "Order")
          [:span.ml-2]
          (optgroup-togglenone model/aosc-types "category" "Champion")
          [:form.ml-2 {:method "post" :action "/aosc/collection/save"}
            [:input {:name "filterjson" :hidden true}]
            [:input#collection {:name "collectionjson"
                                :hidden true 
                                :value (json/write-str (db/get-user-collection (-> req get-authentications (get :uid "1002"))))}]
            [:button#btnsave.btn.btn-warning {:type "submit" :disabled true} [:i.fas.fa-bookmark.mr-1] "Save"]]]
        [:div.row-fluid.mb-1
          [:div#cards.mx-1]]]
      [:div#updatemodal.modal {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h5.modal-title ""]
              [:button {:type "button" :class "close" :data-dismiss "modal"}
                [:span "x"]]]
            [:div.modal-body]
            [:div.modal-footer
              [:a.btn.btn-warning {:href "#" :target "_blank"} "Open Card Page"]
              [:button.btn.btn-primary {:data-dismiss "modal"} "Done"]]]]]
      [:div#importmodal.modal {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h5.modal-title "Load Collection"]
              [:button {:type "button" :class "close" :data-dismiss "modal"} 
                [:span "x"]]]
            [:div.modal-body
              [:textarea#inputcollection.form-control {:rows 5}]]
            [:div.modal-footer
              [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
              [:form {:method "post" :action "/aosc/collection/save"}
                [:input#filterjson {:type "text" :name "filterjson" :hidden true}]
                [:input#importcollection {:type "text" :name "collectionjson" :hidden true :value "{}"}]
                [:button.btn.btn-danger {:type "submit"} "Save Changes"]]]]]]
      (h/include-js "/js/aosc_tools.js?v=1")
      (h/include-js "/js/aosc_quickcollection.js?v=1")]))
      
(defn- aosc-cardimage [r]
  [:div.col-sm-3 
    [:div.row
      [:a {:href (str "/aosc/cards/" (:id r))}
        [:img.py-1.px-1.img-fluid.img-thumbnail {
          :src (str "/img/aosc/cards/" (->> r :skus (filter :default) first :id) ".jpg") 
          :title (:name r) 
          :alt   (:name r)}]]]])

(defn aosc-cards-page [req]
  (let [q (or (-> req :params :q) "")]
    (h/html5 
      aosc-pretty-head
      [:body  
        (aosc-navbar req)
        [:div.container.my-3
          [:div.row
            [:form.form-inline.my-2 {:action "/aosc/cards" :method "get"}
              [:div.row
                [:div.col
                  [:div.input-group
                    [:input.form-control.search-info {:type "text" :name "q" :value q :placeholder "Search"}]
                    [:div.input-group-append
                      [:button.btn.btn-primary {:type "submit"} "Search"]]]]]]]
          [:div.row
            (map (fn [r] (aosc-cardimage r)) (model/cardfilter q (model/aosc-get-cards)))]]])))

            

(defn aosc-card-page [req]
  (let [id (-> req :params :id)
        src (->> (model/aosc-get-cards) (filter #(= (-> % :id str) id)) first)
        owned (db/get-user-collection (-> req get-authentications :uid))]
    (h/html5
      aosc-pretty-head
      [:body
        (aosc-navbar req)
        [:div.container.my-3
          [:div.row.my-3
            [:span.h4.m-auto.border-bottom (:name src)]]
          [:div.row
            [:div.col-sm-8
              [:div.row
                [:table.table.table-sm.table-hover
                  [:tbody
                    [:tr [:td "Alliance"][:td.h5 [:span.badge.badge-secondary [:a.text-white {:href (str "/aosc/cards?q=a:" (:alliance src))} (:alliance src)]]]]
                    [:tr [:td "Category"][:td [:a {:href (str "/aosc/cards?q=c:" (-> src :category :en))} (-> src :category :en)]]]
                    [:tr [:td "Class"][:td [:a {:href (str "/aosc/cards?q=l:" (-> src :class :en))} (-> src :class :en)]]]
                    [:tr [:td "Collector Info"][:td (-> src :collectorInfo)]]
                    [:tr 
                      [:td "Collection"]
                      [:td (if (nil? (-> req get-authentications :uid))
                              [:span [:a {:href (str "/aosc/cardlogin/" id)} "Login"] " to see your collection info"]
                              (for [[k v] (owned (keyword id))] [:span.mr-2 (str (-> k name clojure.string/capitalize) ": " v)]))]]
                    [:tr [:td "Corners"]
                      [:td 
                        (for [corner (->> src :corners)]
                          [:span
                            {:class (str "popover-corner-bg popover-corner-" (-> src :category :en clojure.string/lower-case) (if (:smooth corner) "smooth" "clunky"))}
                            (case (:value corner)
                              ("Remove" "Heal" "Damage" "Spell" "Unit" "Ability") 
                                [:img.popover-corner-image {:src (str "/img/aosc/icons/quest_" 
                                               (-> corner :value clojure.string/lower-case) 
                                               (if (some? (:qualifier corner)) (str "_" (-> corner :qualifier clojure.string/lower-case)))
                                               ".png")}]
                              "o" ""
                              [:span.popover-corner-value (:value corner)])])]]
                    [:tr [:td "Tags"]
                      [:td 
                        (for [tag (:tags src)]
                          [:span.mr-2 
                            [:img.data-icon.mr-2 {:src (str "/img/aosc/icons/tag_" (clojure.string/lower-case tag) ".png")}]
                            [:a {:href (str "/aosc/cards?q=t:" tag)} (str tag)]])]]
                    [:tr [:td "Set"][:td [:a {:href (str "/aosc/cards?q=s:" (:setnumber src))} (str (-> src :set first :number) ": " (-> src :set  first :name))]]]
                    [:tr [:td "Rarity"][:td [:img.data-icon.mr-2 {:src (str "/img/aosc/icons/rarity_" (-> src :rarity clojure.string/lower-case) ".png")}]
                                          [:a {:href (str "/aosc/cards?q=r:" (:rarity src))} (:rarity src)]]]
                    [:tr [:td "Cost"][:td (:cost src)]]
                    [:tr [:td "Health"][:td (:healthMod src)]]
                    
                    [:tr [:td "Effect"][:td (-> src :effect :en markdown)]]
                    
                    [:tr 
                      [:td "Subject"]
                      [:td (if (some? (:subjectImage src)) 
                            [:img.subject-icon {:src (str "/img/aosc/icons/subject_" (:subjectImage src) ".png")}])]]
                  ; Additional info
                    [:tr [:td "id"][:td (:id src)]]
                  ]]]]
            [:div.col-sm-4
              [:img#cardimg.img-fluid {:src (str "/img/aosc/cards/" (->> src :skus (filter :image) first :id) ".jpg")}]
              (if (< 1 (->> src :skus (filter :image) count))
                [:div.d-flex.justify-content-around
                  (map-indexed (fn [id sku]
                    [:span [:a.altlink {:href "#" :data-id (:id sku)} (str "#" (inc id))]]) (->> src :skus (filter :image)))])]]]
        [:script "$('.altlink').on('click',function (evt) {evt.preventDefault(); $('#cardimg').attr('src','/img/aosc/cards/' + $(this).data('id') + '.jpg');});"]])))



(defn- get-aosc-deck-cards [deck]
  (map (fn [c] 
    (conj (->> (model/aosc-get-cards) (filter #(= (:id %) (:id c))) first) c)) 
    deck))

(defn- write-deck-list [deck]
  [:div
    (map (fn [category]
      [:div
        [:div.bold (str category " (" (->> deck (filter #(= (-> % :category :en) category)) (map :count) (reduce +)) ")")]
        [:div.text-muted
          (->> deck (filter #(= (-> % :category :en) category)) (sort-by :name) (map #(str (:count %) "x " (:name %))) (clojure.string/join ", "))]])
      ["Blessing" "Unit" "Spell" "Ability"])])
      
(defn- deck-export-string [deck]
  (clojure.string/join "\n"
    (apply concat 
      (map (fn [category] 
        (concat [category] 
                (->> deck 
                    (filter #(= (-> % :category :en) category)) 
                    (map #(str (:count %) "x " (:name %))))))
        ["Champion" "Blessing" "Unit" "Spell" "Ability"]))))
    
          
(defn- aosc-deck-card [d]
  (let [deck-cards (get-aosc-deck-cards (-> d :data model/parsedeck :cards))]
    [:li.list-group-item.list-deck-card
      [:div.d-flex.justify-content-between {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} 
        [:div
          [:div.h3.mr-2 (:name d)]
          [:div
            [:div.bold "Champion"]
            [:div.text-muted
              (clojure.string/join ", " 
                (reduce concat (map (fn [c]
                    (repeat (:count c) (str (:name c) " (" (:healthMod c) "/" (:cost c) ")"))
                    ) (->> deck-cards (filter #(= (-> % :category :en) "Champion")) (sort-by :name)))))]]]
        [:div.bold.text-center.cardcounts
          [:div
            [:img.deck-icon.mr-1 {:src "/img/aosc/icons/quest_unit.png"}]
            [:span.mr-1 (->> deck-cards (filter #(= (-> % :category :en) "Unit")) (map :count) (reduce +) )]]
          [:div
            [:img.deck-icon.mr-1 {:src "/img/aosc/icons/quest_spell.png"}]
            [:span.mr-1 (->> deck-cards (filter #(= (-> % :category :en) "Spell")) (map :count) (reduce +) )]]
          [:div 
            [:img.deck-icon.mr-1 {:src "/img/aosc/icons/quest_ability.png"}]
            [:span.mr-1 (->> deck-cards (filter #(= (-> % :category :en) "Ability")) (map :count) (reduce +) )]]]]
      [:div.collapse.mb-2 {:id (str "deck_" (:uid d))} 
        (write-deck-list deck-cards)
        [:div
          [:button.btn.btn-sm.btn-danger.mr-1 {:data-toggle "modal" :data-target "#deletemodal" :data-name (:name d) :data-uid (:uid d)} [:i.fas.fa-times.mr-1] "Delete"]
          [:button.btn.btn-sm.btn-success.mr-1 {:data-toggle "modal" :data-target "#exportdeck" :data-export (deck-export-string deck-cards) :data-deckname (:name d)} [:i.fas.fa-file-export.mr-1] "Export"]
          [:a.btn.btn-sm.btn-dark.mr-1 {:href (str "warhammer-tcg://share-deck?deckCode=" (:data d) "&deepLinkTimestamp=" (tc/to-long (time/now)))} [:i.fas.fa-gamepad.mr-1] "Champions"]
          [:a.btn.btn-sm.btn-primary {:href (str "/aosc/decks/edit/" (:uid d))} [:i.fas.fa-edit.mr-1] "Edit"]]]]))  
          
(defn aosc-decks [req]
  (let [decks (db/get-user-decks 1 (-> req get-authentications (get :uid 1002)))]
    (h/html5
      aosc-pretty-head
      [:body
        (aosc-navbar req)
        [:div.container.my-3
          [:div.row.justify-content-between
            [:div.h3 (str "Decks (" (count decks) ")")]
            [:div 
              [:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
              [:a.btn.btn-primary {:href "/aosc/decks/new" :title "New Deck"} [:i.fas.fa-plus]]]]
          [:div.row
            [:div#decklists.w-100
              [:ul.list-group
                (map (fn [d] (aosc-deck-card d)) decks)]]]]
        [:div#deletemodal.modal {:tabindex -1 :role "dialog"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h5.modal-title "Confirm Delete"]
                [:button {:type "button" :class "close" :data-dismiss "modal"} 
                  [:span "x"]]]
              [:div.modal-body]
              [:div.modal-footer
                [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
                [:form {:action "/aosc/decks/delete" :method "post"}
                  [:input#deletemodaldeckuid {:name "deletedeckuid" :hidden true}]
                  [:button.btn.btn-danger {:submit "true"} "OK"]]]]]]
        [:div#importdeck.modal {:role "dialog"}
          [:div.modal-dialog.modal-lg {:role "document"}
            [:div.modal-content
              [:div.modal-header "Load Deck"
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                  [:span {:aria-hidden "true"} "x"]]]
              [:div.modal-body
                [:div.container-fluid
                  [:div.row
                    [:div "Paste Decklist or Sharing Code below"]
                    [:textarea#importdecklist.form-control {:rows "10"}]]]]
              [:div.modal-footer
                [:form {:action "/aosc/decks/new" :method "post"}
                  [:input#deckcode {:hidden true :name "id"}]
                  [:button.btn.btn-primary {:type "submit"} "Load Deck"]]
                [:button.btn.btn-secondary {:type "button" :data-dismiss "modal"} "Close"]]]]]
          [:div#exportdeck.modal {:role "dialog"}
            [:div.modal-dialog {:role "document"}
              [:div.modal-content
                [:div.modal-header "Export Deck:" [:span.ml-1]
                  [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                    [:span {:aria-hidden "true"} "x"]]]
                [:div.modal-body 
                  [:textarea.form-control.mb-2 {:rows "10"}]
                  [:div.input-group {:title "Copy sharing code to clipboard" :style "cursor: pointer;"}
                    [:input#sharecode.form-control {:type "input" :style "cursor: pointer;"}]
                    [:div.input-group-append
                      [:span.input-group-text [:i.fas.fa-clipboard]]]]]
                [:div.modal-footer
                  [:button.btn.btn-secondary {:type "button" :data-dismiss "modal"} "Close"]]]]]
          [:div#toaster {:style "position: fixed; top: 10px; right: 10px; z-index: 1050"}]
        (h/include-js "/js/externs/warhammer-deck-sharing.js")
        (h/include-js "/js/aosc_decklist.js")])))                  
          
(defn aosc-newdeck [req]
  (h/html5
    aosc-pretty-head
    [:body
      (aosc-navbar req)
      [:div.container.my-3
        [:div.row-fluid
          [:div.h2.text-center "Choose Alliance"]]
        [:div.row
          [:div.col-sm-6
            [:div.row
              [:div.h3.mx-auto "Order"]]
            [:div.row.mb-3
              [:a.btn.mx-auto.newdeck.newdeck-order {:href "/aosc/decks/new/Order"}]]
            [:div.row
              [:div.h3.mx-auto "Chaos"]]
            [:div.row.mb-3
              [:a.btn.mx-auto.newdeck.newdeck-chaos {:href "/aosc/decks/new/Chaos"}]]]
          [:div.col-sm-6
            [:div.row
              [:div.h3.mx-auto "Death"]]
            [:div.row.mb-3
              [:a.btn.mx-auto.newdeck.newdeck-death {:href "/aosc/decks/new/Death"}]]
            [:div.row
              [:div.h3.mx-auto "Destruction"]]
            [:div.row.mb-3
              [:a.btn.mx-auto.newdeck.newdeck-destruction {:href "/aosc/decks/new/Destruction"}]]]]]]))
              
(defn- decknamecolour [ uid ]
  (let [rgb (->> (re-matcher #"(?i)([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})" uid) re-find rest)]
    (if (> 0.5 (/ (reduce + (map #(* %1 (Integer/parseInt %2 16))[0.299 0.587 0.114] rgb)) 255))
       "white"
       "black")))
       
(defn- decknameinput [ deckdata ]
  [:input#deckname.form-control {
    :name "deckname" :placeholder "deck name" :value (:name deckdata)
    :style (str "background-color: #" (:uid deckdata) "; color: " (decknamecolour (:uid deckdata)) ";") }])
    
(defn aosc-deckbuilder [req]
  (let [deckdata (model/get-deck-data req)]
    (h/html5
      aosc-pretty-head
      [:body
        (aosc-navbar req)
        [:div.container
          [:div.row.my-2
            [:div.col-md-6
              [:div.sticky-top.pt-2
                [:div.row 
                  [:form.form.w-100 {:action "/aosc/decks/save" :method "post"}
                    [:div.row.py-1.mr-1
                      [:div.col-sm-8 (decknameinput deckdata)]
                      [:div.col-sm-4
                        [:input#deckdata {:name "deckdata" :hidden true :value (:data deckdata)}]
                        [:input#deckuid {:name "deckuid" :hidden true :value (:uid deckdata)}]
                        [:input#deckalliance {:name "deckalliance" :hidden true :value (:alliance deckdata)}]
                        [:input#decknotes {:name "decknotes" :hidden true :value (:notes deckdata)}]
                        [:button.btn.btn-warning.float-right {:submit "true"} [:i.fas.fa-bookmark.mr-1] "Save"]]]]]
                [:div#decklist.row]]]
            [:div.col-md-6
              [:ul.nav.nav-tabs.nav-fill
                [:li.nav-item [:a.nav-link.active {:href "#buildtab" :data-toggle "tab" :role "tab"} "Build"]]
                [:li.nav-item [:a.nav-link {:href "#notestab" :data-toggle "tab" :role "tab"} "Notes"]]
                [:li.nav-item [:a.nav-link {:href "#checktab" :data-toggle "tab" :role "tab"} "Check"]]]
              [:div.tab-content
                [:div#buildtab.tab-pane.fade.show.active.mt-2 {:role "tabpanel"}
                  [:div.row.px-3.justify-content-between
                    (btngroup model/aosc-types "filter_type")
                    (optgroup (remove #(= "Any" (:name %)) model/aosc-alliances) "filter_alliance" (:alliance deckdata)) ;  [:span.float-sm-right
                    [:div.btn-group.btn-group-toggle.mb-1 {:data-toggle "buttons"}
                      [:label.btn.btn-outline-secondary {:title "Toggle owned cards"}
                        [:input#lock {:type "checkbox"} [:i.fas.fa-unlock]]]]]
                  [:div.d-flex.my-1
                    [:input#filtertext.form-control.search-info {:type "text" :placeholder "Search"}]
                    [:select#selecttrait.selectpicker.ml-2 {:multiple true :data-width "fit" :data-multiple-separator "" :data-none-selected-text "Tags"}
                      (for [t model/aosc-traits]
                        (let [imgtag (str "<img class=\"trait-icon ml-1\" src=\"/img/aosc/icons/tag_" (clojure.string/lower-case t) ".png\" title=\"" t "\" />")]
                          ^{:key (gensym)}[:option {:data-content imgtag} t]))]]
                  [:div.d-flex.my-1
                    [:table#cardtbl.table.table-hover.table-sm
                      [:thead
                        [:tr
                          [:th.text-center {:scope "col" :title "Quantity"} "Qty."]
                          [:th.sortable {:scope "col" :data-field "name"} "Name"]
                          [:th.sortable.text-center {:scope "col" :data-field "type" :title "Type"} "Ty."]
                          [:th.text-center {:scope "col" :title "Traits"} "Traits"]
                          [:th.sortable.text-center {:scope "col" :data-field "healthMod" :title "Health"} "H"]
                          [:th.sortable.text-center {:scope "col" :data-field "cost" :title "Cost"} "C"]
                          ]]
                     [:tbody#tblbody]]]]
                [:div#notestab.tab-pane.fade {:role "tabpanel"}
                  [:div.row-fluid.mt-2 [:h3 "Deck Notes"]]
                  [:div.row-fluid.text-muted.mb-2 [:a {:href "https://github.com/showdownjs/showdown/wiki/Showdown's-Markdown-syntax" :target "_blank"} "Use showdown's markdown syntax"]]
                  [:div.row-fluid.mb-1 [:textarea#usernotes.form-control {:type "text" :rows 10} (:notes deckdata)]]
                  [:div.row-fluid [:label#notesmarkdown.p-3.w-100 {:style "background-color: lightgrey;"}]]]
                [:div#checktab.tab-pane.fade {:role "tabpanel"}]]]]]
      [:div#cardmodal.modal {:role "dialog" :tabindex -1}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header]
            [:div.modal-body]]]]
      (h/include-js "/js/externs/typeahead.js")
      (h/include-js "/js/externs/warhammer-deck-sharing.js")
      (h/include-js "/js/aosc_deckbuilder.js?v=1.100")
      (h/include-js "/js/aosc_tools.js?v=1.000")])))