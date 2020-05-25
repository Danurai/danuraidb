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
                    :qdeck (sort-by :position (filter #(= (:type_code %) "quest") encountercards)))))
                    
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
  (let [dl (js->clj (.parse js/JSON decklist))]
    (swap! ad assoc :p1deck 
      (map-indexed (fn [id c]
        (if (= (:type_code c) "hero")
            (assoc c :id (str "p1" id) :loc :hero :resource 0)
            (assoc c :id (str "p1" id) :loc :deck)))
        (apply concat
          (mapv (fn [[code qty]]
            (->> @data :cards (filter #(= (:code %) code)) first (repeat qty))) dl))))))
        
(defn select-pdeck! [ d ]
  (swap! ad assoc :pdeck d)
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
                  :stage 1
                  :log ["start"]))
; Utility ;
;=========;

(def deckmap {
  :edeck "Encounter Deck"
  :p1deck "Player Deck"})

(defn- log [ deck & msg ]
  (swap! ad update :log conj (apply str (deckmap deck "") msg)))
  
  
        
(defn set-counter! [ param func ]
  (doseq [deck [:edeck :p1deck :p2deck]]
    (prn param func)
    (swap! ad assoc deck
      (map 
        #(if (contains? (:selected @ad) (:id %))
             (assoc % param (Math.max 0 (func (param % 0))))
             %)
        (deck @ad)))))
  

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
      

      
; draw-cards[ #set target-location ]
; draw-cards[ number-of-cards ]
; draw-cards[] 
(defn draw-cards!
"draw-cards []  Draw 1 card from the selected deck
 draw-cards [#{set}] Draw cards with ids in the set
 draw-cards [n] Draw n cards"
  ([ cards tgt ]
    (let [deck-key (get-deck (:selected @ad))
          target (if (= tgt :draw) (if (= deck-key :edeck) :stage :hand) tgt)]
      (if (set? cards)
        (swap! ad assoc deck-key
          (map #(if (contains? cards (:id %))
                    (assoc % :loc target)
                    %) (-> @ad deck-key)))
        (let [p1 (filter #(= (:loc %) :deck) (deck-key @ad))
              p2 (filter #(not= (:loc %) :deck) (deck-key @ad))]
                (swap! ad assoc deck-key 
                  (concat 
                    (mapv #(assoc % :loc target) (take cards p1))
                    p2 (nthrest p1 cards)))))
      (log deck-key " Draw Cards " cards)
      (swap! ad assoc :selected #{})))
  ([] 
    (draw-cards! 1 :draw))
  ([ cards ]
    (draw-cards! cards :draw)))

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