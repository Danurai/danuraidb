(ns danuraidb.web
   (:require 
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [compojure.core :refer [context defroutes GET ANY POST]]
    [compojure.route :refer [resources]]
    [clj-time.core :as time]
    [clj-time.coerce :as tc]
    [clj-time.format :as tf]
    [ring.util.response :refer [response content-type redirect]]
    [ring.middleware.keyword-params :refer [wrap-keyword-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.session :refer [wrap-session]]
    [ring.middleware.cors :refer [wrap-cors]]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                   [credentials :as creds])
    [hiccup.page :as h]
    [danuraidb.database :as db]
    [danuraidb.pages :as pages]
    [danuraidb.model :as model]))
  
(defn- alert [ type msg ]
  (swap! model/alert conj {:type type :msg msg}))
  
(defn do-redirect [ req ]
  (redirect
    (re-find #".+decks|.+" 
      (-> req :headers (get "referer" "/")))))
  
;[id system name decklist alliance tags notes uid]
(defn- save-deck-handler [{:keys [id system name decklist alliance tags notes] :as deck} req]
  (prn (db/save-deck id system name decklist alliance tags notes (-> req model/get-authentications :uid)))
  (alert "info" [:span [:b name] " saved."])
  (do-redirect req))
  
(defn- delete-deck-handler [ uid name req ]
  (db/delete-deck uid)
  (alert "warning" [:span [:b name] " deleted."])
  (do-redirect req))
  
(defn delete-group-handler [ uid name req ]
  (db/delete-deck-group uid)
  (alert "warning" [:span "Fellowship " [:b name] " deleted."])
  (do-redirect req))
  
(defn- save-collection-handler [collectionjson filterjson req]
  (db/save-user-collection collectionjson (-> req model/get-authentications :uid))
  (alert "info" "Collection saved")
  (do-redirect req))

;; ADMIN ;;
     
(defroutes admin-routes
  (GET "/" []
    pages/admin-page)
  (POST "/updatepassword" [uid password]
    (db/updateuserpassword uid password)
    (alert "alert-info" "Password updated")
    (redirect "/admin"))
  (POST "/updateadmin" [uid admin]
    (db/updateuseradmin uid (some? admin))
    (alert "alert-info" (str "Admin status " (if (some? admin) "added" "removed")))
    (redirect "/admin"))
  (POST "/adduser" [username password admin]
    (db/adduser username password (= admin "on"))
    (alert "alert-info" (str "User Account created for " username))
    (redirect "/admin"))
 ; (POST "/deleteuser" [uid]
 ;   (alert "alert-warning" "User Account Deleted")
 ;   (db/dropuser uid)
 ;   (redirect "/admin"))
    )
    
;; LOTRDB ;;
(defn- parsedeckdata [ data ] 
; (questid) deckname decklist deadh dmgh threat score
  (mapv (fn [n]
    (let [pid (str "p" n)]
      (hash-map 
        :deckname (get data (str pid "deckname"))
        :decklist (get data (str pid "decklist"))
        :deadh (read-string (get data (str pid "deadh")))
        :dmgh (read-string (get data (str pid "dmgh")))
        :threat (read-string (get data (str pid "threat")))
        :score (read-string (get data (str pid "score"))))))
    (range 1 (-> data (get "players") read-string inc))))
    
(defn lotrdb-save-quest [ req ]
  ; Main Save
  (let [uid (-> req model/get-authentications :uid)
        savedate (->> (-> req :form-params (get "date"))
                      (tf/parse (tf/formatter "yyyy-MM-dd"))
                      tc/to-long)
        fp (-> req :form-params clojure.walk/keywordize-keys)
        questdata (hash-map 
                    :questid    (-> fp :questid read-string)
                    :difficulty (-> fp :difficulty)
                    :players    (-> fp :players read-string)
                    :vp         (-> fp :vp read-string)
                    :turns      (-> fp :turns read-string)
                    ;:progressive (-> fp :progressive read-string)
                    :score      (-> fp :score read-string)
                    :uid uid
                    :date savedate
                  )
        deckdata (parsedeckdata (:form-params req))]
    (db/savequest questdata deckdata)
    (redirect "/lotrdb/questlog")))
    
