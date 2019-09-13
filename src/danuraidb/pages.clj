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
        (map (fn [{:keys [code desc icon]}]
          [:div [:a {:href (str "/" code "/decks") } [:img.mr-2 {:src icon :style "width: 1em"}] desc]]) model/systems)]]))
          
(defn testpage [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        (toaster)
        [:div.row
          [:div.col-12
            [:div (str @model/alert)]]]]]))
            
(defn staging-page [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        (toaster)
        [:div.row.h4 "Staging"]
        [:div.row.mb-2
          [:div.btn-group.mr-2
            [:button#importselected.btn.btn-primary {:title "Import Selected"} [:i.fas.fa-file-upload]]
            [:button#deleteselected.btn.btn-danger {:title "Delete Selected" :data-toggle "modal" :data-target "#deletemodal"} [:i.fas.fa-trash-alt]]
            [:button#selectall.btn.btn-warning {:title "Select All"} [:i.fas.fa-check-square]]]]
        [:div.row
          [:table.table.table-sm
            [:thead [:tr [:th "Select"][:th "Type"][:th "System"][:th "Name"][:th "Data"]]]
            [:tbody
              (for [data (db/get-staged-data)]
                [:tr 
                  [:td [:input {:type "checkbox" :data-id (:uid data)}]]
                  [:td (-> data :type clojure.string/capitalize)]
                  [:td [:img.icon-sm {:src (->> model/systems (filter #(= (:id %) (-> data :system read-string))) first :icon)}]] ;[:img.icon-sm {:src (->> model/systems (filter (= :id (-> data :system read-string))) first :icon)}]]
                  [:td (:name data)]
                  [:td.w-25 {:style "text-overflow: hidden;"} (-> data :data str)]])]]
          [:span (db/get-staged-data)]]]
      [:div#deletemodal.modal {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h5.modal-title "Confirm Delete"]
              [:button {:type "button" :class "close" :data-dismiss "modal"} 
                [:span "x"]]]
            [:div.modal-body
              [:div.mb-2 "Are you sure you want to delete selected decks?"]
              [:div.progress [:div.progress-bar {:role "progressbar"}]]]
            [:div.modal-footer
              [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
              [:button#deletedata.btn.btn-danger "Delete"]]]]]
      (h/include-js "/js/staging.js?v=0.1")
    ]))
                  
                  
                  