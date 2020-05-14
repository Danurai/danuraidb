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

(defn lotrdb-scenarios-page [ req ]
	(let [cards (model/get-cards-with-cycle)]
		(h/html5
			lotrdb-pretty-head
			[:body  
				(lotrdb-navbar req)
        [:div.container.my-3
          [:ul.list-group
            (for [s (model/get-scenarios)]
              (let [quests (->> cards (filter #(= (:type_code %) "quest")) (filter #(= (:encounter_name %) (:name s))))] 
                [:li.list-group-item 
                  [:div.row.justify-content-between
                    [:span.h4 (:name s)
                      [:i.fas.fa-chart-pie.ml-2.fa-xs.text-secondary {
                        :style "cursor: pointer;" 
                        :data-target "#modaldata" :data-toggle "modal"
                        :data-quest-id (:id s)
                        }]] ;quests}]]
                    [:span
                      [:span.mr-2.align-middle (-> quests first :pack_name)]
                      (lotrdb-card-icon (first quests))]]
                  [:div.row
                    [:div.col-sm-6
                      [:h5 "Quests"]
                      (for [q quests]
                        [:div [:a.card-link {:href (str "/lotrdb/card/" (:code q)) :data-code (:code q)} (:name q)]])]
                    [:div.col-sm-6 
                      [:h5
                        [:a {:href (str "/lotrdb/search?q=n:" (->> s :encounters (map :name) (clojure.string/join "|")))}
                        "Encounter Sets"]]
                      ; assumed Encounter set always includes encounter pack with a matching name
                      [:div [:a {:href (str "/lotrdb/search?q=n:" (clojure.string/replace (:name s) " " "+"))} (:name s)]]
                      (for [e (sort-by :id (:encounters s))]
                        (if (not= (:name s) (:name e))
                          [:div [:a {:href (str "/lotrdb/search?q=n:" (clojure.string/replace (:name e) " " "+"))} (:name e)]]))]]]))]]
        [:div#modaldata.modal.fade {:tab-index -1 :role "dialog"}
          [:div.modal-dialog.modal-lg {:role "document"}
            [:div.modal-content
              [:div.modal-header
                [:h4.modal-title.w-100 ""]
                [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
                  [:span {:aria-hidden "true"} "\u00d7"]]]
              [:div.modal-body
                [:div.d-flex.mb-3
                  [:h5.my-auto "Difficulty:"
                    [:span#difftxt.mx-2 "Normal"]]
                  [:div.ml-2.btn-group.btn-group-sm 
                    [:button#diffdown.btn.btn-outline-secondary {:style "line-height: 1em;"} [:i.fas.fa-caret-left.fa-xs]]
                    [:button#diffup.btn.btn-outline-secondary {:style "line-height: 1em;"} [:i.fas.fa-caret-right.fa-xs]]]]
                [:div.row
                  [:div.col-6
                    [:canvas#piechart {:width "200" :height "200"}]
                    ]
                  [:div.col-6
                    [:canvas#barchart {:width "200" :height "200"}]
                    ]]]]]]
        (h/include-js "https://cdn.jsdelivr.net/npm/chart.js@2.9.3/dist/Chart.min.js")
        (h/include-js "https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@0.7.0")
        (h/include-js "/js/lotrdb/lotrdb_scenarios.js?v=0.1")]))) 


(defn lotrdb-quest-page [ req ]
  (let [scenarios (model/get-scenarios) dateformatter (tf/formatter "yyyy-MM-dd")]
    (h/html5
      lotrdb-pretty-head
      [:body
        (lotrdb-navbar req)
        [:div.container.my-3
          [:form.mb-3 {:action "/lotrdb/questlog/save" :method "POST"}
            [:div.form-row
              [:div.form-group.col-lg-4
                [:label "Quest"]
                [:input#questid {:name "questid" :hidden true :readonly true :value 1}]
                [:select#scenario.form-control
                  (for [s scenarios]
                    [:option (:name s)])]]
              [:div.form-group.col-lg-2
                [:label "Difficulty"]
                [:select#difficulty.form-control {:name "difficulty"}
                  [:option "Easy"][:option {:selected true} "Normal"][:option "Nightmare"]]]
              [:div.form-group.col-lg-1
                [:label "# Players"]
                [:select#players.form-control {:name "players"} (for [n (range 1 5)] [:option (str n)])]]
              [:div.form-group.col-lg-1
                [:label "VP"]
                [:input#vp.form-control {:name "vp" :value 0 :min 0 :type "number"}]]
              [:div.form-group.col-lg-1
                [:label "# Turns"]
                [:input#turns.form-control {:name "turns" :value 1 :min 1 :type "number"}]]
              [:div.form-group.col-lg-2
                [:label "Date"]
                [:input#date.form-control {:name "date"   :type "Date" :value (tf/unparse dateformatter (time/now))}]]
              [:div.form-group.col-lg-1
                [:h5.text-center "Score"]
                [:input#score {:name "score" :hidden true :readonly true}]
                [:h4#scoreshown.pt-2.text-center "40"]]]
            [:div#plyrstats
              [:datalist#decklists] ;(for [dl decklists] [:option {:value (:name dl)}])]
              (for [n (range 1 5) :let [p (str "p" n)]]
                [:div.form-row {:hidden (> n 1) :id (str p "stats")}
                  [:div.form-group.col-lg-3
                    [:label (str "Player " (last p) " Deck Name")]
                    [:input.form-control {:name (str p "deckname") :type "text" :list "decklists" :id (str p "deckname")}]
                    [:input {:name (str p "decklist") :hidden true :readonly true :id (str p "decklist")}]]
                  [:div.form-group.col-lg-1
                    [:label "Spheres"]
                    [:div.pt-2 {:name (str p "spheres") :id (str p "spheres")}]]
                  [:div.form-group.col-lg-2
                    [:label "Dead Hero Threat"]
                    [:input.form-control {:name (str p "deadh") :type "number" :value 0 :min 0 :id (str p "deadh")}]]
                  [:div.form-group.col-lg-2
                    [:label "Damage on Heroes"]
                    [:input.form-control {:name (str p "dmgh") :type "number" :value 0 :min 0 :id (str p "dmgh")}]]
                  [:div.form-group.col-lg-2
                    [:label "Final Threat"]
                    [:input.form-control {:name (str p "threat") :type "number" :value 30 :min 0 :max 50 :id (str p "threat")}]]
                  [:div.form-group.col-lg-1.offset-lg-1
                    [:h5.text-center "Subtotal"]
                    [:input {:name (str p "score") :id (str p "score") :hidden true :readonly true :value 30}]
                    [:h5.text-center {:id (str p "scoreshown")} "30"]]
                ])]
            [:div.form-row
              [:button#savequest.btn.ml-auto.btn-primary [:i.fas.fa-feather.mr-1] "Save"]]]]
        [:div.row
          [:div.col
            [:div.list-group
              (for [q (db/get-quests (-> req model/get-authentications :uid))]
                [:li.list-group-item (str q)])]]]
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