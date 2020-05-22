(ns danuraidb.lotrsoloview
  (:require
    [reagent.core :as r]
    [danuraidb.lotrsolomodel :refer 
      [ad data init! startsolo! get-cards-by-location
       select-scenario-by-name! select-pdeck-by-name! 
        shuffle-deck! draw-cards! select-deck! select-card! set-counter!]]))


 
(defn scenariosetup []
  [:div.container.my-3
    [:div.row
      [:div.col-sm-3
        [:div.h5 "Select Scenario"]
        [:select.form-control {:size 10 :value (or (-> @ad :scenario :name) "null") 
          :on-change #(select-scenario-by-name! (-> % .-target .-value))}
          (for [s (:scenarios @data)] [:option {:key (gensym)} (:name s)])]]
      [:div.col-sm-3 
        [:div.h5 "Select Deck"]
        [:select.form-control {
          :size 10 :value (or (-> @ad :pdeck :name) "null") 
          :on-change #(select-pdeck-by-name! (-> % .-target .-value))}
          (for [d (:pdecks @data)] [:option {:key (gensym)} (:name d)])]]
      [:div.col-sm-6
        [:div.row
          [:div.col.d-flex
            [:button.btn.btn-primary.ml-auto {:on-click #(startsolo!)} "Start"]]]
        [:div.row
          [:div.col-sm-6
            [:h4 (-> @ad :scenario :name)] 
            (for [crd (->> @ad :edeck (map :name))]
              [:div {:key (gensym)} crd])]
          [:div.col-sm-6
            [:h4 (-> @ad :pdeck :name)]
            (for [crd (->> @ad :p1deck (map :name))]
              [:div {:key (gensym)} crd])]]]]])
    
  
(defn exhaust-selected! []
  nil)
    
(defn discard-selected! []
  (doseq [deck [:edeck :p1deck]]
    (swap! ad assoc deck 
      (map #(if (contains? (:selected @ad) (:id %))
                (assoc % :loc :discard)
                %) (deck @ad)))))
                
(defn showcards! []
  (when (contains? #{:edeck :p1deck :ediscard :p1discard} (-> @ad :selected first))
    (swap! ad assoc-in [:temp :state] :showcards)
    (swap! ad assoc-in [:temp :loc] (keyword (re-find #"deck|discard" (-> @ad :selected first name))))
    ))
    
(defn show-card! [ c ]
  (swap! ad assoc :showcard c))
   
(def commandbuttons (r/atom [
  {:active true :title "Show"    :fn #(showcards!)}
  {:active true :title "Shuffle" :fn #(shuffle-deck!)}
  {:active true :title "Draw"    :fn #(draw-cards! 1)}
  {:active true :title "Draw 6"  :fn #(draw-cards! 6)}
  {:active true :title "Aside"   :fn #(draw-cards! (-> @ad :selected) :aside)}
  {:active true :title "Exhaust" :fn #(exhaust-selected!)}
  {:active true :title "Discard" :fn #(discard-selected!)}
  
  {:active true :title "+ Dmg"   :fn #(set-counter! :damage inc)}
  {:active true :title "- Dmg"   :fn #(set-counter! :damage dec)}
  {:active true :title "+ Prog"  :fn #(set-counter! :progress inc)}
  {:active true :title "- Prog"  :fn #(set-counter! :progress dec)}
  {:active true :title "+ Res"   :fn #(set-counter! :resource inc)}
  {:active true :title "- Res"   :fn #(set-counter! :resource dec)}
  ]))
    
(defn commandbar []
  [:div#commandbar
    (for [itm @commandbuttons]
      ^{:key (gensym)}[:button.btn.btn-dark.mr-1 {
        :on-click (:fn itm)
        :class (if (false? (:active itm)) "disabled")} 
        (:title itm)])])

    
(defn card-component [ c ]
  [:div.small-card {      ;.card-link
    :key (gensym)
    :data-code (:code c)
    :class (if (contains? (:selected @ad) (:id c)) "selected")
    :on-click (fn [e] (select-card! e (:id c))) }
    [:img.img-fluid {:src (:cgdbimgurl c) :on-mouse-over #(show-card! c)  :on-mouse-out #(swap! ad dissoc :showcard)}]
    (case (:type_code c)
      "location" 
        [:div.counter.counter-prg 
          [:span.text-center (if (>= (:progress c 0) (:quest_points c 0)) "Y" (:progress c 0))]]
      ("enemy" "ally")
        [:div.counter.counter-dmg 
          [:span.text-center (if (>= (:damage c 0) (:health c)) "X" (:damage c 0))]]
      "hero"
        [:span
          [:div.counter.counter-dmg 
            [:span.text-center (if (>= (:damage c 0) (:health c)) "X" (:damage c 0))]]
          [:div.counter.counter-res 
            [:span.text-center (:resource c 0)]]]
      nil)])  
  
(defn- deck [ key deck location image & params ]
    [:div.small-card {
      :id key
      :class (if (contains? (:selected @ad) key) "selected")
      :on-click (fn [e] (.stopPropagation e) (select-deck! key))}
      [:img.img-fluid {:style (first params) :src (str "/img/lotrdb/" image)}]
      [:div.counter.counter-count [:span (count (get-cards-by-location deck location))]]])
  
  
(defn game []
  [:div.container-fluid.my-3
    [commandbar]
    [:div.row
      [:div.col-9
        [:div.row 
          [:div.col
            [:div.d-flex
              [:div [:span.lotr-type-threat.mr-2] [:span (->> (get-cards-by-location :edeck :stage) (map :threat) (apply +))]]]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "ENEMY"]
            [:div.d-flex
              (deck :edeck :edeck :deck "encounter_back.jpg" )
              (deck :ediscard :edeck :discard "encounter_back.jpg" {:opacity 0.4})
              [:div.mr-2]
              (doall (for [c (reverse (get-cards-by-location :edeck :aside))] (card-component c)))]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "STAGING"]
            [:div.d-flex (doall (for [c (get-cards-by-location :edeck :stage)] (card-component c)))]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "ENGAGED"]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "PLAYER"]
            [:div.d-flex (doall (for [c (get-cards-by-location :p1deck :hero)] (card-component c)))]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "HAND"]
            [:div.d-flex
              (deck :p1deck :p1deck :deck "player_back.jpg" )
              (deck :p1discard :p1deck :discard "player_back.jpg" {:opacity 0.4})
              [:div.mr-2]
              (doall (for [c (reverse (get-cards-by-location :p1deck :hand))] (card-component c)))]]]]
      [:div.col-3
          [:div (if (:showcard @ad) [:img.img-fluid {:src (-> @ad :showcard :cgdbimgurl)}])]
          [:div.border {:style {:height "100%"}}
            (doall (for [l (:log @ad)] [:div {:key (gensym)} l]))]]]])
  
(defn setup []
  [:div.container-fluid.my-3
    [:div.col
      [commandbar]
      [:div.row
        [:div.col-9
          (let [aside (->> @ad :edeck (filter #(= (:loc %) :aside)))] ;(get-cards-by-location (:edeck @ad) :aside)]
            [:div.row
              [:div.col
                [:div "Aside"]
                [:div.d-flex
                  (doall (for [c aside]
                    (card-component c)))]]])
          [:div.row 
        ; Deck and discard      
            (let [deck (get-cards-by-location (:edeck @ad) :deck)
                  discard (get-cards-by-location (:edeck @ad) :discard)]
              [:div.col-sm-2
                [:div.d-flex
                  [:span.mx-auto.mb-2 "Encounter Deck"]]
                [:div.d-flex
                  [:div.small-card {
                    :class (if (contains? (:selected @ad) :edeck) "selected")
                    :on-click (fn [e] (.stopPropagation e) (select-deck! :edeck))}
                    [:img.img-fluid {:src "/img/lotrdb/encounter_back.jpg"}]
                    [:div.counter.counter-count [:span (count deck)]]]
                  [:div.small-card
                      [:img.img-fluid {:style {:opacity 0.4} :src "/img/lotrdb/encounter_back.jpg"}]
                      [:div.counter.counter-count (count discard)]]]])
        ; Staging
            (let [staging (get-cards-by-location (:edeck @ad) :stage)]
              [:div.col-sm-9
                [:div.d-flex.justify-content-center.mb-2
                  [:span.mr-2 "Staging"]
                  [:i.lotr-type-threat.mr-1]
                  [:span (->> @ad :edeck (filter #(= (:loc %) :stage)) (map :threat) (apply +))]]
                [:div.d-flex
                  (doall (for [c staging]
                    (card-component c)))]])]
          [:div.row {:style {:min-height "50px"}}]  
          [:div.row 
            [:div.col-sm-2
              [:div.d-flex
          ; Deck and discard 
                [:div.small-card {
                  :class (if (contains? (:selected @ad) :p1deck) "selected")
                  :on-click (fn [e] (.stopPropagation e) (select-deck! :p1deck))}
                  [:img.img-fluid {:src "/img/lotrdb/player_back.jpg"}]
                  [:div.counter.counter-count [:span (->> @ad :p1deck (filter #(= (:loc %) :deck)) count)]]]
                [:div.small-card
                  [:img.img-fluid {:style {:opacity 0.4} :src "/img/lotrdb/player_back.jpg"}]
                  [:div.counter.counter-count (count [])]]]]
        ; Cards
            [:div.col-sm-9
              [:div.d-flex
                (doall (for [h (get-cards-by-location (:p1deck @ad) :hero)]
                   (card-component h)))
                   ;(hero-card-component h)))
                (doall (for [h (get-cards-by-location (:p1deck @ad) :area)]
                   (card-component h)))]]]]
        [:div.col-3
          [:div {:style {:width "100%" :height "60%"}} (if (:showcard @ad) [:img.img-fluid {:src (-> @ad :showcard :cgdbimgurl)}])]
          [:div.border {:style {:height "100%"}}]]]]])
        
; MODAL ;
    
(defn- toggle-select-shown [ id ]
  (if (nil? (-> @ad :temp :selected))
      (swap! ad assoc-in [:temp :selected] #{}))
  (if (contains? (-> @ad :temp :selected) id)
      (swap! ad update-in [:temp :selected] disj id)
      (swap! ad update-in [:temp :selected] conj id)))
       
(defn- close-modal []
  (swap! ad dissoc :temp))
               
(defn- show-select-modal []
  [:div.modal {
    :style {:display "block" :background-color "rgba(0,0,0,0.3)" :overflow-x "hidden" :overflow-y "auto"} 
    :on-click #(close-modal)}
    [:div.modal-dialog.modal-lg {:on-click (fn [e] (.stopPropagation e) nil)}
      [:div.modal-content
        [:div.modal-body
          [:div.row-fluid 
            (doall (for [c (filter #(= (-> @ad :temp :loc) (:loc %)) (get @ad (if (contains? #{:edeck :ediscard} (-> @ad :selected first)) :edeck :p1deck))) :let [selected (-> @ad :temp :selected)]]
              [:img.small-card {
                :key (gensym)
                :class (if (contains? selected (:id c)) "selected") 
                :title (:id c) 
                :src (:cgdbimgurl c) 
                :on-click #(toggle-select-shown (:id c))}]))]]
        [:div.modal-footer
          [:button.btn.btn-secondary {:on-click (fn [e] (-> @ad :temp :selected draw-cards!)  (swap! ad update :temp dissoc :selected)) } "Stage"]
          [:button.btn.btn-secondary {:on-click (fn [e] (-> @ad :temp :selected (draw-cards! :aside)) (swap! ad update :temp dissoc :selected))} "Aside"]
          [:button.btn.btn-primary {:on-click #(close-modal)} "Close"]
          [:button.btn.btn-success {:on-click (fn [e] (close-modal)(shuffle-deck!) )} "Shuffle and Close"]]]]])
          
(defn- debug [] 
  [:div.container
      [:button.btn.btn-dark {:on-click #((reset! ad {:selected #{}}) (init!))} "Reset!"]
      [:div (-> @ad :selected str)]
      [:div (->> @ad :edeck (map :loc) str)]
      ;[:div (->> @ad :edeck (filter #(= (:loc %) :stage)) (map #(select-keys % [:id :name :loc :damage :progress])) str)]
      ;[:div (->> @ad :p1deck (map :resource))]
      [:div (-> @ad (dissoc :pdeck :p1deck :edeck :qdeck :scenario) str)]])
              
(defn Page []
  (init!)
  (fn []
    [:div 
      (case (:screen @ad)
        :game (game)
        (scenariosetup))
      (if (= (-> @ad :temp :state) :showcards) [show-select-modal])
      [debug]]))