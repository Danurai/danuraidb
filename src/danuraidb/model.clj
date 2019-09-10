(ns danuraidb.model
  (:require
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [octet.core :as buf]
    [clj-http.client :as http]
		[danuraidb.database :as db]))

(import java.util.Base64)
    
(def alert (atom []))

(def ^:const systems [
  {:id 1 :code "aosc"   :desc "Age of Sigmar: Champions"      :icon "/img/aosc/icons/quest_ability.png"}
  {:id 2 :code "whuw"   :desc "Warhammer: Underworlds"        :icon "/img/whuw/icons/Shadespire-Library-Icons-Universal.png"}
  {:id 0 :code "lotrdb" :desc "Lord of the Rings LCG"         :icon "/img/lotrdb/icons/sphere_fellowship.png"}
  {:id 3 :code "whconq" :desc "Warhammer 40,000: Conquest LCG" :icon "/img/whconq/icons/skull.png"}])
  
;;;;;;;;;;;;
; LOTRDB   ;
;;;;;;;;;;;;

;; DATA
(def cgdb-pack-name {
  "HfG"  "thfg"
  "JtR"  "ajtr"
  "HoEM" "thoem"
  "WitW" "twitw"
  "BoG"  "tbog"
  "Starter" "core"
  "TiT" "trouble-in-tharbad"
})
;; DATA
(def sku-code { ; id (MEC)sku
  1  1                                ; Core
  2  2  3  3  4  4  5  5  6  6  7  7   ; Shadows of Mirkwood
  8  8                                ; Khazad-dum
  9  9  10 10 11 11 12 12 13 13 14 14   ; Dwarrowdelf
  15 17                               ; Heirs of Numenor
  16 18 17 19 18 20 19 21 20 22 21 23   ; Against the Shadow
  22 25                               ; Voice of Isengard/
  23 26 24 27 25 28 26 29 27 30 28 31   ; Ring-maker
  29 38                               ; The Lost Realm
  30 39 31 40 32 41 33 42 34 43 35 44   ; Angmar Awakened
  36 47                               ; The Grey Havens
  37 48 40 34 41 45 42 46 43 48 44 49   ; Dream-chaser
  38 16 39 24                          ; Saga - The Hobbit 
  45 50 46 51 47 54 48 52 49 53 55 62   ; Saga - The Lord of the Rings
  53 55                               ; The Sands of Harad
  50 56 51 57 52 58 54 59 56 60 57 61   ; Haradrim
  58 65                               ; The Wilds of Rhovanion
  59 66 60 67 62 68 63 69 65 70         ; Ered Mithrin
  61 73                                ; 2 player Ltd Collectors Edition Starter
  64 99 ; PoD
})
    
(defn- load-json-file [ fname ] 
  (-> fname io/resource slurp (json/read-str :key-fn keyword)))
    
;; ALL GET JSON FILES - API WRAPPER

(defn get-cycles [] 
  (load-json-file "private/lotrdb_data_cycles.json"))
			
(defn get-packs [] 
  (load-json-file "private/ringsdb-api-public-packs.json"))
      
(defn get-cards [] 
  (load-json-file "private/lotrdb_data_cards.json"))
			
(defn get-scenarios []
  (load-json-file "private/ringsdb-api-public-scenarios.json"))
      
;; Actual Functions
(defn get-packs-with-sku []
  (->> (get-packs)
       (map (fn [p]
              (assoc p :sku (str "MEC" (format "%02d" (get sku-code (:id p)))))))))
							
