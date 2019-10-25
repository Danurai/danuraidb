(ns danuraidb.lotrsoloview
  (:require
    [reagent.core :as r]
    [danuraidb.lotrsolomodel :refer 
      [ad data init! startsolo! selectscenario! movecard!
       shuffle-edeck! draw-card! select-deck! select-card! set-counter!]]))


(defn get-cards-by-location [ deck location ]
  (filter #(= (:loc %) location) deck))       
 
(defn scenarioselect []
  (let [ec (->> @data :cards (filter #(= (:encounter_name %) (-> @ad :scenario :name))))
        qc (->> ec (filter #(= (:type_code %) "quest")) (sort-by :position) first)]
    [:div.row
      [:div.col-sm-3
        [:div.h5 "Select Scenario"]
        [:select.form-control {:size 10 :value (or (-> @ad :scenario :name) "null")}
          (for [s (:scenarios @data)]
            ^{:key (gensym)}[:option {:on-click #(selectscenario! s)} (:name s)])]]
      [:div.col-sm-9
        [:div.d-flex.mb-2
          [:div.h5 (:name qc)]
          [:button.btn.btn-primary.ml-auto {:on-click #(startsolo!)} "Start"]]
        [:div.row
          [:div.col-sm-3
            [:img {:src (:cgdbimgurl qc)}]] 
          [:div.col-sm-4
            [:div.mb-2 (:text qc)]
            [:cite (:flavor qc)]]
          [:div.col-sm-5
            [:ul#edeck.list-group.mb-2 {:style {:maxHeight "300px" :overflow-y "scroll"}}
              (for [c (:edeck @ad)]
                ^{:key (gensym)}[:li.list-group-item
                  [:div.d-flex
                    [:span (:name c)]
                    [:div.btn-group.btn-group-sm.ml-auto {:data-toggle "buttons"}
                      (for [loc ["deck" "stage" "aside"]]
                        ^{:key (gensym)}[:button.btn.btn-outline-dark {
                          :class (if (= (:loc c) (keyword loc)) "active") 
                          :on-click #(movecard! ad (:id c) (keyword loc))}
                          (-> loc (subs 0 2) clojure.string/capitalize) ])]]])]
            [:div.row
              [:div.col-sm-6
                [:div.text-center [:b "Staged"]]
                (for [c (get-cards-by-location (:edeck @ad) :stage)]
                  ^{:key (gensym)}[:div (:name c)])]
              [:div.col-sm-6
                [:div.text-center [:b "Aside"]]
                (for [c (get-cards-by-location (:edeck @ad) :aside)]
                  ^{:key (gensym)}[:div (:name c)])]]]]]]))
    
  
(defn exhaust-selected! []
    nil)
    
(defn discard-selected! []
  nil)
    
   
(def commandbuttons (r/atom [
  {:active true :title "Shuffle" :fn #(shuffle-edeck!)}
  {:active true :title "Draw"   :fn #(draw-card!)}
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
        :class (if (false? (:active itm)) "disabled")
        } 
        (:title itm)])])

    
(defn card-component [ c ]
  ^{:key (gensym)}[:div.small-card {      ;.card-link
    :data-code (:code c)
    :class (if (contains? (:selected @ad) (:id c)) "selected")
    :on-click (fn [e] (select-card! e (:id c))) }
    [:img.img-fluid {:src (:cgdbimgurl c)}]
    (if (= (:type_code c) "location") 
      [:div.counter.counter-prg 
        [:span.text-center (if (>= (:progress c 0) (:quest_points c)) "Y" (:progress c))]])
    (if (= (:type_code c) "enemy") [:div.counter.counter-dmg [:span.text-center.my-auto (if (>= (:damage c 0) (:health c)) "X" (:damage c))]])])
    
(defn hero-card-component [ c ]
  ^{:key (gensym)}[:div.small-hero {
    :data-code (:code c)
    :class (if (contains? (:selected @ad) (:id c)) "selected")
    :on-click (fn [e] (select-card! e (:id c)))
    :style {:background-image (str "URL(" (:cgdbimgurl c) ")")}
    }
    [:span.counter.counter-res [:span (:resource c)]]
    [:span {:style {:position "absolute" :bottom "2px" :right "2px"}}
      [:img {:style {:width "35px"} :src (str "/img/lotrdb/icons/sphere_" (:sphere_code c) ".png")}]]])
  
  
  
(defn setup []
  [:div 
    [commandbar]
    [:div.row {:on-click #(swap! ad assoc :selected #{})}
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
        [:div.col-sm-6
          [:div.d-flex.justify-content-center.mb-2
            [:span.mr-2 "Staging"]
            [:i.lotr-type-threat.mr-1]
            [:span (->> @ad :edeck (filter #(= (:loc %) :stage)) (map :threat) (apply +))]]
          [:div.d-flex
            (doall (for [c staging]
              (card-component c)))]])
      
      [:div.col-sm-4
        [:div.d-flex [:span.mx-auto "P1"]]
        [:div.d-flex
          (doall (for [h (get-cards-by-location (:p1deck @ad) :hero)]
             (hero-card-component h)))]
    ; Deck and discard      
        [:div.d-flex
          [:div.small-card {
            :class (if (contains? (:selected @ad) :p1deck) "selected")
            :on-click (fn [e] (.stopPropagation e) (select-deck! :p1deck))}
            [:img.img-fluid {:src "/img/lotrdb/player_back.jpg"}]
            [:div.counter.counter-count [:span (->> @ad :p1deck (filter #(= (:loc %) :deck)) count)]]]
          [:div.small-card
            [:img.img-fluid {:style {:opacity 0.4} :src "/img/lotrdb/player_back.jpg"}]
            [:div.counter.counter-count (count [])]]]]
    ]])
              
(defn Page []
  (init!)
  (fn []
    [:div.container-fluid.my-3
      (case (:screen @ad)
        :setup (setup)
        (scenarioselect))
      [:button.btn.btn-dark {:on-click #(reset! ad {:screen :scenarioselect})} "Reset!"]
      [:div (-> @ad :selected str)]
      ;[:div (->> @ad :edeck (map :id) str)]
      ;[:div (->> @ad :edeck (filter #(= (:loc %) :stage)) (map #(select-keys % [:id :name :loc :damage :progress])) str)]
      [:div (->> @ad :p1deck (map :resource))]
      ]))