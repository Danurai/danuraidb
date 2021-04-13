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
(load "pages/whuw")

(defn yauwdb [ req ]
  (let [ sets     (->> model/whuwdata2 :sets (map (fn [[k, v]] v)) (sort-by :id)) 
         factions (->> model/whuwdata2 :factions (map (fn [[k v]] v)) (sort-by :id)) 
         cards    (->> model/whuwdata2 :cards (map (fn [[k v]] v)) (sort-by :id))  ]
    (h/html5
      pretty-head
      [:body.text-light {:style "background-color: #222;"}
        (whuw-navbar req)
        [:div.container-fluid.my-3
          [:div#info.bg-dark.w-100 {:style "position: fixed; bottom: 0px; left: 0px; padding-left: 1rem; z-index: 99;"}
            [:small
              [:span.mr-1 "Warhammer Underworlds is &#169; "]
              [:a.mr-1 {:href="https://warhammerunderworlds.com/"} "Games Workshop."]
              [:span.mr-1 "Warband images and databases courtesy of"] 
              [:a.mr2 {:href "https://github.com/PompolutZ/yawudb"} "https://github.com/PompolutZ/yawudb"]
              [:a.mr1 {:href "https://yawudb.com/"} "yawudb.com"]
             ]]
          [:div.container.mb-2
            [:div.d-flex
              [:div#filteroptions.btn-group.btn-group-toggle.mr-2 {:data-toggle "buttons"}
                [:label.btn.btn-secondary.active [:input#opt-faction {:type "radio" :name "option" :checked true} "Faction"]]
                [:label.btn.btn-secondary [:input#opt-set {:type "radio" :name "option"} "Set"]]
              ]
              [:select#faction.form-control.mr-2.bg-dark.text-light
                (for [ faction factions ]
                  [:option (:displayName faction)])]
              [:select#set.form-control.mr-2.bg-dark.text-light {:style "display: none;"}
                (for [ set sets ]
                  [:option (:displayName set)])]
            ]]
          [:div#faction-members]
          [:div
            [:ul.nav.nav-tabs {:role "tablist"}
              [:li.nav-item {:role "presentation"}
                [:button#faction-tab.nav-link.btn-secondary.active {:data-toggle "tab" :data-target "#faction-cards" :type "button" :role "tab"} "Faction Cards" ] ]
              [:li.nav-item {:role "presentation"}
                [:button#set-tab.nav-link.btn-secondary {:data-toggle "tab" :data-target "#set-cards" :type "button" :role "tab"} "Set Cards"]]]
            [:div.tab-content
              [:div#faction-cards.tab-pane.fade.show.active.py-3 {:role "tabpanel"}]
              [:div#set-cards.tab-pane.fade.py-3 {:role "tabpanel"}]]]]
        [:div#card-modal.modal {:tabindex -1 :role "modal"}
          [:div.modal-dialog {:role "document"}
            [:div.modal-content {:style "border: none;"}
              [:div.modal-body.bg-dark.rounded]]]]
      ]
      (h/include-js "/js/whuw/whuw_yauwdbdata.js")
      (h/include-css "/css/whuw-style.css"))))

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