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
    [danuraidb.database :as db]
    [danuraidb.model :as model]))
                  
(load "pages/common")
(load "pages/lotrdb")  



(defn lotrdb-quest-page [ req ]
  (let [scenarios (model/get-scenarios) dateformatter (tf/formatter "yyyy-MM-dd")]
    (h/html5
      lotrdb-pretty-head
      [:body {:style "color: maroon"}
        (lotrdb-navbar req)
        [:div.container.my-3
          [:form.mb-3 {:action "/lotrdb/questlog/save" :method "POST"}
            [:div.form-row
              [:input#id {:name "id" :hidden true :readonly true}]
              [:div.form-group.col-4
                [:label "Quest"]
                [:input#questid {:name "questid" :hidden true :readonly true :value 1}]
                [:select#scenario.form-control
                  (for [s scenarios]
                    [:option (:name s)])]]
              [:div.form-group.col-2
                [:div [:label.mr-1 "Difficulty"] [:i.fas.fa-mountain]]
                [:select#difficulty.form-control {:name "difficulty"}
                  [:option "Easy"][:option {:selected true} "Normal"][:option "Nightmare"]]]
              [:div.form-group.col-1
                [:label "# Players"]
                [:select#players.form-control {:name "players"} (for [n (range 1 5)] [:option (str n)])]]
              [:div.form-group.col-2
                [:label "Date"]
                [:input#date.form-control {:name "date"   :type "Date" :value (tf/unparse dateformatter (time/now))}]]]
            [:div.form-row
              [:div.col-4 [:label "Player Deck Name"]]
              [:div.col-1 [:label "Spheres"]]
              [:div.col-2 [:label "Final Threat"]]
              [:div.col-2 [:label "Threat of Dead Heroes"]]
              [:div.col-2 [:label "Damage on Heroes"]]
              [:div.col-1 [:label "Subtotal"]]]
            [:div#plyrstats
              [:datalist#decklists] ;(for [dl decklists] [:option {:value (:name dl)}])]
              (for [n (range 1 5) :let [p (str "p" n)]]
                [:div.form-row.mb-1 {:hidden (> n 1) :id (str p "stats")}
                  [:div.col-4
                    [:div.input-group
                      [:input.form-control {:name (str p "deckname") :type "text" :list "decklists" :id (str p "deckname")}]
                      [:div.input-group-append
                        [:button.btn.btn-secondary {:type "button" :data-plyr-id n :data-toggle "modal" :data-target "#modaldecklist"} [:i.fas.fa-plus]]]]
                    [:input {:name (str p "decklist") :hidden true :readonly true :id (str p "decklist")}]]
                  [:div..col-1
                    [:div.pt-2 {:name (str p "spheres") :id (str p "spheres")}]]
                  [:div.col-2
                    [:input.form-control {:name (str p "threat") :type "number" :value 30 :min 0 :max 50 :id (str p "threat")}]]
                  [:div.col-2
                    [:input.form-control {:name (str p "deadh") :type "number" :value 0 :min 0 :id (str p "deadh")}]]
                  [:div.col-2
                    [:input.form-control {:name (str p "dmgh") :type "number" :value 0 :min 0 :id (str p "dmgh")}]]
                  [:div.col-1
                    [:input {:name (str p "score") :id (str p "score") :hidden true :readonly true :value 30}]
                    [:h5.text-center {:id (str p "scoreshown")} "30"]]
                ])]
            [:div.form-row
              [:div.form-group.col-2.offset-7
                [:div [:label.mr-2 "VP"] [:i.far.fa-star]]
                [:input#vp.form-control {:name "vp" :value 0 :min 0 :type "number"}]]
              [:div.form-group.col-2
                [:span [:label.mr-2 "# Turns"] [:i.far.fa-clock]]
                [:input#turns.form-control {:name "turns" :value 1 :min 1 :type "number"}]]
              [:div.form-group.col-1
                [:h5.text-center "Score"]
                [:input#score {:name "score" :hidden true :readonly true :value 40}]
                [:h3#scoreshown.pt-2.text-center "40"]]]
            [:div.d-flex.justify-content-end
              [:button#resetquest.btn.btn-secondary.mr-2 {:type "button"} [:i.fas.fa-eraser.mr-1] "New/Reset"]
              [:button#savequest.btn.btn-warning [:i.fas.fa-feather.mr-1] "Save"]]]
          [:div.row
            [:div.col
              [:div#savedquests.list-group
                (for [q (->> (-> req model/get-authentications :uid) db/get-quests (sort-by :id >) (sort-by :date >))
                      :let [questdecks (db/get-quest-decks (:id q))]]
                  [:li.list-group-item 
                    [:div.d-flex
                      [:h4.mr-3 (->> scenarios (filter #(= (:id %) (:questid q))) first :name)]
                      [:span.mt-1 
                        [:i.fas.fa-mountain.mr-1] [:span.mr-2 (:difficulty q)]
                        [:i.far.fa-clock.mr-1] [:span.mr-2 (:turns q)]
                        [:i.far.fa-star.mr-1] [:span (:vp q)]]
                      [:h4.ml-auto [:h4 (:score q)]]]
                    [:div.d-flex 
                      (for [d questdecks] 
                        [:div.mr-3 
                          [:span.mr-2 
                            {:data-decklist (:decklist d) :data-toggle "modal" :data-target "#qlmodal" :style "cursor: pointer;"}
                            (:deckname d)]
                          [:span (str "(" (:score d) ")")]])]
                    [:div.d-flex.justify-content-between
                      [:div.text-muted.mt-auto
                        [:small.mr-2 (tf/unparse (tf/formatter "dd-MMM-yyyy") (tc/from-long (:date q)))]
                        [:small (str "#" (:id q))]]
                      [:div 
                        [:button.btn.btn-primary.btn-sm.mr-1.btn-edit {:data-quest (str q) :data-questdecks (str questdecks) } [:i.fas.fa-edit.mr-1] "Edit"]
                        [:button.btn.btn-danger.btn-sm.mr-2.btn-delete {:data-qid (:id q) :data-toggle "modal" :data-target "#qlmodal"} [:i.fas.fa-times.mr-1] "Delete"]]]])]]]]
        [:div#qlmodal.modal.fade {:tab-index -1 :role "dialog"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4.modal-title ""]
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"} [:span {:aria-hidden "true"} "\u00d7"]]]
              [:div.modal-body]
              [:div.modal-footer]]]]
        [:div#modaldecklist.modal.fade {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-lg {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4.modal-title.w-100 "Decklist"]
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                  [:span {:aria-hidden "true"} "\u00d7"]]]
              [:div.modal-body
                [:div.row.mb-2
                  [:div.col
                    [:label.mr-1 "Deck Name"]
                    [:input#mdeckname.form-control.mb-2 {:type "text"}]
                    [:label "Add cards"]
                    [:div.d-flex.mb-2
                      [:input#mcardname.typeahead.form-control {:type "text"}]
                      [:div#mcardqty.btn-group.btn-group-sm.ml-2
                        (for [n (range 1 4)] [:button.btn.btn-outline-secondary.mqtybtn {:value n} n])]]
                    ;[:textarea#mdecklist.form-control {:rows 10}]
                    [:div#mdecklistpretty {:style "min-height:200px;"}]
                    [:input#mparsedecklist {:hidden true :readonly true}]]]]
              [:div.modal-footer
                [:input#mpnum {:hidden true :readonly true :value 0}]
                [:button.btn.btn-secondary {:data-dismiss "modal"} "Close"]
                ;[:button.btn.btn-Danger "Update Saved Deck"]
                [:button#mdecksave.btn.btn-primary "Save"]]]]]

        (h/include-js "/js/externs/typeahead.js")
        (h/include-css "/css/lotrdb-icomoon-style.css?v=1")
        (h/include-js "/js/lotrdb/lotrdb_questlog.js?v=0.1")
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