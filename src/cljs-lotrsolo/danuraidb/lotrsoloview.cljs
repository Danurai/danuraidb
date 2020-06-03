(ns danuraidb.lotrsoloview
  (:require
    [reagent.core :as r]
    [danuraidb.lotrsolomodel :refer 
      [ad data showcards!
       init! startsolo! get-cards-by-location get-card-back toggle-status! update-threat! update-quest-stage!
       select-scenario-by-name! select-pdeck-by-name! start-round! mulligan! toggle-debug! show-card!
        shuffle-deck! draw-cards! select-deck! select-card! set-counter!]]))
        
(def commandbuttons (r/atom [
  {:active? true :op :show    :title "Show"    :fn #(showcards!)}
  {:active? true :op :shuffle :title "Shuffle" :fn #(shuffle-deck!)}
  {:active? true :op :draw    :title "Draw"    :fn #(draw-cards! 1)}
  {:active? true :op :draw6   :title "Draw 6"  :fn #(draw-cards! 6)}
  {:active? true :op :stage   :title "Stage"   :fn #(draw-cards! (-> @ad :selected) :stage)}
  {:active? true :op :aside   :title "Aside"   :fn #(draw-cards! (-> @ad :selected) :aside)}
  {:active? true :op :exhaust :title "Exhaust" :fn #(toggle-status! :exhausted)}
  {:active? true :op :engage  :title "Engage"  :fn #(draw-cards! (-> @ad :selected) :engaged)}
  {:active? true :op :active  :title "Active"  :fn #(draw-cards! (-> @ad :selected) :active)}
  {:active? true :op :play    :title "Play"    :fn #(draw-cards! (-> @ad :selected) :play)}
  {:active? true :op :discard :title ">Discard" :fn #(draw-cards! (-> @ad :selected) :discard)}
  {:active? true :op :deck    :title ">Deck"    :fn #(draw-cards! (-> @ad :selected) :deck)}
  {:active? true :op :flip    :title "Flip"    :fn #(toggle-status! :flipped)}
  {:active? true :op :quest   :title "Quest"   :fn 
    (fn [] (let [cardset (:selected @ad)]
      (doseq [s [:questing :exhausted]]  (toggle-status! cardset s))))}
  
  {:active? true :op :dmg     :title "+ Dmg"   :fn #(set-counter! :damage inc)}
  {:active? true :op :dmg     :title "- Dmg"   :fn #(set-counter! :damage dec)}
  {:active? true :op :prg     :title "+ Prog"  :fn #(set-counter! :progress inc)}
  {:active? true :op :prg     :title "- Prog"  :fn #(set-counter! :progress dec)}
  {:active? true :op :res     :title "+ Res"   :fn #(set-counter! :resource inc)}
  {:active? true :op :res     :title "- Res"   :fn #(set-counter! :resource dec)}
  {:active? true :op :thr     :title "+ Thr"   :fn #(update-threat! inc)}
  {:active? true :op :thr     :title "- Thr"   :fn #(update-threat! dec)}
  {:active? true :op :que     :title "+ Quest" :fn #(update-quest-stage! inc)}
  {:active? true :op :que     :title "- Quest" :fn #(update-quest-stage! dec)}
  {:active? true :op :round   :title "Round"   :fn #(start-round!)}
  
  {:active? true :op :mull    :title "Mulligan" :fn #(mulligan! :p1deck)}
  {:active? true :op :debug   :title "Debug?" :fn #(toggle-debug!)}
  ]))

(def commandlist {
  :p1deck    #{:show :shuffle :draw :draw6 :mull :debug :round}
  :edeck     #{:show :shuffle :draw :debug}
  :p1discard #{:show :debug}
  :ediscard  #{:show :debug}
  :pcard     #{:exhaust :play :discard :deck :flip :quest :dmg :res :debug}
  :ecard     #{:aside :exhaust :engage :active :stage :discard :deck :flip :dmg :prg :debug}
  :qcard     #{:flip :prg :debug}
  })
  
(defn update-commandbuttons! [ key ]
  (prn key)
  (let [btnset (if (empty? (:selected @ad)) #{:que :round} (if (< -1 (:turn @ad)) (disj (key commandlist #{}) :mull) (key commandlist #{})))]
    (prn btnset)
    (reset! commandbuttons
      (map #(if (contains? btnset (:op %))
                (assoc % :active? true)
                (dissoc % :active?)) @commandbuttons))))
                
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
                
              
(defn commandbar []
  [:div#commandbar
    (for [itm (filter :active? @commandbuttons)]
      [:button.btn.btn-dark.mr-1 {
        :key (gensym)
        :on-click (:fn itm)
        :class (if (false? (:active itm)) "disabled")} 
      (:title itm)])])

    
(defn card-component [ c ]
  [:div.small-card {      ;.card-link
    :key (gensym)
    :data-code (:code c)
    :class (str 
            (if (contains? (:selected @ad) (:id c)) "selected "))
    :on-click 
      (fn [e] 
        (select-card! e (:id c)) 
        (update-commandbuttons!
          (let [cardid (-> @ad :selected first)]
            (cond 
              (nil? cardid)     :none
              (integer? cardid) :ecard
              (= "p" (subs cardid 0 1)) :pcard
              :else :none))))
    :on-mouse-over #(show-card! c)  
    :on-mouse-out #(swap! ad dissoc :showcard) }
    [:img.img-fluid {
      :src (if (:flipped c) (get-card-back c) (:cgdbimgurl c))
      :class (if (:exhausted c) "exhausted ")}]
    (if (:questing c)
      [:div.counter-quest [:span.lotr-type-willpower]])
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
      :on-click (fn [e] (.stopPropagation e) (select-deck! key) (update-commandbuttons! key))}
      [:img.img-fluid {:class (if (= location :discard) "exhausted") :src (str "/img/lotrdb/" image)}]
      [:div.counter.counter-count [:span (count (get-cards-by-location deck location))]]])
  
  
(defn game []
  [:div.container-fluid.my-3
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
                (doall (for [c (reverse (get-cards-by-location :edeck :aside))] (card-component c)))]]]]
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
                      :on-click (fn [e] (select-card! e (:id q)) (update-commandbuttons! :qcard))
                      :on-mouse-over #(show-card! q)  
                      :on-mouse-out #(swap! ad dissoc :showcard)
                      :style {:position "relative"}
                    }
                    [:img.questcard.ml-auto {:style {:background-image (str "url(" (:cgdbimgurl q) ")") :background-position-y (if (= :b (-> @ad :stage :side)) "100%" "0%")}}]
                    (if (= :b (-> @ad :stage :side))
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
            [:div.d-flex.mr-2
              (doall 
                (for [c (->> @ad :p1deck (filter #(and (= (:type_code %) "hero") (= (:loc %) :play))) (sort-by :normalname))]
                  (card-component c)))]
            [:div.d-flex
              (doall 
                (for [c (->> @ad :p1deck (filter #(and (not= (:type_code %) "hero") (= (:loc %) :play))))] 
                  (card-component c)))]]]
        [:div.row {:style {:min-height "105px"}}
          [:div.col
            [:div {:style {:position "absolute" :font-size "56pt" :color "lightgrey"}} "HAND"]
            [:div.d-flex
              (deck :p1deck :p1deck :deck "player_back.jpg" )
              (deck :p1discard :p1deck :discard "player_back.jpg")
              [:div.mr-2]
              (doall (for [c (reverse (get-cards-by-location :p1deck :hand))] (card-component c)))]]]]
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
  {:id :stage :title ">Stage" :on-click #(-> @ad :temp :selected (draw-cards! :stage))}
  {:id :aside :title ">Aside" :on-click #(-> @ad :temp :selected (draw-cards! :aside))}
  {:id :draw  :title ">Draw"  :on-click #(-> @ad :temp :selected (draw-cards! :hand))}
  {:id :close :title "Close"  :on-click #(close-modal)}
  {:id :shuffleclose :title "Shuffle & Close" :on-click #((close-modal) (shuffle-deck!))}])
               
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
            [:button.btn.btn-secondary {:key (gensym) :on-click (:on-click mc)} (:title mc)])]]]])
            
(defn- debug [] 
  (if (:debug? @ad)
    [:div.container
        [:button.btn.btn-dark {:on-click #((reset! ad {:selected #{}}) (init!))} "Reset!"]
        [:div (-> @ad :selected str)]
        [:div "Stage " (-> @ad :stage)]
        [:div (-> @ad :temp str)]
        [:div (-> @ad (dissoc :pdeck :p1deck :edeck :qdeck :scenario) str)]]))
              
(defn Page []
  (init!)
  (fn []
    [:div 
      (case (:screen @ad)
        :game (game)
        (scenariosetup))
      (if (= (-> @ad :temp :state) :showcards) [show-select-modal])
      [debug]]))