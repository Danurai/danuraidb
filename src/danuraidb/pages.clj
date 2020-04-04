(ns danuraidb.pages
  (:require 
    [clojure.data.json :as json]
    [clojure.java.io :as io]
    [clojure.string :as string]
    [cemerick.friend :as friend]
    [hiccup.page :as h]
    [clj-time.core :as time]
    [clj-time.coerce :as tc]
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
                  
(load "pages/common")
(load "pages/lotrdb")

(defn lotrdb-folders [ req ]
  (let [card_types ["hero" "ally" "attachment" "event"]
        cycles (drop-last (model/get-cycles))
        packs  (model/get-packs)
        cards  (model/get-cards) ;(filter #(contains (set card_types) (:type_code %) (model/get-cards)))
        spheres [{:name "Leadership" :col "purple"}{:name "Lore" :col "green"}{:name "Spirit" :col "blue"}{:name "Tactics" :col "darkred"}{:name "Neutral" :col "slategrey"}]]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row
            [:div.col-lg-7 
              [:div.row
                [:ol.breadcrumb
                  (for [s spheres]
                    [:li.breadcrumb-item {:style (str "color: " (:col s) "; cursor: pointer;") } (:name s)] ;:color (str (:col s))
                    )]]] 
            [:div.col-lg-5
              [:div.list-group
                [:div.h4.text-center "Packs Owned"]
                (for [c cycles]
                  (if (= 1 (:cycle_position c)) ; Core
                    [:div.list-group-item
                      [:div.d-flex
                        [:span.h5.my-auto (:name c)]
                        [:div.ml-auto.btn-group.btn-group-sm.btn-group-toggle {:data-toggle "buttons"}
                          [:label.btn.btn-outline-secondary.active
                            [:input#core1 {:name "corecount" :type "radio" :checked true}] "1"]
                          [:label.btn.btn-outline-secondary
                            [:input#core2 {:name "corecount" :type "radio"}] "2"]
                          [:label.btn.btn-outline-secondary
                            [:input#core3 {:name "corecount" :type "radio"}] "3"]
                          ]]]
                    (let [pcks (->> packs (filter #(= (:cycle_position %) (:cycle_position c))))]
                      [:div.list-group-item 
                        [:div.d-flex
                          [:span.h5 (:name c)]
                          [:span.ml-auto [:input {:type "checkbox" :id (str "cyc_" (:cycle_position c))}]]]
                        (if (< 1 (count pcks))
                          (for [p pcks]                          
                            [:div.d-flex
                              [:span (:name p)]
                              [:span.ml-auto [:input {:type "checkbox" :id (str "pck_" (:id p))}]]]))])))]]]
          ]])))
      
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
                  
                  
                  