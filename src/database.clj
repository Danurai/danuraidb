(ns danuraidb.database
  (:require 
    [clojure.java.io :as io]
    [clojure.data.json :as json]
    [clojure.java.jdbc :as j]
    [clj-time.core :as t]
    [clj-time.coerce :as c]
    [cemerick.friend :as friend]
      (cemerick.friend [credentials :as creds])))

; Role Hierarchy
(derive ::admin ::user)

; Define sqlite for local, or system (postgresql)
(def db (or (System/getenv "DATABASE_URL")
            {:classname   "org.sqlite.JDBC"
             :subprotocol "sqlite"
             :subname     "resources/db/db.sqlite3"}))
; Local postgresql for testing
;(def db {:dbtype "postgresql" :dbname "danuraidb" :host "localhost" :port "5432" :user "danuraidbadmin" :password "admin"})
             
(def tcreate {
  :users {
    :sqlite     [[:uid      :integer :primary :key :AUTOINCREMENT]
                [:username :text]
                [:password :text]
                [:admin    :boolean]
                [:active   :boolean]
                [:created  :date]]
    :postgresql [[:uid      :int :default "nextval ('user_uid_seq')"]
                [:username :text]
                [:password :text]
                [:admin    :boolean]
                [:active   :boolean]
                [:created  :bigint]]}
  :staging {
    :sqlite     [[:uid      :integer :primary :key :AUTOINCREMENT]
                 [:system   :text]
                 [:type     :text]
                 [:name     :text]
                 [:decklist :text]
                 [:alliance :text]
                 [:tags     :text]
                 [:notes    :text]
                 [:uploaded :date]]
    :postgresql [[:uid      :int :default "nextval ('staging_uid_seq')"]
                 [:system   :text]
                 [:type     :text]
                 [:name     :text]
                 [:decklist :text]
                 [:alliance :text]
                 [:tags     :text]
                 [:notes    :text]
                 [:uploaded :bigint]]}
   :systems [[:id   :integer :primary :key]
            [:code :text]
            [:desc :text]]
   :decklists [[:uid         :text :primary :key]
              [:system      :text]
              [:name        :text]
              [:author      :integer]
              [:data        :text]
              [:alliance    :text]
              [:tags        :text]
              [:notes       :text]
              [:created     :bigint]
              [:updated     :bigint]
              ;["FOREIGN KEY (author) REFERENCES users(uid)"]
              ]
   :aosccollection [[:owner :integer] [:collection :text]] ; ["FOREIGN KEY (owner) REFERENCES users(uid)"]]
   :version [[:major :int] [:minor :int] [:note :text] [:released :bigint]]
})

      
(defn updateuseradmin [uid admin]
  (j/db-do-commands db [(str "update users set admin=" admin " where uid=" uid)]))
;  (j/update! db :users {:admin (= admin 1)}
;           ["uid = ?" uid]))
  
(defn updateuserpassword [uid password]
  (j/db-do-commands db [(str "update users set password='" (creds/hash-bcrypt password) "' where uid=" uid)]))
;  (j/update! db :users {:password (creds/hash-bcrypt password)}
;           ["uid = ?" (int uid)]))
           
(defn adduser [username password admin]
  (j/insert! db :users 
    {:username username
     :password (creds/hash-bcrypt password)
     :admin admin
     :active true
     :created (if (= (:subprotocol db) "sqlite")
                (t/now)
                (c/to-long (t/now)))}))

(defn dropuser [uid]
  (j/delete! db :decklists ["author = ?" uid])
  (j/delete! db :users ["uid = ?" uid]))
  

;(def tupdate {
;  :users {
;    :vn {
;      "1.0" nil}}})
      
(defn- create-tbl-users []
  (let [sp (keyword (get db :subprotocol "postgresql"))
        timestamp (if (= sp :postgresql) (c/to-long (t/now)) (t/now))]
    (prn "db subprotocol" sp)
    (try
      (if (= sp :postgresql) (j/db-do-commands db ["create sequence user_uid_seq minvalue 1000"]))
      (j/db-do-commands db 
        (j/create-table-ddl :users (-> tcreate :users sp)))
      (if (= sp :sqlite) (j/insert! db :sqlite_sequence {:name "users" :seq 1000}))
      (j/insert! db :users {:username "root" :password (creds/hash-bcrypt "admin") :active true :admin true  :created timestamp})
      (j/insert! db :users {:username "dan"  :password (creds/hash-bcrypt "user")  :active true :admin false :created timestamp})
      (catch Exception e (println (str "DB Error - Users: " e ))))))
      
