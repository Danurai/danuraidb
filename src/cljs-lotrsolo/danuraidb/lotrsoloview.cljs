(ns danuraidb.lotrsoloview
  (:require
    [reagent.core :as r]
    [danuraidb.lotrsolomodel :refer 
      [ad data showcards!
       init! startsolo! 
       get-card-back get-cards-by-location 
       toggle-status! update-threat! update-quest-stage! get-deck-key
       select-scenario-by-name! select-pdeck-by-name! start-round! mulligan! toggle-debug! show-card!
       shuffle-deck! move-cards! draw-cards! select-deck! select-card! set-counter!]]))
        
(def commandbuttons [
  {:active? true :op :show    :title "Show"     :fn #(showcards!)}
  {:active? true :op :shuffle :title "Shuffle"  :fn #(shuffle-deck!)}
  {:active? true :op :draw    :title "Draw"     :fn #(draw-cards! 1)}
  {:active? true :op :draw6   :title "Draw 6"   :fn #(draw-cards! 6)}
  {:active? true :op :stage   :title "Stage"    :fn #(move-cards! (-> @ad :selected) :stage)}
  {:active? true :op :aside   :title "Aside"    :fn #(move-cards! (-> @ad :selected) :aside)}
  {:active? true :op :engage  :title "Engage"   :fn #(move-cards! (-> @ad :selected) :engaged)}
  {:active? true :op :active  :title "Active"   :fn #(move-cards! (-> @ad :selected) :active)}
  {:active? true :op :play    :title "Play"     :fn #(move-cards! (-> @ad :selected) :play)}
  {:active? true :op :discard :title ">Discard" :fn #(move-cards! (-> @ad :selected) :discard)}
  {:active? true :op :deck    :title ">Deck"    :fn #(move-cards! (-> @ad :selected) :deck)}
  {:active? true :op :flip    :title "Flip"     :fn #(toggle-status! (-> @ad :selected) :flipped)}
  {:active? true :op :exhaust :title "Exhaust"  :fn #(toggle-status! (-> @ad :selected) :exhausted)}
  {:active? true :op :quest   :title "Quest"    :fn #(toggle-status! (-> @ad :selected) :questing :exhausted)}
  {:active? true :op :attack  :title "Attack"   :fn #(toggle-status! (-> @ad :selected) :attacking :exhausted)}
  {:active? true :op :dmg     :title "+ Dmg"    :fn #(set-counter! :damage inc)}
  {:active? true :op :dmg     :title "- Dmg"    :fn #(set-counter! :damage dec)}
  {:active? true :op :prg     :title "+ Prog"   :fn #(set-counter! :progress inc)}
  {:active? true :op :prg     :title "- Prog"   :fn #(set-counter! :progress dec)}
  {:active? true :op :res     :title "+ Res"    :fn #(set-counter! :resource inc)}
  {:active? true :op :res     :title "- Res"    :fn #(set-counter! :resource dec)}
  {:active? true :op :thr     :title "+ Thr"    :fn #(update-threat! inc)}
  {:active? true :op :thr     :title "- Thr"    :fn #(update-threat! dec)}
  {:active? true :op :que     :title "+ Quest"  :fn #(update-quest-stage! inc)}
  {:active? true :op :que     :title "- Quest"  :fn #(update-quest-stage! dec)}
  {:active? true :op :round   :title "Round"    :fn #(start-round!)}
  {:active? true :op :mull    :title "Mulligan" :fn #(mulligan! :p1deck)}
  {:active? true :op :debug   :title "Debug?"   :fn #(toggle-debug!)}
  ])

(defn commandlist [ selected ]
  (cond
    (= selected :p1deck)    #{:show :shuffle :draw :draw6 :mull :round :thr :debug}
    (= selected :edeck)     #{:show :shuffle :draw :debug}
    (= selected :p1discard) #{:show :debug}
    (= selected :ediscard)  #{:show :debug}
    (= (get-deck-key selected) :p1deck) #{:exhaust :play :discard :deck :flip :quest :attack :dmg :res :debug}
    (= (get-deck-key selected) :edeck)  #{:aside :exhaust :engage :active :stage :discard :deck :flip :dmg :prg :debug}
    (= (get-deck-key selected) :qdeck)  #{:flip :prg :debug}
    :default #{:round :que :debug}))
    
(defn commandbar []
  (let [clist (-> @ad :selected first commandlist)]
    [:div#commandbar
      (for [itm (filter #(contains? clist (:op %)) commandbuttons)]
        [:button.btn.btn-dark.mr-1 {
          :key (gensym)
          :on-click (:fn itm)
          :class (if (false? (:active itm)) "disabled")} 
        (:title itm)])]))                

                
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
                
(defn- touch-card! [ e c ]
  (.stopPropagation e)
  (if (= (:id c) (-> @ad :showcard :id))
      (swap! ad dissoc :showcard)
      (swap! ad assoc :showcard c))
  (select-card! e (:id c)))
    
(defn card-component [ c ]
  [:div.small-card {
    :key (gensym)
    :data-code (:code c)
    :class     (str (if (contains? (:selected @ad) (:id c)) "selected "))
    :on-click       (fn [e] (.stopPropagation e) (select-card! e (:id c)))
    :on-touch-start (fn [e] (touch-card! e c))
    :on-mouse-over  #(show-card! c)  
    :on-mouse-out   #(swap! ad dissoc :showcard) }
    [:img.img-fluid {
      :src (if (:flipped c) (get-card-back c) (:cgdbimgurl c))
      :class (if (:exhausted c) "exhausted ")}]
    (if (:questing c)  [:div.counter-quest [:span.lotr-type-willpower]])
    (if (:attacking c) [:div.counter-quest [:span.lotr-type-attack]])
    (if (and (-> c :flipped nil?) (contains? #{:stage :active :engaged :play :hero} (:loc c)))
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
        nil))])  
  
(defn- deck [ key deck location image ]
    [:div.small-card {
      :id key
      :class (if (contains? (:selected @ad) key) "selected ")
      :on-click (fn [e] (.stopPropagation e) (select-deck! key))}
      [:img.img-fluid {:class (if (= location :discard) "exhausted") :src (str "/img/lotrdb/" image)}]
      [:div.counter.counter-count [:span (count (get-cards-by-location deck location))]]])
  
  
(defn game []
  [:div.container-fluid.my-3 {:on-click #(swap! ad assoc :selected #{})}
    [commandbar]
    [:div.row
      [:div.col-9
        [:div.row 
          [:div.col
            [:div.d-flex.justify-content-around
              [:div
                [:span.mr-1 "Staging"] 
                [:span.lotr-type-threat.mr-2] 
                [:span (->> (get-cards-by-location :edeck :stage) (map :threat) (apply +))]]
              [:div
                [:span.mr-1 "Player 1"] 
                [:span.lotr-type-threat.mr-2] 
                [:span (:pthreat @ad)]]
              [:div
                [:span.mr-1 "Questing"] 
                [:span.lotr-type-willpower.mr-2] 
                [:span (->> @ad :p1deck (filter :questing) (map :willpower) (apply +))]]
              [:div
                [:span.mr-1 "Attacking"] 
                [:span.lotr-type-willpower.mr-2] 
                [:span (->> @ad :p1deck (filter :attacking) (map :willpower) (apply +))]]
              [:div
                [:span.mr-1 "Turn"] 
                [:i.fas-fa-clock.mr-2] 
                [:span (:turn @ad)]]]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div.d-flex
              [:div.d-flex {:style {:margin-right "100px"}}
                ;[:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "ENEMY"]
                (deck :edeck :edeck :deck "encounter_back.jpg" )
                (deck :ediscard :edeck :discard "encounter_back.jpg")]
              [:div.d-flex
                [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "ASIDE"]
                (doall (for [c (get-cards-by-location :edeck :aside)] (card-component c)))]]]]
        [:div.row {:style {:min-height "105px"}}
          (let [q (-> @ad :qdeck (nth (-> @ad :stage :id)))]
            [:div.col
              [:div.d-flex.justify-content-between
                [:div.d-flex
                  [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "STAGING"]
                  [:div.d-flex (doall (for [c (get-cards-by-location :edeck :stage)] (card-component c)))]]
                [:div.d-flex 
                  [:div {:style {:position "absolute" :right "5px" :font-size "56pt" :color "lightgrey" :z-index -1}} "QUEST"]
                  [:div.d-flex {
                      :class (if (contains? (:selected @ad) (:id q)) "selected ")
                      :on-click (fn [e] (select-card! e (:id q)))
                      :on-mouse-over #(show-card! q)  
                      :on-mouse-out #(swap! ad dissoc :showcard)
                      :style {:position "relative"}
                    }
                    [:img.questcard.ml-auto {:style {:background-image (str "url(" (:cgdbimgurl q) ")") :background-position-y (if (:flipped q) "100%" "0%")}}]
                    (if (:flipped q)
                      [:div.counter.counter-prg {:style {:width "3em" :height "3em" :line-height "2.8em"}}
                        [:span.text-center (str (:progress q 0) "/" (:quest_points q 0))]])]]]])]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "ENGAGED"]
            [:div {:style {:position "absolute" :right "15px" :font-size "56pt" :color "lightgrey"}} "ACTIVE"]
            [:div.d-flex 
              [:div.d-flex (doall (for [c (get-cards-by-location :edeck :engaged)] (card-component c)))]
              [:div.d-flex.ml-auto (doall (for [c (get-cards-by-location :edeck :active)] (card-component c)))]
              ]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "PLAYER"]
            [:div.d-flex
              [:div.d-flex.mr-2
                (doall 
                  (for [c (->> @ad :p1deck (filter #(and (= (:type_code %) "hero") (= (:loc %) :play))) (sort-by :normalname))]
                    (card-component c)))]
              [:div.d-flex
                (doall 
                  (for [c (->> @ad :p1deck (filter #(and (not= (:type_code %) "hero") (= (:loc %) :play))) reverse)] 
                    (card-component c)))]]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "HAND"]
            [:div.d-flex
              (deck :p1deck :p1deck :deck "player_back.jpg" )
              (deck :p1discard :p1deck :discard "player_back.jpg")
              [:div.mr-2]
              (doall (for [c (get-cards-by-location :p1deck :hand)] (card-component c)))]]]]
      [:div.col-3
        [:div (if (:showcard @ad) [:img.img-fluid {:src (-> @ad :showcard :cgdbimgurl)}])]
      ;  [:div.border {:style {:height "100%"}} (doall (for [l (:log @ad)] [:div {:key (gensym)} l]))]
       ]]])
      
; MODAL ;
    
(defn- toggle-select-shown [ id ]
  (if (nil? (-> @ad :temp :selected))
      (swap! ad assoc-in [:temp :selected] #{}))
  (if (contains? (-> @ad :temp :selected) id)
      (swap! ad update-in [:temp :selected] disj id)
      (swap! ad update-in [:temp :selected] conj id)))
       
(defn- close-modal []
  (swap! ad dissoc :temp))
  
(def modal-commands [
  {:id :stage :title ">Stage" :fn #((-> @ad :temp :selected (move-cards! :stage)) (swap! ad assoc-in [:temp :selected] #{}))}
  {:id :aside :title ">Aside" :fn #((-> @ad :temp :selected (move-cards! :aside)) (swap! ad assoc-in [:temp :selected] #{}))}
  {:id :draw  :title ">Draw"  :fn #((-> @ad :temp :selected (move-cards! :draw )) (swap! ad assoc-in [:temp :selected] #{}))}
  {:id :close :title "Close"  :fn #(close-modal)}
  {:id :shuffleclose :title "Shuffle & Close" :fn #((close-modal) (shuffle-deck!))}])
               
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
          (for [mc modal-commands]
            [:button.btn.btn-secondary {:key (gensym) :on-click (:fn mc)} (:title mc)])]]]])
            
(defn- debug [] 
  (if (:debug? @ad)
    [:div.container
        [:button.btn.btn-dark {:on-click #((reset! ad {:selected #{}}) (init!))} "Reset!"]
        [:div (-> @ad :selected str)]
        [:div "Stage " (-> @ad :stage)]
        [:div (-> @ad :temp str)]
        [:div (-> @ad (dissoc :pdeck :p1deck :edeck :qdeck :scenario) str)]]))
              
(defn  Page []
  (init!)
  (fn []
    [:div 
      (case (:screen @ad)
        :game (game)
        (scenariosetup))
      (if (= (-> @ad :temp :state) :showcards) [show-select-modal])
      [debug]]))