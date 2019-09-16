(ns danuraidb.web
   (:require 
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [compojure.core :refer [context defroutes GET ANY POST]]
    [compojure.route :refer [resources]]
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
  (db/save-deck id system name decklist alliance tags notes (-> req pages/get-authentications :uid))
  (alert "info" [:span [:b name] " saved."])
  (do-redirect req))
  
(defn- delete-deck-handler [ uid name req ]
  (db/delete-deck uid)
  (alert "warning" [:span [:b name] " deleted."])
  (do-redirect req))
  
(defn- save-collection-handler [collectionjson filterjson req]
  (db/save-user-collection collectionjson (-> req pages/get-authentications :uid))
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
    
(defroutes lotrdb-deck-routes 
  (GET "/" [] pages/lotrdb-decks)
  (GET "/new" [] pages/deckbuilder)
  (GET "/edit" [] pages/deckbuilder)
  (GET "/edit/:id" [] pages/deckbuilder))

(defroutes lotrdb-routes
  (GET "/" [req]
    pages/lotrdb-home)
  (GET "/api/data/:id" [id] 
		(-> (model/lotrdb-api-data id)
				json/write-str
				response
				(content-type "application/json")))
  (context "/decks" [] 
    (friend/wrap-authorize lotrdb-deck-routes #{::db/user}))
  (GET "/packs" []
    pages/lotrdb-packs-page)
  (GET "/scenarios" []
    pages/lotrdb-scenarios-page)
  (GET "/search" []
    pages/lotrdb-search-page)
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
    #(-> (db/get-user-collection (-> % pages/get-authentications :uid))
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
  ;(GET "/new/:id"  [] pages/aosc-deckbuilder)
  (GET "/edit/:id" [] pages/whuw-deckbuilder))
  
(defroutes whuw-routes
  (GET "/" [] pages/whuw-home)
  (context "/decks" [] 
    (friend/wrap-authorize whuw-deck-routes #{::db/user}))
  (GET "/cards" [] pages/whuw-cards)
  (GET "/api/data" [] (-> "private/whuw_data_r2.json" io/resource slurp response (content-type "application/json")))
  (GET "/api/cards" [] (-> (model/whuw_fullcards) json/write-str response (content-type "application/json"))))
  
;; WHCONQ ;;
  
(defroutes whconq-deck-routes 
  (GET "/" [] pages/whconq-decks)
  )
  
(defroutes whconq-api-routes
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
  (context "/decks" [] whconq-deck-routes)
  (context "/api"   [] whconq-api-routes))
  
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
      :id id
      :system (read-string system)
      :decklist data 
      :name (if (empty? name) "Imported Deck" name) 
      :alliance alliance 
      :tags tags 
      :notes notes} %))
  (POST "/delete" [uid name] 
    (friend/wrap-authorize 
      #(delete-deck-handler uid name %)
      #{::db/user})))
   
(defroutes app-routes
  (GET "/test" [] pages/testpage)
  (GET "/"     [] pages/home)
  (GET "/login" [] pages/login)
  (context "/decks"   [] (friend/wrap-authorize deck-admin-routes #{::db/user}))
  (context "/lotrdb"  [] lotrdb-routes)
  (context "/aosc"    [] aosc-routes)
  (context "/whuw"    [] whuw-routes)
  (context "/whconq"  [] whconq-routes)
  (context "/admin"   [] (friend/wrap-authorize admin-routes #{::db/admin}))
  (GET "/staging" []
    (friend/wrap-authorize pages/staging-page #{::db/user}))
  (POST "/staging" []
    #(let [data (-> % :params)]
      (if (contains? #{"deck" "collection"} (:type data))
        (db/stage-data data)
        (response nil))))
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
  :workflows [(workflows/interactive-form)]})
  
(def app
  (-> app-routes
    (friend/authenticate friend-authentication-hash)
    (wrap-keyword-params)
    (wrap-params)
    (wrap-session)
    (wrap-cors :access-control-allow-origin [#".*"]
               :access-control-allow-methods [:get :post] )
    ))
