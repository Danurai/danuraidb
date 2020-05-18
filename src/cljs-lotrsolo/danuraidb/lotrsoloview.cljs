(ns danuraidb.lotrsoloview
  (:require
    [reagent.core :as r]
    [danuraidb.lotrsolomodel :refer 
      [ad data init! startsolo! selectscenario! selectpdeck! movecard!
       shuffle-deck! draw-cards! select-deck! select-card! set-counter!]]))


(defn get-cards-by-location [ deck location ]
  (filter #(= (:loc %) location) deck))       
 
(defn scenariosetup []
  (let [ec (->> @data :cards (filter #(= (:encounter_name %) (-> @ad :scenario :name))))
        qc (->> ec (filter #(= (:type_code %) "quest")) (sort-by :position) first)]
    [:div.row
      [:div.col-sm-3
        [:div.h5 "Select Scenario"]
        [:select.form-control {:size 10 :value (or (-> @ad :scenario :name) "null") :on-change #() }
          (for [s (:scenarios @data)]
            ^{:key (gensym)}[:option {:on-click #(selectscenario! s)} (:name s)])]]
      [:div.col-sm-3 
        [:div.h5 "Select Deck"]
        [:select.form-control {:size 10 :value (or (-> @ad :pdeck :name) "null") :on-change #()}
          (for [d (:pdecks @data)]
            ^{:key (gensym)}[:option {:on-click #(selectpdeck! d)} (:name d)])]]
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
              [:div {:key (gensym)} crd])]]]]))
    
  
(defn exhaust-selected! []
    nil)
    
(defn discard-selected! []
  nil)
  
(defn showcards! []
  (if (contains? #{:edeck :p1deck} (-> @ad :selected first))
    (swap! ad assoc-in [:temp :state] :showcards)))
   
(def commandbuttons (r/atom [
  {:active true :title "show" :fn #(showcards!)}
  {:active true :title "Shuffle" :fn #(shuffle-deck!)}
  {:active true :title "Draw"   :fn #(draw-cards! 1)}
  {:active true :title "Draw 6"   :fn #(draw-cards! 6)}
  {:active true :title "Exhaust" :fn #(exhaust-selected!)}
  {:active true :title "Discard" :fn #(discard-selected!)}
  {:active true :title "+ Dmg"  :fn #(set-counter! :damage inc)}
  {:active true :title "- Dmg"  :fn #(set-counter! :damage dec)}
  {:active true :title "+ Prog" :fn #(set-counter! :progress inc)}
  {:active true :title "- Prog" :fn #(set-counter! :progress dec)}
  {:active true :title "+ Res"  :fn #(set-counter! :resource inc)}
  {:active true :title "- Res"  :fn #(set-counter! :resource dec)}
  ]))
    
(defn commandbar []
  [:div#commandbar
    (for [itm @commandbuttons]
      ^{:key (gensym)}[:button.btn.btn-dark.mr-1 {
        :on-click (:fn itm)
        :class (if (false? (:active itm)) "disabled")} 
        (:title itm)])])

    
(defn card-component [ c ]
  ^{:key (gensym)}[:div.small-card {      ;.card-link
    :data-code (:code c)
    :class (if (contains? (:selected @ad) (:id c)) "selected")
    :on-click (fn [e] (select-card! e (:id c))) }
    [:img.img-fluid {:src (:cgdbimgurl c)}]
    (if (= (:type_code c) "location") 
      [:div.counter.counter-prg 
        [:span.text-center (if (>= (:progress c 0) (:quest_points c 0)) "Y" (:progress c 0))]])
    (if (= (:type_code c) "enemy") [:div.counter.counter-dmg [:span.text-center.my-auto (if (>= (:damage c 0) (:health c)) "X" (:damage c 0))]])])
    
(defn hero-card-component [ c ]
  ^{:key (gensym)}[:div.small-hero {
    :title (:id c) 
    :data-code (:code c)
    :class (if (contains? (:selected @ad) (:id c)) "selected")
    :on-click (fn [e] (select-card! e (:id c)))
    :style {:background-image (str "URL(" (:cgdbimgurl c) ")")}}
    [:span.counter.counter-res [:span (:resource c)]]
    [:span {:style {:position "absolute" :bottom "2px" :right "2px"}}
      [:img {:style {:width "35px"} :src (str "/img/lotrdb/icons/sphere_" (:sphere_code c) ".png")}]]])
  
  
  
(defn setup []
  [:div ;{:on-click #(swap! ad assoc :selected #{})}
    [commandbar]
    [:div.row
      [:div.col-10
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
                 (hero-card-component h)))
              (doall (for [h (get-cards-by-location (:p1deck @ad) :area)]
                 (card-component h)))]]]]
      [:div.col-2
        [:div.border {:style {:height "100%"}}]]]])
    
(defn toggle-select-shown [ id ]
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
            (doall (for [c (filter #(= :deck (:loc %)) (get @ad (-> @ad :selected first))) :let [selected (-> @ad :temp :selected)]]
              [:img.small-card {
                :key (gensym)
                :class (if (contains? selected (:id c)) "selected") 
                :title (:id c) 
                :src (:cgdbimgurl c) 
                :on-click #(toggle-select-shown (:id c))}]))]]
        [:div.modal-footer
          [:button.btn.btn-secondary {:on-click (fn [e] (-> @ad :temp :selected draw-cards!) (close-modal) ) } "Stage"]
          [:button.btn.btn-secondary {:on-click #(close-modal)} "Aside"]
          [:button.btn.btn-primary {:on-click #(close-modal)} "Close"]
          [:button.btn.btn-success {:on-click (fn [e] (close-modal)(shuffle-deck!) )} "Shuffle and Close"]]]]])
              
(defn Page []
  (init!)
  (fn []
    [:div.container-fluid.my-3
      (if (= (-> @ad :temp :state) :showcards) [show-select-modal])
      (case (:screen @ad)
        :setup (setup)
        (scenariosetup))
  ; Debug
      [:button.btn.btn-dark {:on-click #(reset! ad {:screen :scenarioselect})} "Reset!"]
      [:div (-> @ad :selected str)]
      [:div (->> @ad :edeck (map :loc) str)]
      ;[:div (->> @ad :edeck (filter #(= (:loc %) :stage)) (map #(select-keys % [:id :name :loc :damage :progress])) str)]
      ;[:div (->> @ad :p1deck (map :resource))]
      [:div (-> @ad (dissoc :pdeck :p1deck :edeck :qdeck :scenario) str)]
      
      ]))