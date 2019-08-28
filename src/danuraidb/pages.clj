(ns danuraidb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [hiccup.page :as h]
    [cemerick.friend :as friend]
    [clj-time.core :as time]
    [clj-time.coerce :as tc]
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
        
                  
(load "pages/common")
(load "pages/lotrdb")
(load "pages/aosc")
(load "pages/whuw")
(load "pages/whconq")
(load "pages/admin")


        


(defn home [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        [:div.h5 "Deckbuilders"]
        (map (fn [{:keys [code desc]}]
          [:div [:a {:href (str "/" code "/decks") } desc]]) model/systems)]]))