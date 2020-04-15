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
        spheres [{:name "Leadership" :col "purple"}{:name "Lore" :col "green"}{:name "Spirit" :col "blue"}{:name "Tactics" :col "darkred"}{:name "Neutral" :col "slategrey"}{:name "Baggins" :col "goldenrod"}]]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:div.row
            [:div.col-lg-7 
              [:div.row
                [:nav
                  [:ol#spheres.breadcrumb
                    (for [s spheres]
                      [:li.breadcrumb-item {:data-code (-> s :name clojure.string/lower-case)  :style (str "cursor: pointer; color: " (:col s) ";") } (:name s)])]]]
              [:div.row
                [:nav [:ol#types.breadcrumb
                  (for [t card_types]
                    [:li.breadcrumb-item {:data-code (clojure.string/lower-case t):style "cursor: pointer;"} (clojure.string/capitalize t)])]]]
              [:div.row.d-flex.justify-content-between.mb-2
                [:div#pagetype] 
                [:div#pageno]
                [:div#pager.btn-group.btn-group-sm
                  [:button.btn.btn-outline-secondary {:value -1} "<<"]
                  [:button.btn.btn-outline-secondary {:value 1} ">>"]]]
              [:div#page.row.mb-3
                [:span "Loading..."]]]
            [:div.col-lg-5
              [:div#packs.list-group
                [:div.h4.text-center "Packs Owned"]
                (for [c cycles :let [cycle_id (str "cyc_" (:cycle_position c))]]
                  (if (= 1 (:cycle_position c)) ; Core
                    [:div.list-group-item
                      [:div.d-flex
                        [:span.h5.my-auto (:name c)]
                        [:div#coresets.ml-auto.btn-group.btn-group-sm.btn-group-toggle {:data-toggle "buttons"}
                          [:label#core1.btn.btn-outline-secondary.active
                            [:input {:name "corecount" :value "1" :type "radio" :checked true}] "1"]
                          [:label#core2.btn.btn-outline-secondary
                            [:input {:name "corecount" :value "2" :type "radio"}] "2"]
                          [:label#core3.btn.btn-outline-secondary
                            [:input {:name "corecount" :value "3" :type "radio"}] "3"]
                          ]]]
                    (let [pcks (->> packs (filter #(= (:cycle_position %) (:cycle_position c))))]
                      (if (= 1 (count pcks))
                        [:div.list-group-item 
                          [:div.d-flex
                            [:span.h5 (:name c)]
                            [:span.ml-auto [:input.pack {:type "checkbox" :id (str "pck_" (-> pcks first :id)) :data-code (-> pcks first :code)}]]]]
                        [:div.list-group-item 
                          [:div.d-flex
                            [:span.h5 (:name c)]
                            [:span.ml-auto [:input.cycle {:type "checkbox" :id cycle_id}]]]
                          (for [p pcks]                          
                            [:div.d-flex
                              [:span (:name p)]
                              [:span.ml-auto [:input.pack {:type "checkbox" :id (str "pck_" (:id p)) :data-code (:code p)}]]])]))))]]]]
      [:div#cardmodal.modal.fade {:tabindex -1 :role "dialog"}
        [:div.modal-dialog {:role "document"}
          [:div.modal-content
            [:div.modal-header
              [:h4#cardname.modal-title]
              [:button.close {:type "button" :data-dismiss "modal"} [:span "&times;"]]]
            [:div.modal-body
              [:div.row
                [:div#carddata.col-8]
                [:div.col-4 [:img#cardimg.img-fluid]]]]
            [:div#cardfooter.modal-footer.bg-light.p-1]]]]]
    (h/include-js "/js/lotrdb/lotrdb_folders.js?v=1")
    (h/include-css "/css/lotrdb-icomoon-style.css?v=1")
    )))
   
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
      (h/include-js "/js/lotrdb/cardurltest.js?v=1")
      ])))
   
(load "pages/aosc")    
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
                  
                  
                  