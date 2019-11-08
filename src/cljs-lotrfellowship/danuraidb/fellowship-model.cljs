(ns danuraidb.fellowship-model
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require 
    [reagent.core :as r]
    [cljs-http.client :as http]
    [cljs.core.async :refer [<!]]))

(def appdata (r/atom nil))

(defn get-item [ obj ]
  (.getItem (.-localStorage js/window) obj))
  
(defn load-card-data! []
  (go (swap! appdata assoc :cards 
    (->> (<! (http/get "/lotrdb/api/data/cards"))
         :body
         (filter #(contains? #{"hero" "ally" "attachment" "event" "player-side-quest"} (:type_code %)))
         ;(filter #(contains? #{"Core"} (:pack_code %)))
    ))))
  
(defn load-user-decks! []
  (go (swap! appdata assoc :decks (:body (<! (http/get "/lotrdb/api/data/userdecks"))))))
(defn load-localStorage! []
  (swap! appdata assoc :corecount (or (int (get-item "lotrcore_owned")) 1) :packsowned (set (get-item "lortpacks_owned")))
  ;(swap! appdata assoc-in [:filter :pack_code] (set (get-item "lortpacks_owned")))
  )
  
(defn init! []
  (load-card-data!)
  (load-user-decks!)
  (load-localStorage!))
  
(defn set-item! [ obj val ]
  (.setItem (.-localStorage js/window) obj (str val))
  (load-localStorage!))
  
(defn load-deck! [ d deck ]
  (swap! appdata assoc d 
    (assoc deck :data (->> deck :data (.parse js/JSON) js->clj))))
  
(defn set-deck-name! [ d name ]
  (swap! appdata assoc-in [d :name] name)
  (swap! appdata update d dissoc :saved?))
  
(defn save-fellowship! []
  nil)
  
(defn add-to-deck! [ d code qty ]
  (swap! appdata update-in [d :data] dissoc code)
  (swap! appdata update d dissoc :saved?)
  (if (< 0 qty) (swap! appdata assoc-in [d :data code] qty))
)

(defn set-modal-card! [ c ]
  (swap! appdata assoc-in [:modal] c))
  
(defn update-filter! [ flt v ]
  (if (nil? (-> @appdata :filter flt)) (swap! appdata assoc-in [:filter flt] #{}))
  (if (contains? (-> @appdata :filter flt) v)
    (swap! appdata update-in [:filter flt] disj v)
    (swap! appdata update-in [:filter flt] conj v)))
    
(defn filtered-cards []
  (reduce 
    (fn [cards [field match]]
      (if (empty? match)
        cards
        (filter 
          #(contains? match (field %))
          cards
        )))
    (:cards @appdata)
    (-> @appdata :filter))
)