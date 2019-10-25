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

(def questinfo
  [{:id 1 :name "Passage Through Mirkwood" :loc [{:code "01096" :qty 1 :loc :stage} {:code "01100" :qty 1 :loc :stage}]}])  
(def testdeck {"01005" 1 "02004" 3 "02098" 3 "02050" 1 "01006" 1 "01029" 3 "02075" 3 "08059" 3 "01014" 2 "01013" 3 "01073" 3 "01042" 1 "01040" 2 "01039" 2 "01027" 1 "01026" 2 "02010" 3 "01032" 2 "02005" 3 "01023" 2 "01035" 2 "02054" 2 "01020" 2 "02119" 2 "01016" 1})


(defn init! []
  (go 
    (swap! data assoc :scenarios (:body (<! (http/get "/lotrdb/api/data/scenarios"))))
    (swap! data assoc :cards (:body (<! (http/get "/lotrdb/api/data/cards"))))))
    

; Scenario Setup ;
;================;   

(defn initialise-encounter [ edeck questlocations ]
  (reduce 
    (fn [ed {:keys [code qty loc]} ]
      (let [p1 (take-while #(not= (:code %) code) ed)]
        (concat 
          p1 
          (map #(assoc % :loc loc) (take qty (nthrest ed (count p1))))
          (nthrest ed (+ qty (count p1))))))
    edeck 
    questlocations))

(defn selectscenario! [ s ]
  (let [ql (->> questinfo (filter #(= (:name %) (:name s))) first :loc)
        encounterset   (->> s :encounters (map :code) set)
        encountercards (->> @data :cards (filter #(contains? encounterset (:encounter_name %))))
        encounterdeck  (->> encountercards 
                            (filter #(not= (:type_code %) "quest")) 
                            (map #(repeat (:quantity %) %)) 
                            (apply concat)
                            (sort-by :name)
                            (map-indexed #(assoc %2 :id %1 :loc :deck)))]
    (prn ql); (if ql (map :loc (initialise-encounter encounterdeck ql))))
    (swap! ad assoc :scenario s
                    :edeck (if ql (initialise-encounter encounterdeck ql) encounterdeck)
                    :qdeck (sort-by :position (filter #(= (:type_code %) "quest") encountercards)))))
                   
(defn movecard! [ ad cardid location ]
  (swap! ad assoc :edeck (map #(if (= (:id %) cardid) (assoc % :loc location) %) (:edeck @ad))))
  
  
; Scenario Start ;
;================;
  
(defn create-edeck! [ decklist ]
  nil)

(defn create-pdeck [ decklist ]
  (map-indexed (fn [id c]
    (if (= (:type_code c) "hero")
        (assoc c :id (str "p1" id) :loc :hero)
        (assoc c :id (str "p1" id) :loc :deck)))
    (apply concat
      (mapv (fn [[code qty]]
        (->> @data :cards (filter #(= (:code %) code)) first (repeat qty))) decklist))))
              
(defn startsolo! []
  (swap! ad assoc :edeck (-> @ad :edeck shuffle)
                  :screen :setup
                  :stage 1
                  :p1deck (shuffle (create-pdeck testdeck))))
                       

; Utility ;
;=========;
  
(defn cards-by-location [ loc ]
  (->> @ad
      :edeck
      (filter #(= (:loc %) loc))))
      
; shuffle-deck
(defn shuffle-edeck! []
  (let [p1 (filter #(= (:loc %) :deck) (:edeck @ad))
        p2 (filter #(not= (:loc %) :deck) (:edeck @ad))]
    (swap! ad assoc :edeck (concat p2 (shuffle p1)))))
    
(defn draw-card! []
  (let [deck (-> @ad :selected first)
        p1 (filter #(= (:loc %) :deck) (deck @ad))
        p2 (filter #(not= (:loc %) :deck) (deck @ad))]
    (case deck
      :edeck 
        (swap! ad assoc :edeck (concat [(assoc (first p1) :loc :stage)] p2 (rest p1)))
      (:p1deck :p2deck)
        (swap! ad assoc deck (concat [(assoc (first p1) :loc :area)] p2 (rest p1))))))
        
(defn set-counter! [ param func ]
  (doseq [deck [:edeck :p1deck :p2deck]]
    (swap! ad assoc deck
      (map 
        #(if (contains? (:selected @ad) (:id %))
             (assoc % param (Math.max 0 (func (param % 0))))
             %)
        (deck @ad)))))

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