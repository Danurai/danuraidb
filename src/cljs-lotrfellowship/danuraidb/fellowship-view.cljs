(ns danuraidb.fellowship-view
  (:require 
    [reagent.core :as r]
    [danuraidb.fellowship-model :as m]))
    
(defn cardcount [ decklist sc ]
  (->> decklist
       (filter (fn [[k v]] (contains? (set (map :code sc)) k)))
       vals
       (apply +)))
    
(defn- pretty-deck [ d deck cards ]
  [:div.py-1.px-2
    [:div.d-flex
      (for [h (filter #(= (:type_code %) "hero") cards)]
        ^{:key (gensym)}
        [:a.card-link {
          :data-code (:code h) 
          :href (str "/lotrdb/cards/" (:code h)) 
          :data-toggle "modal" 
          :data-target "#cardmodal"
          :on-click #(m/set-modal-card! h )}
          [:div.deckhero {:style {:background-image (str "url(" (:cgdbimgurl h) ")") :position "relative"}}
            [:span.herosphere ;{:class (str "lotr-type-" (:sphere_code h))}
              [:img.img-fluid {:src (str "/img/lotrdb/icons/sphere_" (:sphere_code h) ".png")}]
              ]]])]
    [:div.decklist
      (for [tc ["ally" "attachment" "event" "player-side-quest"] 
            :let [sectioncards (filter #(= (:type_code %) tc) cards)]]
        (if (< 0 (count sectioncards))
          ^{:key (gensym)}[:div.decklist-section
            [:div.font-weight-bold (str (-> sectioncards first :type_name) " (" (cardcount (:data deck) sectioncards)  ")")]
            (for [sc (sort-by :name sectioncards)]
              ^{:key (gensym)}[:div (str (get (:data deck) (:code sc)) "x ")
                [:a.card-link {:data-target "#cardmodal" :data-toggle "modal" :data-code (:code sc) :href (str "/lotrdb/cards/" (:code sc))}
                  (:name sc)]])]))]])
    
(defn- deck-list [ d ]
  (let [deck (d @m/appdata)
       cards (->> @m/appdata :cards (filter #(contains? (-> deck :data keys set) (:code %)))) ]
    [:div
      [:div.d-flex.mb-2
        [:label.mr-1.my-auto "Name:"]
        [:input.form-control.mr-1 {
          :type "text" 
          :on-change #(m/set-deck-name! d (-> % .-target .-value)) 
          :value (-> @m/appdata d :name)}]
        [:button.btn.btn-warning.mr-1 {:class (if (:saved? deck) "disabled")} [:i.fas.fa-feather]]
        [:button.btn.btn-danger {
          :data-toggle "modal"
          :data-target "#confirmmodal"
          :on-click (fn []
            (swap! m/appdata 
              assoc :modal {
                :msg (str "Are you sure you want to remove deck " (get (d @m/appdata) :name "(untitled)") "?")
                :fn #(swap! m/appdata dissoc d)}))}
          [:i.fas.fa-minus]]]
      [:ul.nav.nav-tabs.nav-fill {:role "tablist"}
        [:li.nav-item [:a.nav-link.active {:data-toggle "tab" :href (str "#" (name d) "deck") :role "tab"} "Deck"]]
        [:li.nav-item [:a.nav-link {:data-toggle "tab" :href (str "#" (name d) "notes") :role "tab"} "Notes"]]]
      [:div.tab-content
        [:div.tab-pane.fade.show.active {:id (str (name d) "deck") :role "tabpanel"}
          (pretty-deck d deck cards)]
        [:div.tab-pane.fade {:id (str (name d) "notes") :role "tabpanel"}
          [:span {:style {:word-wrap "break-word"}} (str deck)]]]]))
  
(defn- select-deck [ d ]
  [:div.btn-group.btn-group-sm.float-right
    [:button.btn.btn-primary {:title "New" :on-click #(swap! m/appdata assoc d {})} [:i.fas.fa-plus]]
    [:button.btn.btn-secondary {:title "Load" :on-click #(swap! m/appdata assoc d :load)} [:i.fas.fa-upload]]])
      
(defn- load-deck [ d ]
  [:div
    [:ul.list-group.mb-2
      (doall (for [sd (->> @m/appdata :decks (filter #(not= (:uid %) (if (= :d1 d) (-> @m/appdata :d2 :uid) (-> @m/appdata :d1 :uid)))))]; (filter #(not= (-> @m/appdata (if (= d :d1) :d2 :d1) :code) (:code %))))]
        ^{:key (gensym)}[:li.list-group-item.px-3.py-2 
          [:div.d-flex.justify-content-between
            [:span.my-auto (:name sd)]
            [:button.btn.btn-sm.btn-outline-secondary {:on-click #(m/load-deck! d (assoc sd :saved? true))} "+"]]]))]
    [:div [:button.btn.btn-outline-secondary.btn-sm.float-right {:on-click #(swap! m/appdata dissoc d)} "Cancel"]]])
        
(defn- deck [ d ]
  [:div.col-sm-6.mb-2
    [:small.text-muted (str "Deck #" (-> d name second))]
    (cond
      (nil? (d @m/appdata))    (select-deck d)
      (= :load (d @m/appdata)) (load-deck d)
      :else (deck-list d)
      )])
    
(defn- fellowship-row []
  [:div.row.mb-2
    [:div.col-sm-12
      [:div.d-flex
        [:label.mr-1.my-auto "Fellowship Name:"]
        [:input.mr-1.form-control.w-50 {
          :type "text"
          :value (:fname @m/appdata)
          :on-change #(swap! m/appdata assoc :fname (-> % .-target .-value))}]
        [:button.btn.btn-warning {:title "Save" :on-click (m/save-fellowship!)} 
          [:i.fas.fa-feather]
          [:span.ml-1.d-none.d-sm-inline "Save"]]]]])
          
          
(defn- collection []
  [:div
    [:div.h5 "Collection"]
    [:div.list-group
      [:li.list-group-item.px-2.py-2 
        [:div.d-flex.justify-content-between
          [:span "Core Sets"]
          [:div.btn-group.btn-group-sm
            (doall (for [n (range 1 4)]
              ^{:key (gensym)}[:button.btn.btn-outline-dark {
                :value n 
                :class (if (= (:corecount @m/appdata) n) "active")
                :on-click (fn [] 
                  (swap! m/appdata assoc :corecount n)
                  (m/set-item! "lotrcore_owned" n))} n]))]]]]])
    
;; TABLE

(defn- get-max [ c ]
  (if (= (:type_code c) "hero")
    1
    (min 3 (* (:quantity c) (-> @m/appdata :corecount)))
    ))
(defn- card-buttons [ d decklist c ]
  (let [indeck (get decklist (:code c) 0)
       maxav (get-max c)]
    [:div.btn-group.btn-group-xs
      (for [n (range (inc maxav))]
        ^{:key (gensym)}[:button.btn.btn-outline-dark {
          :value n 
          :class (if (= indeck n) "active")
          :on-click #(m/add-to-deck! d (:code c) n)
          } n])]))
          
(defn- card-table [card-list]
  (let [decklist1 (-> @m/appdata :d1 :data)  decklist2 (-> @m/appdata :d2 :data)]
    [:table#cardtbl.table.table-sm
      [:thead
        [:tr
          [:th "Deck #1"]
          [:th "Name"]
          [:th.d-table-cell.d-sm-none.text-center {:title "Type"} "T."]
          [:th.d-none.d-md-table-cell.text-center "Type"]
          [:th.d-table-cell.d-sm-none.text-center {:title "Sphere"} "S."]
          [:th.d-none.d-md-table-cell.text-center "Sphere"]
          [:th.d-none.d-sm-table-cell.text-center {:title "Cost/Threat"} "C."]
          [:th.d-none.d-sm-table-cell.text-center {:title "Attack"} "A."]
          [:th.d-none.d-sm-table-cell.text-center {:title "Defense"} "D."]
          [:th.d-none.d-sm-table-cell.text-center {:title "Willpower"} "W."]
          [:th.text-right "Deck #2"]]]
      [:tbody
        (doall (for [c card-list]
          ^{:key (gensym)}[:tr
            [:td (card-buttons :d1 decklist1 c)]
            [:td [:a.card-link {
              :href (str "/lotrdb/cards/" (:code c))
              :on-click #(m/set-modal-card! c)
              :data-code (:code c)
              :data-toggle "modal"
              :data-target "#cardmodal"}
              (if (:is_unique c) [:span.mr-1.lotr-type-unique])
              [:span (:name c)]]]
            [:td.d-table-cell.d-sm-none.text-center (if (:type_name c) (subs (:type_name c) 0 2))]
            [:td.d-none.d-md-table-cell.text-center (:type_name c)]
            [:td.text-center [:span {:class (str "lotr-type-" (:sphere_code c))}]]
            [:td.d-none.d-sm-table-cell.text-center (or (:cost c) (:threat c))]
            [:td.d-none.d-sm-table-cell.text-center (:attack c)]
            [:td.d-none.d-sm-table-cell.text-center (:defense c)]
            [:td.d-none.d-sm-table-cell.text-center (:willpower c)]
            [:td.text-right (card-buttons :d2 decklist2 c)]]))
        ]]))
        
(defn- card-table-filter-type []
  [:div.btn-group.btn-group-sm
    (doall (for [tc (->> @m/appdata :cards (map :type_code) distinct sort)]
      ^{:key (gensym)}
      [:button.btn.btn-outline-dark {
        :class (if (contains? (-> @m/appdata :filter :type_code) tc) "active")
        :on-click #(m/update-filter! :type_code tc)}
        tc]))])
        
(defn- card-row []
  (let [card-list (m/filtered-cards)]
    [:div.row.mb-1
      [:div.col-sm-12
        [:ul.nav.nav-tabs.nav-fill {:role "tablist"}
          [:li.nav-item [:a.nav-link.active {:data-toggle "tab" :role "tab" :href "#cardlist"} "Cards"]]
          [:li.nav-item [:a.nav-link {:data-toggle "tab" :role "tab" :href "#collection"} "Collection"]]]
        [:div.tab-content
          [:div#cardlist.tab-pane.fade.show.active.my-2
            [:div 
              [:div "FILTER"]
              [card-table-filter-type]
              ;[:div (count (m/filtered-cards))]
              ;[:div (-> @m/appdata :filter str)]
              ]
            [:div "TYPEAHEAD"]
            [:small.text-muted.float-right (str "Shown: " (count card-list) "/" (-> @m/appdata :cards count))]
            (card-table card-list)]
          [:div#collection.tab-pane.fade.my-2
            [collection]]]
      ]]))
      
(defn- card-modal []
  (let [c (-> @m/appdata :modal)
       decklist1 (-> @m/appdata :d1 :data)
       decklist2 (-> @m/appdata :d2 :data)]
    [:div#cardmodal.modal {:role "dialog" :tab-index -1}
      [:div.modal-dialog {:role "document"}
        [:div.modal-content
          [:div.modal-header
            [:div.modal-title
              [:h4 (if (:is_unique c) [:span.lotr-type-unique.mr-1]) (:name c)]]
            [:button.close {:data-dismiss "modal" :type "button"} [:i.fas.fa-times]]]
          [:div.modal-body
            [:div.row
              [:div.col
                [:img.img-fluid.center {:src (:cgdbimgurl c)}]]
              [:div.col
                [:div.my-1 "Deck #1"]
                (card-buttons :d1 decklist1 c)
                [:div.my-1 "Deck #2"]
                (card-buttons :d2 decklist2 c)
                [:div.text-muted.my-1 (str (:pack_name c) " #" (:position c))]
                [:div.text-muted.my-1 (str "Owned: " 
                  (* (:quantity c) (if (= (:pack_code c) "Core") (-> @m/appdata :corecount) 1)))]]]]
        ]]]))
        
(defn- confirm-modal []
  (let [c (-> @m/appdata :modal)]
    [:div#confirmmodal.modal {:role "dialog" :tab-index -1}
      [:div.modal-dialog.modal-sm {:role "document"}
        [:div.modal-content
          [:div.modal-header
            [:div.modal-title
              [:h4 "Confirm"]]
            [:button.close {:data-dismiss "modal" :type "button"} [:i.fas.fa-times]]]
          [:div.modal-body
            [:div (:msg c)]]
          [:div.modal-footer
            [:button.btn.btn-secondary {:data-dismiss "modal"} "Close"]
            [:button.btn.btn-warning {:data-dismiss "modal" :on-click (:fn c)} "Confirm"]]
        ]]]))
            
(defn page []
  (m/init!)
  (fn []
    [:div.container-fluid.my-3 
      [fellowship-row]
      [:div.row
        (deck :d1)
        (deck :d2)]
      [:div 
        [:div [:small (str "cards: " (-> @m/appdata :cards count) "\ndecks: " (-> @m/appdata :decks count))]]
        [:button.btn.btn-sm.btn-outline-dark {:on-click #(swap! m/appdata dissoc :d1 :d2 :modal)} "Reset"]
        ;[:small (-> @m/appdata :modal str)]
        ]
      [card-row]
      [card-modal]
      [confirm-modal]
    ]))