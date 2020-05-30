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
            (assoc c :id (str "p1" id) :loc :hero :resource 0)
            (assoc c :id (str "p1" id) :loc :deck))) decklist))))
        
(defn select-pdeck! [ d ]
  (create-pdeck! (:data d)))
  
(defn select-pdeck-by-name! [ n ]
  (->> @data
        :pdecks
        (filter #(= (:name %) n))
        first
        select-pdeck!))
  
; Initialise ;
;============;
  
(defn init! []
  (go 
    (swap! data assoc :scenarios (:body (<! (http/get "/lotrdb/api/data/scenarios"))))
    (swap! data assoc :cards (:body (<! (http/get "/lotrdb/api/data/cards"))))
    (swap! data assoc :pdecks (:body (<! (http/get "/lotrdb/api/data/decks"))))
    (select-scenario! (-> @data :scenarios first))
    (select-pdeck! (-> @data :pdecks first))))
                   
                   
(defn movecard! [ ad cardid location ]
  (swap! ad assoc :edeck (map #(if (= (:id %) cardid) (assoc % :loc location) %) (:edeck @ad))))
  
  
; Scenario Start ;
;================;
  
(defn startsolo! []
  (swap! ad assoc :screen :game
                  :stage {:id 0 :side :a}
                  :log ["start"]
                  :turn -1
                  :debug true
                  ))
; Utility ;
;=========;

(def deckmap {
  :edeck "Encounter Deck"
  :p1deck "Player Deck"})

(defn- log [ deck & msg ]
  (swap! ad update :log conj (apply str (deckmap deck "") msg)))
  
  
        
(defn set-counter! [ param func ]
  (doseq [deck [:edeck :p1deck :p2deck]]
    (swap! ad assoc deck
      (map 
        #(if (contains? (:selected @ad) (:id %))
             (assoc % param (Math.max 0 (func (param % 0))))
             %)
        (deck @ad)))))

(defn toggle-status! 
  ([ status ]
    (toggle-status! (:selected @ad) status))
  ([ cardset status ]
  ;Force effect?
    (if (and (some? (re-matches #"q.+" (first cardset))) (= status :flipped))
      (swap! ad assoc-in [:stage :side] (if (= :a (-> @ad :stage :side)) :b :a))
      (doseq [deck [:edeck :p1deck]]
          (swap! ad assoc deck 
            (map #(if (contains? cardset (:id %))
                      (if (status %)
                          (dissoc % status)
                          (assoc % status true))
                      %) (deck @ad)))))
    (swap! ad assoc :selected #{})))

(defn get-cards-by-location [ deck-key location ]
  (->> @ad 
       deck-key
       (filter #(= (:loc %) location))))
      
(defn- deckselected? []
  (contains? #{:edeck :p1deck} (-> @ad :selected first)))
      
; shuffle-deck
(defn shuffle-deck! []
  (if (deckselected?)
    (let [deck-key (-> @ad :selected first)
          p1 (filter #(= (:loc %) :deck) (deck-key @ad))
          p2 (filter #(not= (:loc %) :deck) (deck-key @ad))]
      (swap! ad assoc deck-key (concat p2 (shuffle p1)))
      (log deck-key " Shuffled"))))

(defn- get-deck [ selected ]
  (if (deckselected?)
      (first selected)
      (cond
        (contains? (->> @ad :p1deck (map :id) set) (first selected)) :p1deck
        ;(contains? (->> @ad :edeck (map :id)) (first selected))
        :else :edeck)))
      
(defn get-card-back [ c ]
  (if (= :edeck (get-deck #{(:id c)}))
      "/img/lotrdb/encounter_back.jpg"
      "/img/lotrdb/player_back.jpg"))
      
(defn draw-cards!
  ([ deck-key cards tgt ]
    (let [target (if (= tgt :draw) (if (= deck-key :edeck) :stage :hand) tgt)]
      (if (set? cards)
        (swap! ad assoc deck-key
          (map #(if (contains? cards (:id %))
                    (assoc % :loc target)
                    (if (and (= target :active) (= (:loc %) :active))
                        (assoc % :loc :stage)
                        %)) (-> @ad deck-key)))
        (let [p1 (filter #(= (:loc %) :deck) (deck-key @ad))
              p2 (filter #(not= (:loc %) :deck) (deck-key @ad))]
                (swap! ad assoc deck-key 
                  (concat 
                    (mapv #(assoc % :loc target) (take cards p1))
                    p2 (nthrest p1 cards)))))
      (log deck-key " Draw Cards " cards)
      ;(if (false? deckselected?) 
        (swap! ad assoc :selected #{})
      ))
  ([ cards tgt ]
    (draw-cards! (get-deck (:selected @ad)) cards tgt))
  ([ cards ]
    (draw-cards! (get-deck (:selected @ad)) cards :draw))
  ([] 
    (draw-cards! (get-deck (:selected @ad)) 1 :draw)))

(defn start-round! []
  ; Ready all cards !?
  (doseq [deck [:edeck :p1deck]]
    (swap! ad assoc deck 
      (map #(dissoc % :exhausted) (deck @ad))))
  ; Add resources (Alive heroes)
  (doseq [deck [:edeck :p1deck]]
    (swap! ad assoc deck 
      (map #(if (= (:type_code %) "hero")
                (update % :resource inc)
                %) (deck @ad))))
  (swap! ad assoc :selected #{:p1deck})
  (swap! ad update :turn inc)
  (draw-cards!)
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
        
(defn update-threat! [ fun ]
  (swap! ad update :pthreat fun))
  
(defn update-quest-stage! [ fun ]
;(->> qdeck (map #(re-find #".+\:|.+" (:name %))) distinct)
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
  (draw-cards! deck 6 :hand))
  
(defn toggle-debug! []
  (swap! ad assoc :debug? (-> @ad :debug? nil?))) 
  
  
(defn showcards! []
  (when (contains? #{:edeck :p1deck :ediscard :p1discard} (-> @ad :selected first))
    (swap! ad assoc-in [:temp :state] :showcards)
    (swap! ad assoc-in [:temp :loc] (keyword (re-find #"deck|discard" (-> @ad :selected first name))))
    ))
    
(defn show-card! [ c ]
  (swap! ad assoc :showcard c))
  
