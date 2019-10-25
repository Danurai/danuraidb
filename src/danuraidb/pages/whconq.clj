'(in-ns danuraidb.pages)

(def whconq-pretty-head
  (into pretty-head (h/include-css "/css/whconq/style.css?v=1.0")))
  
(defn whconq-navbar [req]
  (navbar 
    "/img/whconq/icons/skull_white.png" 
    "WH40k DB" 
    "whconq"
    ["decks" "cards" "collection"]
    req
    :style "background-color: teal !important;"))
                  
(defn whconq-home [ req ]
  (h/html5
    whconq-pretty-head
    [:body
      (whconq-navbar req)
      [:body
        [:div.container.my-3
          [:div.row
            [:a {:href "/whconq/decks"} "Login"] 
            [:span.ml-1 "to see your decks"]]]]]))

; SEARCH PAGES ;

(defn whconq-cards [ req ]
  (h/html5
    whconq-pretty-head
    [:body  
      (whconq-navbar req)
      [:div.container.my-2
        [:div.col-md-6
          [:div.row-fluid
            (for [cycle model/whconq-cycle-data]
              [:div
                [:a {:href (str "/whconq/cycle/" (:position cycle))} (:name cycle)]
                [:ol
                  (for [pack (->> model/whconq-pack-data (remove #(= (:code %) (:code cycle))) (filter #(= (:cycle_code %) (:code cycle))))]
                    [:li [:a {:href (str "/whconq/pack/" (:code pack))} (:name pack)]])]])]]]]))
                    
(defn whconq-findcards [q]
  (h/html5
    whconq-pretty-head
    [:body
      (whconq-navbar nil)
      [:div.container.my-2
        [:div.row
          [:form.form-inline.my-2 {:action "/whconq/find" :method "get"}
            [:div.input-group
              [:input.form-control.search-info {:type "text" :name "q" :value q :placeholder "Search"}]
              [:div.input-group-append
                [:button.btn.btn-primary {:type "submit"} "Search"]]]]]
        [:div.row
          [:table.table.table-sm.table-hover
            [:thead [:tr [:td "Name"][:td "Faction"][:td "Type"][:td "Cost"][:td [:i.fas.fa-cog]][:td "Set"]]]
            [:tbody
              (map (fn [r]
                [:tr
                  [:td [:a.card-tooltip {:href (str "/whconq/card/" (:code r)) :data-code (:code r)} (:name r)]]
                  [:td (:faction r)]
                  [:td (:type r)]
                  [:td (:cost r)]
                  [:td {:title (:signature_loyal r)} (case (:signature_loyal r) "Signature" [:i.fas.fa-cog.icon-sig] "Loyal" [:i.fas-fa-crosshairs.icon-loyal] "")]
                  [:td (str (:pack r) " #" (-> r :position Integer.))]
                 ])
                (model/cardfilter q model/whconq-card-data :whconq))]]]]
      (h/include-js "/js/whconq/whconq_cards.js?v=1.0")
      (h/include-js "/js/whconq/whconq_popover.js?v=1.0")]))
      
(defn whconq-cardpage [code]
  (h/html5 
    whconq-pretty-head
    [:body
      (whconq-navbar nil)
      [:div.container.my-2
        ((fn [r]
          (let [code-card (Integer/parseInt (:code r))
               card-next (model/whconq-card-from-code (format "%06d" (inc code-card)))
               card-prev (model/whconq-card-from-code (format "%06d" (dec code-card)))]
            [:div.col
              [:div.row-fluid.d-flex.justify-content-between.my-3
                [:span
                  [:a.btn.btn-outline-secondary.card-tooltip {:href (str "/whconq/card/" (:code card-prev)) :data-code (:code card-prev) :hidden (nil? card-prev)} (:name card-prev)]]
                [:span 
                  [:a.btn.btn-outline-secondary {:href (str "/whconq/pack/" (:pack_code r))} (:pack r)]]
                [:span 
                  [:a.btn.btn-outline-secondary.card-tooltip {:href (str "/whconq/card/" (:code card-next)) :data-code (:code card-next) :hidden (nil? card-next)} (:name card-next)]]]
              [:div.row
                [:div.col-sm
                  [:div.card  
                    [:div.card-header [:h2 (if (:unique r) [:img.unique-icon.mr-1 {:src "/img/whconq/icons/skull.png"}]) (:name r)]]
                    [:div.card-body (:text r)]
                    [:div.card-footer.text-muted.d-flex.justify-content-between
                      [:span (:faction r)]
                      [:span (str (:pack r) " #" (-> r :position Integer.))]]]]
                [:div.col-sm
                  [:img {:src (:img r) :alt (:name r)}]]]
              ; Signature Squad cards/links
                (if (some? (:signature_squad r))
                  [:div.mb-2
                    [:div.h3 "Signature Squad"]
                    [:div.row.d-flex.justify-content-between.my-2
                      (map (fn [s]
                        [:div
                          [:a.btn.btn-outline-secondary.card-tooltip {:data-code (:code s) :href (str "/whconq/card/" (:code s))} 
                            (str (:name s) (if (not= (:type_code s) "warlord_unit") (str " x" (:quantity s))))]]
                      ) (->> model/whconq-card-data (filter #(= (:signature_squad %) (:signature_squad r))) (sort-by :code)))]])
            ])) (model/whconq-card-from-code code))]
      (h/include-js "/js/whconq/whconq_cards.js?v=1.0")
      (h/include-js "/js/whconq/whconq_popover.js?v=1.0")]))

; COLLECTION

(defn whconq-collection [ req ]
  (h/html5
    whconq-pretty-head
    [:body
      (whconq-navbar req)
      [:div.container
        [:div.row.my-2
          [:div.col-md-4
            [:h2 "Packs"]
            [:div#packlist]]
          [:div.col-md-8
            [:div.row.justify-content-between
              [:h2 "Virtual Folders"]
              [:div.form-check
                [:input#showimg.form-check-input {:type "checkbox"}]
                [:label.form-check-label "Show Card Images"]]]
            [:div#foldersections.row.justify-content-between]
            [:div#folderpager.row.justify-content-between]
            [:div#folderpages.row.justify-content-between
              [:span.chaos-loader]]]]]
      (h/include-css "/css/whconq/folderstyle.css")
      (h/include-js "/js/whconq/whconq_folders.js")
      (h/include-js "/js/whconq/whconq_popover.js")]))
      
      
; DECKLIST      

(defn- deck-card-list-by-type [type_code cards-in-deck]
  (let [cid-by-type (filter #(= (:type_code %) type_code) cards-in-deck)]
    [:div.decklist-section
      [:div [:b (str (-> cid-by-type first :type) " (" (->> cid-by-type (map :qty) (reduce +)) ")")]]
      (map (fn [r] 
            [:div (str (:qty r) "x ")
              [:a.card-tooltip {:href (str "/card/" (:code r)) :data-code (:code r) :class (:faction_code r)} (if (:unique r) [:i.fas.fa-skull.fa-xs.mr-1]) (:name r)]
              (if (= (:signature_loyal r) "Signature") [:i.fa.fa-sm.fa-cog.ml-1.icon-sig])
              (if (= (:signature_loyal r) "Loyal") [:i.fa.fa.sm.fa-cog.ml-1.icon-loyal])
            ]) cid-by-type)]))

(defn- code-to-name [ c ]
  (-> c
      (clojure.string/replace "_" " ")
      clojure.string/capitalize))
            
(defn- whconq-export-string [ d deck-cards ]
  (let [warlord    (->> deck-cards (filter #(= (:type_code %) "warlord_unit")) first)]
    (clojure.string/join "\n"
      (reduce concat
        [(:name d)
         (str "\nTotal Cards: " (->> deck-cards (filter #(not= (:type_code %) "warlord_unit")) count))
         "\nWarlord"
         (str (:name warlord) " (" (:pack warlord) ")")]
        (mapv (fn [tc]
          (reduce conj 
            [(str "\n" (code-to-name tc))]
            (mapv #(str (:qty %) "x " (:name %) " (" (:pack %) ")") (filter #(= (:type_code %) tc) deck-cards)))
          ) ["army_unit" "attachment" "event" "support"])))))
            
          
(defn- whconq-deck-card [d card-data]
  (let [deck-cards (map (fn [[k v]] (assoc (first (filter #(= (:code %) k) card-data)) :qty v)) (json/read-str (:data d)))
        warlord    (->> deck-cards (filter #(= (:type_code %) "warlord_unit")) first)]
    [:li.list-group-item.list-deck-card
      [:div.px-3.py-1 {:data-toggle "collapse" :href (str "#" "deck_" (:uid d))} 
        [:div.row
          [:div.col-sm-9
            [:div.h4.mt-2 (:name d)]
            [:div.mt-2 [:span.h5.mr-2 (:name warlord)] [:span.text-muted (last (re-find #"^<b>(.+?)<\/b>" (:text warlord)))]]
            [:div
              (map (fn [x] [:a.badge.badge-secondary.text-light.mr-1 x]) (re-seq #"\w+" (:tags d)))]]
          [:div.col-sm-3.d-none.d-sm-block
            [:div.warlord-thumb.ml-auto.border.border-secondary.rounded {:style (str "background-image: url(" (:img warlord) ");")}]]]]
      [:div.collapse.mb-2 {:id (str "deck_" (:uid d))}   
        [:div.text-muted.mb-2
          (str "Cards " (->> deck-cards (filter #(not= "warlord_unit" (:type_code %))) (map :qty) (reduce +)) "/50")]
        [:div.mb-2.decklist
          (map #(deck-card-list-by-type % deck-cards) ["army_unit" "attachment" "event" "support"])]
        [:div.mb-2
          [:div.small.col-sm-12.text-muted (str "Created on " (-> d :created tc/from-long))]
          [:div.small.col-sm-12.text-muted (str "Updated on " (-> d :updated tc/from-long))]]
        [:div
          [:button.btn.btn-sm.btn-danger.mr-1 {:data-toggle "modal" :data-target "#deletemodal" :data-name (:name d) :data-uid (:uid d)} [:i.fas.fa-times.mr-1] "Delete"]
          [:button.btn.btn-sm.btn-success.mr-1 {:data-toggle "modal" :data-target "#exportdeck" :data-export (whconq-export-string d deck-cards) :data-deckname (:name d)} [:i.fas.fa-file-export.mr-1] "Export"]
          [:a.btn.btn-sm.btn-primary {:href (str "/whconq/decks/edit/" (:uid d))} [:i.fas.fa-edit.mr-1] "Edit"]]]]))  
          
(defn whconq-decks [req]
  (let [decks (db/get-user-decks 3 (-> req get-authentications :uid))
        card-data model/whconq-card-data]
    (h/html5
      whconq-pretty-head
      [:body
        (whconq-navbar req)
        [:div.container.my-3
          [:div.d-flex.justify-content-between
            [:div.h3 (str "Army Roster (" (count decks) ")")]
            [:div 
              ;[:button#exportall.btn.btn-secondary.mr-1 {:title "Export to JSON" :data-export (json/write-str (map #(select-keys % [:name :data]) decks))} [:i.fas.fa-clipboard]]
              ;[:button#importall.btn.btn-secondary.mr-1 {:title "Import from JSON" :data-toggle "modal" :data-target "#importallmodal"} [:i.fas.fa-paste]] 
              [:button.btn.btn-warning.mr-1 {:data-toggle "modal" :data-target "#importdeck" :title "Import"} [:i.fas.fa-file-import]]
              [:a.btn.btn-primary {:href "/whconq/decks/new" :title "New Deck"} [:i.fas.fa-plus]]]]
          [:div.d-flex
            [:div#decklists.w-100
              [:ul.list-group
                (map (fn [d] (whconq-deck-card d card-data)) decks)]]]]
        (deletemodal)
        (importallmodal)
        (importdeckmodal)
        (exportdeckmodal)
        (toaster)
        (h/include-js "/js/whconq/whconq_decklist.js?v=1.0")])))
        
; DECKBUILDER

;; NEW DECK
(defn whconq-newdeck [req]
  (h/html5
    whconq-pretty-head
    [:body
      (whconq-navbar req)
      [:div.container.my-2
        [:div.row
          [:div.col-sm-4
            [:div.card
              [:div.card-header "Choose your Warlord"]
              [:div.list-group
                (map (fn [f]
                  (for [x (->> model/whconq-card-data (sort-by :code) (filter #(and (= (:faction_code %) f) (= (:type_code %) "warlord_unit"))))]
                    [:a.list-group-item {:href (str "/whconq/decks/new/" (:code x)) :data-code (:code x)} 
                      (model/whconq-icon-svg (:faction_code x)) (:name x)]))
                  (->> model/whconq-card-data (sort-by :code) (map :faction_code) distinct))]]]
          [:div.col-sm-4.d-none.d-sm-block
            [:div#warlord.sticky-top.pt-1]]
          [:div.col-sm-4.d-none.d-sm-block
            [:div#signaturesquad.sticky-top.pt-1]]]]
      (h/include-js "/js/whconq/whconq_cards.js?v=1.0")
      (h/include-js "/js/whconq/whconq_new_deck.js?v=1.0")]))
      
(defn- check-deck []
  [:div.row
    [:div.col-md
    ;; Sample Draw
      [:div.row.justify-content-between.border-bottom.border-primary.mb-2
        [:i.fas.fa-play]
        [:a {:href "#sampledraw" :data-toggle "collapse"} "Sample Draw"]]
      [:div#sampledraw.collapse.show
        [:div.row.justify-content-center.my-2
          [:div.btn-group
            [:button.btn.btn-sm.btn-light {:type "button" :disabled "true"} "Draw:"]
            [:button#draw1.btn.btn-sm.btn-light.btn-draw {:type "button" :val "1"} "1"]
            [:button#draw1.btn.btn-sm.btn-light.btn-draw {:type "button" :val "2"} "2"]
            [:button#draw1.btn.btn-sm.btn-light.btn-draw {:type "button" :val "7"} "7"]
            [:button#draw1.btn.btn-sm.btn-light.btn-draw {:type "button" :val "all"} "All"]
            [:button#draw1.btn.btn-sm.btn-light.btn-draw {:type "button" :val "0"} "Reset"]]]
        [:div#hand]]
    ;; Charts
      [:div.row.justify-content-between.border-bottom.border-primary.mb-2
        [:i.fas.fa-chart-bar]
        [:a {:href "#charts" :data-toggle "collapse"} "Charts"]]
      [:div#charts.collapse
        [:div.row
          [:div.col-sm-6
            [:canvas#pieFact {:width "400" :height "400"}]]
          [:div.col-sm-6
            [:canvas#pieType {:width "400" :height "400"}]]]
        [:div.row
          [:div.col-sm-6
            [:canvas#pieCommand {:width "400" :height "400"}]]
          [:div.col-sm-6
            [:canvas#pieShield {:width "400" :height "400"}]]]
        [:div.row
          [:canvas#lineCost {:width "400" :height "300"}]]]
    ;; Planets
      [:div.row.justify-content-between.border-bottom.border-primary.mb-2
        [:i.fas.fa-globe]
        [:a {:href "#planets" :data-toggle "collapse"} "Planets"]]
      [:div#planets.collapse]]])

(defn whconq-deckbuilder [req]
  (let [deck (model/get-deck-data req)]
    (h/html5
      whconq-pretty-head
      [:body
        (whconq-navbar req)
        [:div#cardmodal.modal {:role "dialog" :tabindex -1}]
        [:div.container
          [:div.alert.alert-dismissible.fade.show {:role "alert"}]
          [:div.row.my-2
        ;; DECKLIST
            [:div.col-md-6
              [:div#decklist.row-fluid.my-1 
                [:div.h5 "Empty Deck"]]
              [:div.row-fluid.my-1.border-dark.border-top
                [:form#save_form.form.needs-validation {:method "post" :action "/decks/save" :role "form" :novalidate true}
                ;;(POST "/save" [id name system data alliance tags notes]
                  [:input#deck-id      {:type "text" :name "id"      :value (:uid deck) :readonly true :hidden true}]
                  [:input#deck-system  {:type "text" :name "system"   :value "3" :readonly true :hidden true}]
                  [:input#deck-content {:type "text" :name "data"     :value (:data deck)  :readonly true :hidden true}]
                  [:input#deck-alliance {:type "text" :name "alliance" :value (:alliance deck) :readonly true :hidden true}]
                  [:input#deck-tags    {:type "text" :name "tags"    :value (:tags deck) :readonly true :hidden true}]
                  [:input#deck-notes   {:type "text" :name "notes"   :value (:notes deck) :readonly true :hidden true}]
                  [:div.form-group
                    [:label {:for "#deck-name" :required true} "Army Name"]
                    [:input#deck-name.form-control {:type "text" :name "name" :placeholder "New Deck" :required true :value (:name deck)}]
                    [:div.invalid-feedback "You must name your Army"]]
                  [:button.btn.btn-warning.mr-2 {:role "submit"} "Save"]
                  [:a.btn.btn-light.mr-2 {:href "/whconq/decks"} "Cancel Edits"]]]]
        ;; OPTIONS
            [:div.col-md-6
              [:ul.nav.nav-tabs.nav-fill  
                [:li.nav-item [:a.nav-link.active {:data-toggle "tab" :href "#deckbuild"} "Build"]]
                [:li.nav-item [:a.nav-link {:data-toggle "tab" :href "#decknotes"} "Notes"]]
                [:li.nav-item [:a.nav-link {:data-toggle "tab" :href "#deckcheck"} "Check"]]
                [:li.nav-item [:a.nav-link {:data-toggle "tab" :href "#decksets"} "Sets"]]]
              [:div.tab-content.my-2
            ;; BUILD
                [:div#deckbuild.tab-pane.active {:role "tabpanel"}
                  [:div.row.my-1
                    [:div.col
                      [:div#factionfilter.btn-toolbar {:role "toolbar"}
                        [:div.btn-group-sm.btn-group.btn-group-toggle.mr-1.my-1 {:data-toggle "buttons"}
                          (for [fname ["Space Marines" "Astra Militarum" "Orks" "Chaos" "Dark Eldar" "Eldar" "Tau" "Neutral"]
                                :let [fcode (-> fname clojure.string/lower-case (clojure.string/replace #"\s" "_"))]]
                            [:label.btn.btn-outline-secondary {:title fname :name fcode} 
                              [:input {:type "checkbox" :autocomplete "off" :name fcode} 
                                (if (= fname "Neutral") [:i.fa.fa-plus] (model/whconq-icon-svg fcode))]])]
                        [:div.btn-group-sm.btn-group.btn-group-toggle.mr-1.my-1 {:data-toggle "buttons"}  
                          [:label.btn.btn-outline-secondary {:title "Tyranids"} 
                            [:input {:type "checkbox" :autocomplete "off" :name "tyranids"} (model/whconq-icon-svg "tyranids")]]
                          [:label.btn.btn-outline-secondary {:title "Necrons"} 
                            [:input {:type "checkbox" :autocomplete "off" :name "necrons"} (model/whconq-icon-svg "necrons")]]]]
                      [:div#typefilter.btn-group.btn-group-sm.btn-group-toggle.mr-auto.my-1 {:data-toggle "buttons"}
                        [:label.btn.btn-outline-secondary {:title "Warlord"} 
                          [:input {:type "checkbox" :autocomplete "off" :name "warlord_unit"} [:i.fas.fa-user-circle]]]
                        [:label.btn.btn-outline-secondary.active {:title "Army"}
                          [:input {:type "checkbox" :autocomplete "off" :name "army_unit" :checked true} [:i.fas.fa-users]]]
                        [:label.btn.btn-outline-secondary {:title "Attachment"} 
                          [:input {:type "checkbox" :autocomplete "off" :name "attachment"} [:i.fas.fa-user-plus]]]
                        [:label.btn.btn-outline-secondary {:title "Event"} 
                          [:input {:type "checkbox" :autocomplete "off" :name "event"} [:i.fas.fa-bolt]]]
                        [:label.btn.btn-outline-secondary {:title "Support"} 
                          [:input {:type "checkbox" :autocomplete "off" :name "support"} [:i.fas.fa-building]]]  ;fa-hands-helping]]]
                        [:label.btn.btn-outline-secondary {:title "Synapse"} 
                          [:input {:type "checkbox" :autocomplete "off" :name "synapse_unit"} [:i.fas.fa-dna]]]]]]
                  [:div.row
                    [:div.col-md  
                      [:input#filterlist.form-control.my-1 {:type "text" :placeholder "Filter Results (or search for card)" :title "* name\nx: text\ne: set code\nf: faction code\nr? cost\ns? shields\nc? command icons\na? attack\nh? hp\nu:true|false unique\nl:true|false loyal\n\n? operators : > < !"}]]]
                  [:div.row
                    [:div.col
                      [:table#cardtbl.table.table-hover.table-sm
                        [:thead.thead-dark
                          [:tr
                            [:td "Quantity"]
                            [:td.sortable {:title "Name" :data-field "name"} "Name"]
                            [:td.sortable {:title "Type" :data-field "type"} "Type"]
                            [:td.sortable {:title "Faction" :data-field "faction"} "Fac."]
                            [:td.sortable {:title "Cost" :data-field "cost"} [:i.fas.fa-cog]]
                            [:td.sortable {:title "Command" :data-field "command_icons"} [:i.fas.fa-gavel]]
                            [:td.sortable {:title "Shields" :data-field "shields"} [:i.fas.fa-shield-alt]]
                            [:td.sortable {:title "Attack" :data-field "attack"} [:i.fas.fa-bolt]]
                            [:td.sortable {:title "HP" :data-field "hp"} [:i.fas.fa-heartbeat]]]]
                        [:tbody#tablebody]]]]]
            ;; NOTES
                [:div#decknotes.tab-pane {:role "tabpanel"}
                  [:div.row
                    [:div.col
                      [:div.form-group
                        [:label {:for "#tags"} "Tags"]
                        [:input#tags.form-control {:type "text"}]]
                      [:div.form-group
                        [:label {:for "#notes"} "Notes"]
                        [:textarea#notes.form-control {:rows 15}]]
                      [:div.card.bg-light.text-muted
                        [:div#notes-preview.card-body
                        [:span "Preview. Showdown markup syntax "
                        [:a {:href "https://github.com/showdownjs/showdown/wiki/Showdown's-Markdown-syntax" :target "_blank"} "here"]]]]]]]
            ;; CHECK      
                [:div#deckcheck.tab-pane {:role "tabpanel"}        
                  (check-deck)]
            ;; SETS
                [:div#decksets.tab-pane {:role "tabpanel"}
                  [:div.row 
                    [:div#setlist.ml-3]]]]]]]
        [:div.border-top.border-dark.bg-secondary.text-light.px-3.pb-5 "Information"]
      
      (h/include-css "/css/whconq/deckstyle.css")
      (h/include-js "https://cdnjs.cloudflare.com/ajax/libs/showdown/1.8.6/showdown.min.js")  ; Markdown Converter
      (h/include-js "https://cdnjs.cloudflare.com/ajax/libs/moment.js/2.22.2/moment.min.js")
      (h/include-js "https://cdnjs.cloudflare.com/ajax/libs/Chart.js/2.7.2/Chart.bundle.min.js")
      (h/include-js "/js/externs/typeahead.js")
      (h/include-js "/js/externs/chartjs-plugin-labels.min.js")
      (h/include-js "/js/whconq/whconq_tools.js")
      (h/include-js "/js/whconq/whconq_popover.js")
      (h/include-js "/js/whconq/whconq_deckbuilder.js")])))