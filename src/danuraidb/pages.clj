(ns danuraidb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [clj-time.core :as time]
    [clj-time.format :as tf]
    [clj-time.coerce :as tc]
    [clj-http.client :as client]
    [danuraidb.database :as db]
    [danuraidb.model :as model]))

  

  
(load "pages/common")
(load "pages/lotrdb") 
(load "pages/aosc")

(import (org.apache.commons.codec.binary Base64))

(defn imgBase64Str [ src ]
  (try 
    (str 
      "data:image/"
      (second (re-find #"\.(\w+)$" src))
      ";base64, "
      (String. (Base64/encodeBase64 (:body (client/get src {:as :byte-array})))))
    (catch Exception e src )))

(defn- card-url [ c ]
  (str 
    aosc_card_path
    (->> c :skus (filter :image) first :image)
    ".jpg"))

    (defn aosc-card-page [req]
      (let [id (-> req :params :id)
            src (->> (model/aosc-get-cards) (filter #(= (-> % :id str) id)) first)
            owned (db/get-user-collection (-> req model/get-authentications :uid))]
        (h/html5
          aosc-pretty-head
          [:body
            (aosc-navbar req)
            [:div.container.my-3
              [:div.row.my-3
                [:span.h4.m-auto.border-bottom 
                  [:span (:name src)]
                  (if (not-empty (:errata src)) [:a {:href "#errata"} [:small.text-muted.ml-2 "e"]])
                  ]]
              [:div.row
                [:div.col-sm-8
                  [:div.row
                    [:table.table.table-sm.table-hover
                      [:tbody
                        [:tr [:td "Alliance"][:td.h5 [:span.badge.badge-secondary [:a.text-white {:href (str "/aosc/cards?q=a:" (:alliance src))} (:alliance src)]]]]
                        [:tr [:td "Category"][:td [:a {:href (str "/aosc/cards?q=c:" (-> src :category :en))} (-> src :category :en)]]]
                        [:tr [:td "Class"][:td [:a {:href (str "/aosc/cards?q=l:" (-> src :class :en))} (-> src :class :en)]]]
                        [:tr [:td "Collector Info"][:td (-> src :collectorInfo)]]
                        [:tr 
                          [:td "Collection"]
                          [:td (if (nil? (-> req model/get-authentications :uid))
                                  [:span [:a {:href (str "/aosc/cardlogin/" id)} "Login"] " to see your collection info"]
                                  (for [[k v] (owned (keyword id))] [:span.mr-2 (str (-> k name clojure.string/capitalize) ": " v)]))]]
                        [:tr [:td "Corners"]
                          [:td 
                            (for [corner (->> src :corners)]
                              [:span
                                {:class (str "popover-corner-bg popover-corner-" (-> src :category :en clojure.string/lower-case) (if (:smooth corner) "smooth" "clunky"))}
                                (case (:value corner)
                                  ("Remove" "Heal" "Damage" "Spell" "Unit" "Ability") 
                                    [:img.popover-corner-image {:src (str aosc_icon_path "quest_" 
                                                   (-> corner :value clojure.string/lower-case) 
                                                   (if (some? (:qualifier corner)) (str "_" (-> corner :qualifier clojure.string/lower-case)))
                                                   ".png")}]
                                  "o" ""
                                  [:span.popover-corner-value (:value corner)])])]]
                        [:tr [:td "Tags"]
                          [:td 
                            (for [tag (:tags src)]
                              [:span.mr-2 
                                [:img.data-icon.mr-2 {:src (str aosc_icon_path "tag_" (clojure.string/lower-case tag) ".png")}]
                                [:a {:href (str "/aosc/cards?q=t:" tag)} (str tag)]])]]
                        [:tr [:td "Set"][:td [:a {:href (str "/aosc/cards?q=s:" (:setnumber src))} (str (-> src :set first :number) ": " (-> src :set  first :name))]]]
                        [:tr [:td "Rarity"][:td [:img.data-icon.mr-2 {:src (str aosc_icon_path "rarity_" (-> src :rarity clojure.string/lower-case) ".png")}]
                                              [:a {:href (str "/aosc/cards?q=r:" (:rarity src))} (:rarity src)]]]
                        [:tr [:td "Cost"][:td (:cost src)]]
                        [:tr [:td "Health"][:td (:healthMod src)]]
                        [:tr [:td "Effect"][:td (-> src :effect :en aosc-markdown)]]                    
                        [:tr 
                          [:td "Subject"]
                          [:td (if (some? (:subjectImage src)) 
                                [:img.subject-icon {:src (str aosc_icon_path "subject_" (:subjectImage src) ".png")}])]]
                      ; Additional info
                        [:tr [:td "id"][:td (:id src)]]
                        (if (not-empty (:errata src))
                          [:tr#errata [:td "Errata"][:td (for [e (:errata src)] [:div [:span.mr-2 "2020:"] [:span (:errata e)]])]])
                      ]]]]
                [:div.col-sm-4
                  ;[:img#cardimg.img-fluid {:src (or (-> src :imgurl first) (aosc-img-uri (str (->> src :skus (filter :image) first :id) ".jpg") "/img/aosc/cards/" aosc_card_path))}]
                  [:img#cardimg.img-fluid {:src (or (-> src :imgurl first) (imgBase64Str (card-url src)) )}]
                  (if (< 1 (->> src :skus (filter :image) count))
                    [:div.d-flex.justify-content-around
                      (map-indexed (fn [id sku]
                        [:span [:a.altlink {:href "#" :data-id (:id sku)} (str "#" (inc id))]]) (->> src :skus (filter :image)))])]]]
            [:script "$('.altlink').on('click',function (evt) {evt.preventDefault(); $('#cardimg').attr('src','/img/aosc/cards/' + $(this).data('id') + '.jpg');});"]])))
    

(load "pages/whuw")
(load "pages/whconq")
(load "pages/admin")

(defn home [req]
  (h/html5
    pretty-head
    [:body;
      (navbar req)
      [:div.container.my-3
        [:div.h5 "Deckbuilders"]
        (map (fn [{:keys [code desc icon]}]
          [:div [:a {:href (str "/" code "/decks") } [:img.mr-2 {:src icon :style "width: 1em"}] desc]]) model/systems)]]))
            
(defn staging-page [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container-fluid.my-3
        (toaster)
        [:div.row.h4 
          [:div.col "Staging"]]
        [:div.row.mb-2
          [:div.col
            [:div.btn-group.mr-2
              [:button#importselected.btn.btn-primary {:title "Import Selected"} [:i.fas.fa-file-upload]]
              [:button#deleteselected.btn.btn-danger {:title "Delete Selected" :data-toggle "modal" :data-target "#deletemodal"} [:i.fas.fa-trash-alt]]]]]
        [:div.row
          [:div.col
            [:table.table.table-sm {:style "table-layout: fixed;"}
              [:thead [:tr
                [:th {:style "width: 5%"} [:input#selectall.checkbox-md {:type "checkbox" :title "Select None/All"}]]
                [:th {:style "width: 10%"} "Type"]
                [:th {:style "width: 5%"} "Sys."]
                [:th {:style "width: 20%"} "Name"]
                [:th {:style "width: 60%"} "Data"]]]
              [:tbody
                (for [data (db/get-staged-data)]
                  [:tr
                    [:td.align-middle.align-center [:input.checkbox-md {:type "checkbox" :data-d (json/write-str data)}]]
                    [:td (-> data :type clojure.string/capitalize)]
                    [:td [:img.icon-sm {:src (->> model/systems (filter #(= (:id %) (-> data :system read-string))) first :icon)}]]
                    [:td (:name data)]
                    [:td {:style "overflow: hidden; white-space: nowrap;"} (-> data :decklist str)]])]]]]]
      [:div#deletemodal.modal {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h5.modal-title "Confirm Delete"]
              [:button {:type "button" :class "close" :data-dismiss "modal"} 
                [:span "x"]]]
            [:div.modal-body
              [:div.mb-2 "Are you sure you want to delete selected items?"]
              [:div.progress [:div.progress-bar {:role "progressbar"}]]]
            [:div.modal-footer
              [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
              [:button#deletedata.btn.btn-danger "Delete"]]]]]
      (h/include-js "/js/staging.js?v=0.1")
    ]))
                  
(defn testpage [ req ]
; lotr card img urls
  (let [crds (model/get-cards)]
    (h/html5
      lotrdb-pretty-head
      [:style ".card {width: 200px;}"]
      [:body 
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row.mb-2
            [:div.col
              [:div#types.btn-group.btn-group-toggle {:data-toggle "buttons"}
                (for [t (->> crds (map :type_code) distinct sort)]
                  [:label.btn.btn-outline-primary {:class (if (= t "quest") "active")}
                    [:input {:type "radio" :data-type_code t :name "typecode" }]
                    t])]]]
          [:div#cards.row.mb-2]
        ]
        [:div#cardmodal.modal.fade {:tabindex -1 :role "dialog"}
          [:div.modal-dialog.modal-lg {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4#cardname.modal-title]
                [:button.close {:type "button" :data-dismiss "modal"} [:span "&times;"]]]
              [:div#cardimg.modal-body]]]]
      (h/include-js "/js/lotrdb/cardurltest.js?v=1")])))