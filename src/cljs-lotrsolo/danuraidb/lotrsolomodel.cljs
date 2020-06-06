(ns danuraidb.lotrsolomodel
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require 
    [reagent.core :as r]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]))

; Setup ;
;=======;
    
(def data (r/atom nil))
(defonce ad (r/atom {:selected #{}}))

; Create Encounter Deck

(defn create-edeck! [ s ]
  (let [encounterset   (->> s :encounters (map :code) set)
        encountercards (->> @data :cards (filter #(contains? encounterset (:encounter_name %))))
        encounterdeck  (->> encountercards 
                            (filter #(not= (:type_code %) "quest")) 
                            (map #(repeat (:quantity %) %)) 
                            (apply concat)
                            (sort-by :name)
                            (map-indexed #(assoc %2 :id %1 :loc :deck)))]
    (swap! ad assoc :edeck encounterdeck
                    :qdeck (map-indexed #(assoc %2 :id (str "q" %1)) (sort-by :position (filter #(= (:type_code %) "quest") encountercards))))))
                    
(defn select-scenario! [ s ]
  (swap! ad assoc :scenario s)
  (create-edeck! s))
  
(defn select-scenario-by-name! [ n ]
  (->> @data  
        :scenarios
        (filter #(= (:name %) n))
        first
        select-scenario!))
        
; Create Player Deck
        
(defn create-pdeck! [ decklist ]
  (let [dl (js->clj (.parse js/JSON decklist))
        decklist 
          (apply concat
            (mapv (fn [[code qty]]
              (->> @data :cards (filter #(= (:code %) code)) first (repeat qty))) dl))]
    (swap! ad assoc :pthreat (->> decklist (filter #(= (:type_code %) "hero")) (map :threat) (apply +)))
    (swap! ad assoc :p1deck 
      (map-indexed (fn [id c]
        (if (= (:type_code c) "hero")
            (assoc c :id (str "p1" id) :loc :play :resource 0)
            (assoc c :id (str "p1" id) :loc :deck))) decklist))))
        
(defn select-pdeck! [ d ]
  (create-pdeck! (:data d)))
  
(defn select-pdeck-by-name! [ n ]
  (->> @data
        :pdecks
        (filter #(= (:name %) n))
        first
        select-pdeck!))
  
  
; Utility ;
;=========;

(def deckmap {
  :edeck "Encounter Deck"
  :p1deck "Player Deck"})
  
  
(defn- deckselected? []
  (contains? #{:edeck :p1deck :p2deck :qdeck} (-> @ad :selected first)))
  
(defn get-deck-key [ card_id ]
  (->> [:edeck :p1deck :qdeck]
       (map #(if (contains? (->> @ad % (map :id) set) card_id) %))
       (remove nil?)
       first))
       
(defn- get-deck [ selected ]
  (if (deckselected?)
      (first selected)
      (if (empty? selected)
        :edeck
        (-> selected first get-deck-key))))
        ;(contains? (->> @ad :p1deck (map :id) set) (first selected)) :p1deck
        ;(contains? (->> @ad :edeck (map :id)) (first selected))
        ;:else :edeck)))

(defn- log [ deck & msg ]
  (swap! ad update :log conj (apply str (deckmap deck "") msg)))
  
(defn set-counter! [ param func ]
  (let [deck-key (get-deck-key (-> @ad :selected first))]
    (swap! ad assoc deck-key
      (map #(if (contains? (:selected @ad) (:id %))
                (assoc % param (Math.max 0 (func (param % 0))))
                %)
        (deck-key @ad)))))
        
(defn toggle-status! [ cardset & tags ]
  (let [deck-key (-> cardset first get-deck-key)]
    (swap! ad assoc deck-key
      (map #(if (contains? cardset (:id %))
                (if (-> tags first %)
                    (apply dissoc % tags)
                    (conj % (zipmap tags (repeat true))))
                %) (deck-key @ad)))))
    
    ;(if (and (some? (re-matches #"q.+" (first cardset))) (= status :flipped))
    ;  (swap! ad assoc-in [:stage :side] (if (= :a (-> @ad :stage :side)) :b :a))
    

(defn get-cards-by-location [ deck-key location ]
  (->> @ad 
       deck-key
       (filter #(= (:loc %) location))
       reverse))
      
(defn get-card-back [ c ]
  (if (= :edeck (get-deck-key #{(:id c)}))
      "/img/lotrdb/encounter_back.jpg"
      "/img/lotrdb/player_back.jpg"))
      
; Change Game State Functions! ;
;==============================;
      
(defn shuffle-deck! []
  (if (deckselected?)
    (let [deck-key (-> @ad :selected first)
          p1 (filter #(= (:loc %) :deck) (deck-key @ad))
          p2 (filter #(not= (:loc %) :deck) (deck-key @ad))]
      (swap! ad assoc deck-key (concat p2 (shuffle p1)))
      (log deck-key " Shuffled"))))

      
(defn move-cards! [ cardset target ]
; Assume all cards in set are in the same deck
  (let [deck-key (-> cardset first get-deck-key)]
    (swap! ad assoc deck-key 
      (map #(if (contains? cardset (:id %))
                (assoc % :loc target)
                %) (deck-key @ad)))))
            
(defn draw-cards!
  "draw-cards n - Draw n cards from the selected deck
   draw-cards deck n - Draw n cards from deck"
  ([ deck-key n ]
    (let [tgt (if (= deck-key :edeck) :stage :hand)
          p1 (filter #(= (:loc %) :deck) (deck-key @ad))
          p2 (filter #(not= (:loc %) :deck) (deck-key @ad))]
      (swap! ad assoc deck-key 
        (concat 
          (mapv #(assoc % :loc tgt) (take n p1))
          p2 (nthrest p1 n)))
      (log deck-key " Draw " n " Card(s)")))
  ([ n ] 
    (draw-cards! (get-deck (:selected @ad)) n)))

        
(defn update-threat! [ fun ]
  (swap! ad update :pthreat fun))
  
(defn start-round! []
  ; Ready all cards !?
  (doseq [deck [:edeck :p1deck]]
    (swap! ad assoc deck 
      (map #(dissoc % :exhausted) (deck @ad))))
  ; Add resources (Alive heroes)
  (doseq [deck [:edeck :p1deck]]
    (swap! ad assoc deck 
      (map #(dissoc (if (= (:type_code %) "hero")
                        (update % :resource inc)
                        %) 
                  :questing :exhausted) (deck @ad))))
  (swap! ad update :turn inc)
  (if (< 0 (:turn @ad)) (update-threat! inc))
  (log nil "Start of round"))
  
; UX ;
;====;
        
(defn select-deck! [ dname ]
  (if (contains? (:selected @ad) dname)
      (swap! ad assoc :selected #{})
      (swap! ad assoc :selected #{dname})))
    
(defn select-card! [ evt id ]
  (.stopPropagation evt)
  (swap! ad update :selected disj :edeck :p1deck :p2deck :ediscard :p1discard :p2discard)
  (if (.-shiftKey evt)
    (if (contains? (:selected @ad) id)
        (swap! ad update :selected disj id) 
        (swap! ad update :selected conj id))
    (if (contains? (:selected @ad) id)
        (swap! ad assoc :selected #{})
        (swap! ad assoc :selected #{id}))))
  
(defn update-quest-stage! [ fun ]
;(->> qdeck (map #(re-find #".+\:|.+" (:name %))) distinct)
  (swap! ad assoc :qdeck (map #(assoc % :progress 0) (:qdeck @ad)))
  (swap! ad assoc :stage {
    :id (Math.max 0 (Math.min (-> @ad :qdeck count dec) (-> @ad :stage :id fun)))
    :side :a})
  (swap! ad assoc :selected #{}))
  
(defn mulligan! [ deck ]
  (swap! ad assoc deck 
    (->> @ad
         deck
         (map #(if (= :hand (:loc %))
                   (assoc % :loc :deck)
                   %))
         shuffle))
  (draw-cards! #{deck} 6))
  
(defn toggle-debug! []
  (if (:debug @ad)
      (swap! ad dissoc :debug)
      (swap! ad assoc  :debug true)))
      
(defn showcards! []
  (when (contains? #{:edeck :p1deck :ediscard :p1discard} (-> @ad :selected first))
    (swap! ad assoc-in [:temp :state] :showcards)
    (swap! ad assoc-in [:temp :loc] (keyword (re-find #"deck|discard" (-> @ad :selected first name))))
    ))
    
(defn show-card! [ c ]
  (swap! ad assoc :showcard c))
  
;================;
; Scenario Start ;
;================;
  
(defn startsolo! []
  (swap! ad assoc :screen :game
                  :stage {:id 0 :side :a}
                  :log ["start"]
                  :turn -1
                  :debug true
                  ))

;============;
; Initialise ;
;============;
  
(defn init! []
  (go 
    (swap! data assoc :scenarios (:body (<! (http/get "/lotrdb/api/data/scenarios"))))
    (swap! data assoc :cards (:body (<! (http/get "/lotrdb/api/data/cards"))))
    (swap! data assoc :pdecks (:body (<! (http/get "/lotrdb/api/data/decks"))))
    (select-scenario! (-> @data :scenarios first))
    (select-pdeck! (-> @data :pdecks first))))