(defroutes lotrdb-deck-routes 
  (GET "/fellowship" [] pages/fellowship)
  (GET "/fellowship/new" [] pages/fellowship)
  (GET "/fellowship/:id" [] pages/fellowship)
  (GET "/" [] pages/lotrdb-decks)
  (GET "/new" [] pages/lotrdb-deckbuilder)
  (GET "/edit" [] pages/lotrdb-deckbuilder)
  (GET "/edit/:id" [] pages/lotrdb-deckbuilder)
	(GET "/download/:id" [id] (response (model/o8dfile id 0))))
  
  
(defroutes lotrdb-quest-routes
  (GET "/" []
    pages/lotrdb-quest-page)
  (POST "/save" []
    lotrdb-save-quest)
  (POST "/delete" [qid]
    (db/delete-quest qid)
    (redirect "/lotrdb/questlog")))

(defroutes lotrdb-routes
  (GET "/" [req]
    pages/lotrdb-home)
  (GET "/api/data/:id" [id] 
		#(-> (model/lotrdb-api-data id %)
				json/write-str
				response
				(content-type "application/json")))
  (context "/decks" []
    ;lotrdb-deck-routes) 
    (friend/wrap-authorize lotrdb-deck-routes #{::db/user}))
  (GET "/packs" []
    pages/lotrdb-packs-page)
  (GET "/scenarios" []
    pages/lotrdb-scenarios-page)
  (GET "/search" []
    pages/lotrdb-search-page)
  (GET "/folders" []
    pages/lotrdb-folders)
  (context "/questlog" []
    ;lotrdb-quest-routes)
    (friend/wrap-authorize lotrdb-quest-routes #{::db/user}))
  (GET "/solo" []
    (friend/wrap-authorize pages/lotrdb-solo-page #{::db/user}))
  (GET "/cycle/:id" [ id ]
    #(pages/lotrdb-search-page (assoc-in % [:params :q] (str "y:" id))))
  (GET "/pack/:id" [ id ]
    #(pages/lotrdb-search-page (assoc-in % [:params :q] (str "e:" id)))) 
  (GET "/card/:id" [ id ]
    (pages/lotrdb-card-page id)))
    
    
;; AOSC ;;
    
(defroutes aosc-collection-routes 
  (GET "/" []
    pages/aosc-collection)
  (POST "/save" [collectionjson filterjson]
    #(save-collection-handler collectionjson filterjson %)))
    
(defroutes aosc-private-api-routes 
  (GET "/collection" []
    #(-> (db/get-user-collection (-> % model/get-authentications :uid))
         json/write-str
         response
         (content-type "application/json"))))
         
(defroutes aosc-api-routes 
  (context "/private" []
    (friend/wrap-authorize aosc-private-api-routes #{::db/user}))
  (GET "/data/cards" []
		(-> (model/aosc-api-cards)
				json/write-str
				response
				(content-type "application/json")))
  (GET "/deck/:id" [id]
    (let [deck (db/get-user-deck id)] 
      (content-type (response
        (-> deck  
           (dissoc :data :uid :author)
           (assoc :data (json/read-str (:data deck) :key-fn keyword))
           json/write-str)) "application/json"))))
           
(defroutes aosc-deck-routes 
  (GET "/"        [] pages/aosc-decks)
  (GET "/new"     [] pages/aosc-newdeck)
  (POST "/new"     [] pages/aosc-deckbuilder)
  (GET "/new/:id"  [] pages/aosc-deckbuilder)
  (GET "/edit/:id" [] pages/aosc-deckbuilder))
    
(defroutes aosc-routes
  (GET "/" [] pages/aosc-home)
  (context "/decks" [] 
    (friend/wrap-authorize aosc-deck-routes #{::db/user}))
  (context "/collection" [] 
    (friend/wrap-authorize aosc-collection-routes #{::db/user}))
  (context "/api" [] aosc-api-routes)
  (GET "/cards" []
    pages/aosc-cards-page)
  (GET "/cards/:id" [] ; Page with ALL the info from data.hits.hits + _source
    pages/aosc-card-page)
  (context "/cardlogin" []
    (friend/wrap-authorize (GET "/:id" [id] (redirect (str "/aosc/cards/" id))) #{::db/user})))
  
;; WHUW ;;
  
(defroutes whuw-deck-routes
  (GET "/"        [] pages/whuw-decks)
  (GET "/new"     [] pages/whuw-deckbuilder)
  (GET "/edit/:id" [] pages/whuw-deckbuilder))
  
(defroutes whuw-mortis-routes
  (GET "/"         [] pages/whuw-mortis-decks)
  (GET "/new"      [] pages/whuw-mortis-deckbuilder)
  (GET "/edit/:id" [] pages/whuw-mortis-deckbuilder))
  
  
  
(defroutes whuw-api-routes
  (GET "/cards" [] (-> (model/whuw_fullcards) json/write-str response (content-type "application/json")))
  (GET "/card/:id" [id]
    (-> (->> (model/whuw_fullcards) (filter #(= (:id %) id)) first)
        json/write-str
        response (content-type "application/json")))
  (GET "/champions" [] (-> model/whuwchamps json/write-str response (content-type "application/json")))
  (GET "/data"  [] (-> model/whuwdata json/write-str response (content-type "application/json")))
  (GET "/data/:id" [id]
    (-> (get model/whuwdata (keyword id)) json/write-str response (content-type "application/json")))
)
  
(defroutes whuw-routes
  (GET "/" [] pages/whuw-home)
  (context "/decks" [] 
    (friend/wrap-authorize whuw-deck-routes #{::db/user}))
  (context "/mortis" []
    whuw-mortis-routes)
    ;(friend/wrap-authorize whuw-mortis-routes #{::db/user}))    
  (GET "/cards"     [] pages/whuw-cards)
  (GET "/boards"    [] pages/whuw-boards)
  (context "/api" [] whuw-api-routes))
  
;; WHCONQ ;;
  
(defroutes whconq-deck-routes 
  (GET "/"        [] pages/whconq-decks)
  (GET "/new"     [] pages/whconq-newdeck)
  (ANY "/new/"    [] pages/whconq-deckbuilder)
  (GET "/new/:id"  [] pages/whconq-deckbuilder)
  (GET "/edit/:id" [] pages/whconq-deckbuilder)
  (GET "/download/:id" [id] (response (model/o8dfile id 3)))
)
  
(defroutes whconq-api-routes
  (GET "/deck/:id" [id]
    (let [deck (db/get-user-deck id)] 
      (content-type (response
        (-> deck  
           (dissoc :data :uid :author)
           (assoc :data (json/read-str (:data deck) :key-fn keyword))
           json/write-str)) "application/json")))
  (GET "/:id" [id]
    (try
      (-> (str "private/whconq/whconq_" id ".min.json")
          io/resource
          slurp
          response 
          (content-type "application/json"))
      (catch Exception e #(h/html5 pages/pretty-head [:body (pages/navbar %) [:div.container.my-3 [:h4 "Data not found"]]])))))
  
(defroutes whconq-routes
  (GET "/" [] pages/whconq-home)
  (GET "/cards"      [] pages/whconq-cards)
  (GET "/collection" [] pages/whconq-collection)
  (GET "/search"    []  (redirect "/whconq/find?q=e%3Acore"))
  (GET "/find"      [q] (pages/whconq-findcards q))
  (GET "/cycle/:id" [id]
    (let [cycle_code (->> model/whconq-cycle-data (filter #(= (:position %) (read-string id))) first :code)]
      (pages/whconq-findcards (str "e:" (->> model/whconq-pack-data (filter #(= (:cycle_code %) cycle_code)) (map :code) (clojure.string/join "|"))))))
  (GET "/pack/:id" [id] (pages/whconq-findcards (str "e:" id)))
  (GET "/card/:code{[0-9]+}" [code] (pages/whconq-cardpage code))
  (context "/decks" [] 
    ;whconq-deck-routes)
    (friend/wrap-authorize whconq-deck-routes #{::db/user}))
  (context "/api"   [] whconq-api-routes)
)

  
;; DECK ADMIN ;;
  
(defroutes deck-admin-routes
  (POST "/import" [name system data alliance tags notes]
    #(save-deck-handler {
      :id (db/unique-deckid)
      :system (read-string system)
      :decklist data 
      :name (if (empty? name) "Imported Deck" name) 
      :alliance alliance 
      :tags tags 
      :notes notes} %))
  (POST "/save" [id name system data alliance tags notes]
    #(save-deck-handler {
      :id (or id (db/unique-deckid "decklists"))
      :system (read-string system)
      :decklist data 
      :name (if (empty? name) "Imported Deck" name) 
      :alliance alliance 
      :tags tags 
      :notes notes} %))
  (POST "/fellowship/save" [id system name decks tags notes]
    #(let [uid (or id (db/unique-deckgroupid))]
      (db/save-deckgroup (hash-map :uid uid :system (or system 0) :name name :decks decks :tags tags :notes notes :author (-> % model/get-authentications :uid)))
      (response uid)))
  (POST "/fellowship/savedeck" [id system name data alliance tags notes uid]
    #(let [uid (or id (db/unique-deckid))]
      (db/save-deck uid (or system 0) name data alliance (or tags "") (or notes"") (-> % model/get-authentications :uid))
      (response uid)))
  (POST "/fellowship/delete" [uid name]
    (friend/wrap-authorize
      #(delete-group-handler uid name %)
      #{::db/user}))
  (POST "/delete" [uid name] 
    (friend/wrap-authorize 
      #(delete-deck-handler uid name %)
      #{::db/user})))
   
(defroutes staging-routes 
  (GET "/" []
    (friend/wrap-authorize pages/staging-page #{::db/user}))
  (POST "/" []
    #(let [data (-> % :params)]
      (if (contains? #{"deck" "collection"} (:type data))
        (db/stage-data data)
        (response nil))))
  (POST "/delete" [uid]
    (db/delete-staged-data uid)))
   
(defroutes app-routes
  (GET "/test" [] pages/testpage)
  (GET "/"     [] pages/home)
  (GET "/login"   [] pages/login)
  (context "/decks"   [] (friend/wrap-authorize deck-admin-routes #{::db/user}))
  (context "/lotrdb"  [] lotrdb-routes)
  (context "/aosc"    [] aosc-routes)
  (context "/whuw"    [] whuw-routes)
  (context "/whconq"  [] whconq-routes)
  (context "/admin"   [] (friend/wrap-authorize admin-routes #{::db/admin}))
  (context "/staging" [] staging-routes)
  ;(POST "/register" [username password]
  ;  (db/adduser username password false)
  ;  (redirect "/"))
  (POST "/checkusername" [username] 
    (response (str (some #(= (clojure.string/lower-case username) (clojure.string/lower-case %)) (map :username (db/get-users))))))
  (friend/logout
    (ANY "/logout" [] (redirect "/")))
  (resources "/"))

(def friend-authentication-hash {
  :allow-anon? true
  :login-uri "/login"
  :default-landing-uri "/"
  :unauthorised-handler (h/html5 [:body [:div.h5 "Access Denied " [:a {:href "/"} "Home"]]])
  :credential-fn #(creds/bcrypt-credential-fn (db/users) %)
  :workflows [(workflows/interactive-form)]
  })
  ;(fn [req]
  ;  (if (::friend/auth-config req)
  ;   (let [result ((workflows/interactive-form) req)]
  ;     (if (friend/auth? result)
  ;       (vary-meta result into {::friend/redirect-on-auth? (-> req :headers (get "referer"))})
  ;       result))
  ;   ( req)))]})
   
(def app
  (-> app-routes
    (friend/authenticate friend-authentication-hash)
    (wrap-keyword-params)
    (wrap-params)
    (wrap-session)
    (wrap-cors :access-control-allow-origin [#"https://api.jquery.com"]
               :access-control-allow-methods [:get :post] )
    ))