(defn get-cards-with-cycle []
  (let [positions (reduce merge (map #(hash-map (:code %) (:cycle_position %)) (get-packs)))
				cycles (get-cycles)]
    (map (fn [c] 
					(let [pos (get positions (:pack_code c))
								cycle (->> cycles (filter #(= (:cycle_position %) pos)) first)]
						(assoc c :cycle_position pos
										 :cycle_name (:name cycle)
										 )))
      (get-cards))))
	
(defn lotrdb-api-data [ id ]
	(case id
		"cards"     (get-cards-with-cycle)
		"packs"     (get-packs-with-sku)
		"cycles"    (get-cycles)
		"scenarios" (get-scenarios)
		{:status "Not Found"}))
		
(defn- normalize-name [ name ]
  (-> name
    clojure.string/lower-case
    (clojure.string/replace #"\s" "-")
    (clojure.string/replace #"\'|\!" "")
    (clojure.string/replace "\u00e1" "a")
    (clojure.string/replace "\u00e2" "a")
    (clojure.string/replace "\u00e4" "a")
    (clojure.string/replace "\u00e9" "e")
    (clojure.string/replace "\u00ed" "i")
    (clojure.string/replace "\u00f3" "o")
    (clojure.string/replace "\u00fa" "u")))
    
      
(defn- cgdb-card-name [ card ]
  (let [pack (->> (get-packs-with-sku) (filter #(= (:code %) (:pack_code card))) first)]
    (cond
      (some #(= (:id pack) %) (set (apply merge (range 1 23) [37 38 39 61])))
        (str 
          (normalize-name (:name card))
          "-"
          (cgdb-pack-name (:pack_code card) (clojure.string/lower-case (:pack_code card))))
      (= (:id pack) 23)
        (str 
          (normalize-name (:name card))
          "_"
          (normalize-name (:pack_name card))
          "_"
          (:position card)) 
      (< 23 (:id pack) 26)
        (str 
          (normalize-name (:name card))
          "-"
          (normalize-name (:pack_name card))
          "-"
          (:position card)) 
      (= (:id pack) 40)
        (str (:sku pack) "_" (format "%03d" (:position card)))
      :else ;(< 25 (:id pack))
        (str (:sku pack) "_" (:position card))
      )))
          
(defn get-card-image-url 
  ([ card size ]
      (str "http://www.cardgamedb.com/forums/uploads/lotr/"
          (if (= size :small) "tn_" "ffg_")
          (cgdb-card-name card)
          ".jpg"))
  ([ card ] 
    (get-card-image-url card :normal)))
    
    
;;;;;;;;
; AOSC ;
;;;;;;;;


(defn aoscsearch [ size ]
  (try 
    (http/post "https://carddatabase.warhammerchampions.com/warhammer-cards/_search" 
               {:content-type :json
                :body (json/write-str {:size size :from 0})})
    (catch Exception e nil)))

(defn aosc-api-cards []
  (if-let [res (aoscsearch 1)]
    (-> (aoscsearch (-> res :body (json/read-str :key-fn keyword) :hits :total))
        :body
        (json/read-str :key-fn keyword)
        (assoc :source "https://carddatabase.warhammerchampions.com/warhammer-cards/_search"))
    (-> (load-json-file "private/aosc_cards.json")
        (assoc :source "aosc_cards.json"))))
        
(defn aosc-get-cards []
  (map 
    #(assoc (:_source %) :setnumber (-> % :_source :set first :number))
    (-> (aosc-api-cards) :hits :hits)))
        
(def aosc-types
  [{:name "Champion" :symbol [:i.fas.fa-users] :img "/img/aosc/icons/category_champion.png"}
   {:name "Blessing" :symbol [:i.fas.fa-sun] :img "/img/aosc/icons/category_blessing.png"}
   {:name "Unit"    :symbol [:i.fas.fa-shield-alt] :img "/img/aosc/icons/category_unit.png"}
   {:name "Spell"   :symbol [:i.fas.fa-scroll] :img "/img/aosc/icons/category_spell.png"}
   {:name "Ability" :symbol [:i.fas.fa-bolt] :img "/img/aosc/icons/category_ability.png"}])
   
(def aosc-alliances
  [{:name "Order"  :symbol [:i.fas.fa-ankh]}
   {:name "Chaos"  :symbol [:i.fas.fa-star-of-life]}
   {:name "Death"  :symbol [:i.fas.fa-skull]}
   {:name "Destruction" :symbol [:i.fas.fa-gavel]}
   {:name "Any" :symbol [:i.fas.fa-plus]}
   ])
   
(def aosc-traits
  (->> (aosc-get-cards)
       (map :tags)
       (reduce concat)
       distinct
       sort))
   
   
;;;;;;;;;;;;
;; WHUW   ;;
;;;;;;;;;;;;

(defn- whuw_fix_cards [ card ]
  (case (:id card)
    9105 (assoc card :warbands [35])   ;; Lucky Trinket
    9112 (assoc card :card_types [20]) ;; Measured Strike
    12609 (assoc card :warbands [35])  ;; Ghoulish Pact
    card))
    
(def whuwdata
  (load-json-file "private/whuw_data_r2.json"))
(def whuwcards
  (map #(whuw_fix_cards %) (load-json-file "private/whuw_cards_r2.json")))
  
(defn whuw_fullcards [] 
  (let [banlist  (load-json-file "private/whuw_restricted_r2.json")]
    (map (fn [c]
      (let [set (->> whuwdata :sets (filter #(= (:id %) (:set c))) first)
            type (->> whuwdata :card-types (filter #(= (:id %) (:card_type c))) first)
            warband (->> whuwdata :warbands (filter #(= (:id %) (:warband c))) first)
            banned (= 1 (->> banlist :banned (filter #(= (:code %) (:code c))) count))
            restricted (= 1 (->> banlist :restricted (filter #(= (:code %) (:code c))) count))
            ]
      (assoc c
        :set_id (:id set)
        :set_name (:name set)
        :set_icon (-> set :icon :filename)
        :card_type_id (:id type)
        :card_type_name (:name type)
        :card_type_icon (-> type :icon :filename)
        :warband_id (:id warband)
        :warband_name (:name warband)
        :warband_icon (-> warband :icon :filename)
        :banned banned
        :restricted restricted
    ))) whuwcards)))
    
;;;;;;;;;;;;
;; FILTER ;;
;;;;;;;;;;;;

(def find-regex #".*?(?=\s[a-z][:\<\>])|.+")
(def field-regex #"([a-z])([:\<\>])(.+)")

(defn- get_op_fn [ operator ]
  (case operator
    "<" <
    ">" >
    =))
    
(def filter-synonyms {
  :type_code {"h" "hero" "a" "ally" "e" "event" "t" "attachment"}
  :sphere_code {"l" "leadership" "o" "lore" "s" "spirit" "t" "tactics" "n" "neutral" "f" "fellowship"}
  :class {"wa" "Warrior" "wi" "Wizard" "ww" "Warrior Wizard"}
  :rarity {"c" "Common" "u" "Uncommon" "r" "Rare" "e" "Exclusive"}
})

(def lotrdb-field-map {
  "e" :pack_code      
  "n" :encounter_name 
  "r" :traits         
  "s" :sphere_code    
  "t" :type_code      
  "x" :text           
  "y" :cycle_position
  "d" :deck ;p or e
  })
  
(def aosc-field-map {
  "a" :alliance 
  "c" :category   ; Champion, Blessing, Unit, Spell, Action
  "w" :class      ; Warrior, Wizard, Warrior Wizard
  "s" :setnumber  ; CUSTOM FIELD, = number
  "t" :tags
  "r" :rarity     ; Common, Uncommon, Rare, Exclusive
  "o" :cost       ; number
  "h" :healthMod  ; number
  "u" :unique     ;true/false
  "x" :effect
  })

(defn fmap [qry field-map]
"returns a collection of maps including {:id 'field name' :val 'match')" 
  (map #(let [field-flt  (->> % (re-seq field-regex) first)
             field-name (get field-map (get field-flt 1))
             field-op   (get field-flt 2)
             field-val  (get field-flt 3)]
          {
            :id field-name
            :val (or 
                  (case field-name 
                     (:cycle_position :healthMod :cost :setnumber) (read-string field-val)
                     field-val)
                  %)
            :op (get_op_fn field-op)
          })
    (->> qry (re-seq find-regex) (remove clojure.string/blank?))
  )
)

(defn cardfilter [ q cards system ]
  (let [field-map (case system 
                    :lotrdb lotrdb-field-map
                    :aosc aosc-field-map
                    {"x" :text})]
    (sort-by :code
      (reduce
        (fn [data {:keys [id val op]}]
          (case id
            (:name :text :traits :alliance) 
              (filter #(some? (re-find (re-pattern (str "(?i)" val)) (id % ""))) data) ; partial match
            (:pack_code :type_code :sphere_code) 
              (filter 
                (fn [x] 
                  (some 
                    #(some? (re-find (re-pattern (str "(?i)" (get-in filter-synonyms [id %] %))) (id x)))
                    ;#(= (id x) (get-in filter-synonyms [id %] %))
                    (clojure.string/split val #"\|")))
                data)
            :deck
              (filter 
                (fn [c] 
                  (let [cfn (if (= val "p") some not-any?)
                       player_type_codes ["hero" "attachment" "ally" "event" "fellowship" "baggins"]]
                    (cfn (partial = (:type_code c)) player_type_codes)))
                data)
            :tags     (filter (fn [c] (some #(= val %) (:tags c))) data)
            :unique   (filter (fn [c] (if (= val "true") (some #(= "Unique" %) (:tags c)) (not-any? #(= "Unique" %) (:tags c)))) data)
            (:category :class) (filter #(= (-> % id :en) val) data)
            :effect   (filter #(some? (re-find (re-pattern (str "(?i)" val)) (-> % id :en))) data)
            (filter #(op (id %) val) data)))
        cards
        (fmap q field-map)))))
      
;;;;;;;;;;;;; Parse Deck ;;;;;;;;;;;;;;


(def MAX_SUPPORTED_VERSION 1)
(def ^:const BYTES_PER_CARD 5)
(def wh-spec (buf/spec :count buf/byte :id buf/int32))

; RYO instad of octet
;
;(defn toUint32 [ta]
;  (bit-or (bit-shift-left (aget ta 3) 24)
;          (bit-shift-left (bit-and 0xff (aget ta 2)) 16)
;          (bit-shift-left (bit-and 0xff (aget ta 1)) 8)
;          (bit-and 0xff (aget ta 0))))
;(defn getCards [data]
;  (let [cardData (-> data second byte-array)]
;    (prn cardData)
;    (for [i (range 0 (count cardData) 5)]
;      (assoc {} :count (aget cardData i) 
;                :id (-> cardData (nthrest (inc i)) byte-array toUint32x)))))

(defn- fromBase64 [b64] 
  (.decode (Base64/getDecoder) b64))
  
(defn- parseQueryStringDeckCode [qsDeckCode]
  (-> qsDeckCode
      (clojure.string/replace #"_" "/")
      (clojure.string/replace #"[- ]" "+")))

(defn- getVersionAndData [raw]
  (let [view (map int raw)]
    (if (= (and (first raw) 0xFF) 255)
        [(nth view 1) (nthrest view 2)]
        [0 []])))
        
(defn- getCards [cards]
  (let [n (count cards)
        buffer (buf/allocate n)]
    (buf/write! buffer (byte-array cards) (buf/repeat n buf/byte))
    (for [i (range 0 n BYTES_PER_CARD)]
      (buf/with-byte-order :little-endian
        (buf/read buffer wh-spec {:offset i})))))
        
(defn parsedeck [deckcode]
  (let [[version cards] (-> deckcode
                            parseQueryStringDeckCode
                            fromBase64
                            getVersionAndData)]
    {:version version :cards (getCards cards)}))

    

(defn get-deck-data [req]
	(let [id   (-> req :params :id)
        new_deck {:uid (db/unique-deckid)}]
    (cond 
      (nil? id) 
        new_deck
      (some? (re-matches #"[0-9A-F]{6}" id)) 
        (db/get-user-deck id)
      (contains? #{"Order" "Chaos" "Death" "Destruction"} id) ;; AoSC 
        (assoc new_deck :alliance id :name (str id " deck"))
      :else 
        (try 
          (if (= 1 (:version (parsedeck id)))
              (assoc new_deck :data id :name "Imported Deck")
              (assoc new_deck :name id))
          (catch Exception e (assoc new_deck :name id))))))

;            (if (some? (-> req :params :deck))
;              (let [new_deck (json/read-str (-> req :params :deck) :key-fn keyword)]
;                (assoc new_deck :data (-> deck :data json/write-str)))
;              {})))))