(defn- create-tbl-staging []
  (let [sp (keyword (get db :subprotocol "postgresql"))]
    (try
      (if (= sp :postgresql) (j/db-do-commands db ["create sequence staging_uid_seq minvalue 1000"]))
      (j/db-do-commands db   (j/create-table-ddl :staging (-> tcreate :staging sp)))
      (if (= sp :sqlite)     (j/insert! db :sqlite_sequence {:name "staging" :seq 1000}))
      (catch Exception e (println (str "DB Error - Staging: " e ))))))
      
(defn- create-tbl-custom [ tname schema ]
  (try
    (j/db-do-commands db
      (j/create-table-ddl tname schema))
    (catch Exception e (str "Table create error: " tname e))))

                              
(defn create-db []
  (if (= (:protocol db) "sqlite")
    (j/db-do-commands db ["PRAGMA foreign_keys = ON;"]))
  (create-tbl-users)
  (create-tbl-staging)
  (doseq [[tname schema] (dissoc tcreate :users :staging)]
    (create-tbl-custom tname schema)))
  
;;;;;;;;
; USER ;
;;;;;;;;
           
(defn users []
  (->> (j/query db ["select * from users"])
       (map (fn [x] (hash-map (:username x) (-> x (dissoc :admin)(assoc :roles (if (or (= 1 (:admin x)) (true? (:admin x))) #{::admin} #{::user}))))))
       (reduce merge)))

(defn get-users []
  (j/query db ["SELECT uid, username, admin FROM users"]))

;;;;;;;;;
; DECKs ;
;;;;;;;;;
  
(defn update-or-insert!
  "Updates columns or inserts a new row in the specified table"
  [db table row where-clause]
  (j/with-db-transaction [t-con db]
    (let [result (j/update! t-con table row where-clause)]
      (if (zero? (first result))
        (j/insert! t-con table row)
        result))))

(defn rnd-deckid []
  (->> (fn [] (rand-nth (seq (char-array "ABCDEF0123456789"))))
       repeatedly
       (take 6)
       (apply str)))
(defn deck-ids []
  (map (fn [x] (:uid x))(j/query db ["SELECT uid FROM decklists"])))      
(defn unique-deckid []
; get all deck IDs to compare to
  (let [uids (deck-ids)]
    (loop []
      (let [uid (rnd-deckid)]
        (if (.contains uids uid)
          (recur)
          uid)))))
            
(defn save-deck [id system name decklist alliance tags notes uid]
;; TODO SYSTEM
  (let [deckid (if (clojure.string/blank? id) (unique-deckid) id)
        qry    {
          :uid deckid 
          :system system 
          :name name 
          :data (str decklist) 
          :alliance alliance 
          :tags tags 
          :notes notes 
          :author uid 
          :updated (c/to-long (t/now))}
        where-clause ["uid = ?" deckid]]
    (j/with-db-transaction [t-con db]
      (let [result (j/update! t-con :decklists qry where-clause)]
        (if (zero? (first result))
          (j/insert! t-con :decklists (assoc qry :created (c/to-long (t/now)) :updated (c/to-long (t/now))))
          result)))))
    
(defn get-user-decks [ system uid ]
  ;(j/query db [(str "SELECT * FROM decklists WHERE author = '" uid "' AND system = '" system "' ORDER BY UPDATED DESC" ) ]))
  (j/query db ["SELECT * FROM decklists WHERE author = ? AND system = ? ORDER BY UPDATED DESC" uid (str system)]))
  
(defn get-user-deck [ deckuid ]
  (first (j/query db ["SELECT * FROM decklists WHERE uid = ?" deckuid])))  
  
(defn delete-deck [ deckuid ]
  (j/delete! db  :decklists ["uid = ?" deckuid]))
  
;;;;;;;;;;;;;;
; COLLECTION ;
;;;;;;;;;;;;;;

(defn get-user-collection [uid]
  (-> (j/query db ["SELECT collection FROM aosccollection WHERE owner = ?" uid])
       first
       (:collection "{}")
       (json/read-str :key-fn keyword)))
      
(defn save-user-collection [collection-json uid]
  (update-or-insert! db :aosccollection {:owner uid :collection collection-json} ["owner =?" uid]))
  
;;;;;;;;;;;
; Staging ;
;;;;;;;;;;;

(defn stage-data [data]
  (let [sp (keyword (get db :subprotocol "postgresql"))]
    (j/insert! db :staging (assoc data :uploaded  (if (= sp :postgresql) (c/to-long (t/now)) (t/now)) ))))
        
(defn get-staged-data []
  (j/query db ["SELECT * FROM staging ORDER BY uid DESC"]))
  
(defn delete-staged-data [ uid ]
  (j/delete! db :staging ["uid = ?" (read-string uid)]))