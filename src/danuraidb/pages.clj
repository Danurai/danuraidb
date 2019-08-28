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


        
(defn user-admin []
  [:div.my-3
    [:ul.list-group
      [:li.list-group-item ;"Add user"
        [:form.form-inline.justify-content-between.needs-validation {:action "admin/adduser" :method "post" :novalidate true}
          [:div.form-row.align-items-center
            [:div.col-auto
              [:input#username.form-control {:name "username" :type "text" :placeholder "Username" :required true}]
              [:div.invalid-feedback "Username Required"]]
            [:div.col-auto
              [:input#password.form-control {:name "password" :type "password" :placeholder "Password" :required true}]
              [:div.invalid-feedback "Password Required"]]
            [:div.col-auto
              [:input#password1.form-control {:name "confirm" :type "password" :placeholder "Password" :required true}]
              [:div.invalid-feedback "Password Required"]]
            [:div.col-auto
              [:div.form-check
                [:input.form-check-input {:name "admin" :type "checkbox"}]
                [:label.form-check-label "Admin"]]]]
          [:button.btn.btn-primary.float-right {:role "submit"} [:i.fas.fa-plus-circle.mr-1] "Create User"]]]
      (for [user (db/get-users)]
        ^{:key (:uid user)}[:li.list-group-item
            [:div.row-fluid.mb-1
              [:span.h3.mr-2 (:username user)]
              (if (or (true? (:admin user)) (= 1 (:admin user)))
                [:i.fas.fa-user-plus.text-primary.align-top]
                [:i.fas.fa-user.align-top])
                (if (not= (:uid user) 1001) 
                  [:form {:action "admin/deleteuser" :method "post"}
                    [:input {:type "text" :name "uid" :value (:uid user) :readonly true :hidden true}]
                    [:button.btn.btn-danger.float-right [:i.fas.fa-times.mr-1] "Delete"]])]
            [:div.row
              [:div.col-sm-2
                (if (not= (:uid user) 1001)
                  [:form.form-inline {:action "admin/updateadmin" :method "post"}
                    [:input {:type "text" :name "uid" :value (:uid user) :readonly true :hidden true}]
                    (if (or (true? (:admin user)) (= 1 (:admin user)))
                      [:button.btn.btn-danger {:role "submit"} [:i.fas.fa-minus-circle.mr-1] "Admin"]
                      [:button.btn.btn-primary {:role "submit" :name "admin"} [:i.fas.fa-plus-circle.mr-1] "Admin"])])]
              [:div.col-auto
                [:form.form-inline {:action "admin/updatepassword" :method "post"}
                  [:input.form-control {:name "uid" :value (:uid user) :readonly true :hidden true}]
                  [:div.form-group 
                    [:label.mr-2 "Reset password"]
                    [:input.form-control {:name "password" :type "password" :placeholder "new password"}]
                    [:input.form-control {:name "confirm" :type "password" :placeholder "confirm password"}]]
                  [:button.btn.btn-warning [:i.fas.fa-edit.mr-1] "Reset"]]]]])]])
                  
(defn aosc-site-admin []
  [:div.my-3
    [:div.nav.nav-tabs {:role "tablist"}
      [:a.nav-item.nav-link.active {:href "#aoscimgsearch" :data-toggle "tab" :role "tab"} "Image Search"]
      [:a.nav-item.nav-link {:href "#aoscdatatab" :data-toggle "tab" :role "tab"} "Source Data"]
      ]
    [:div.tab-content.my-3
      [:div#aoscimgsearch.tab-pane.fade.show.active {:role "tabpanel"}
        [:input#filter.form-control.mb-2 {:placeholder "TaffyDB filter"}]
        [:div#results]]
      [:div#aoscdatatab.tab-pane.fade {:role "tabpanel"}
        [:div.d-flex.mb-2
          [:div#datasrcinfo]
          [:button#copydata.btn.btn-secondary.ml-auto {:title "Copy to Clipboard"} [:i.fas.fa-clipboard]]]
        [:textarea#aoscdata.form-control {:rows 20}]]]
    (h/include-js "/js/aosc_admin.js?v=1")])
  
(defn admin-page [ req ]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        [:h3.row "Admin Page"]
        [:div.row 
          [:div.col-12
            [:ul.nav.nav-tabs.nav-fill {:role "tablist"}
              [:li.nav-item [:a.nav-link.active {:href "#useradmin" :data-toggle "tab" :role "tab"} "User Admin"]]
              [:li.nav-item [:a.nav-link {:href "#lotradmin" :data-toggle "tab" :role "tab"} "LotR Admin"]]
              [:li.nav-item [:a.nav-link {:href "#aoscadmin" :data-toggle "tab" :role "tab"} "AoSC Admin"]]
              [:li.nav-item [:a.nav-link {:href "#whuwadmin" :data-toggle "tab" :role "tab"} "WHUW Admin"]]
              [:li.nav-item [:a.nav-link {:href "#whconqadmin" :data-toggle "tab" :role "tab"} "WHConq Admin"]]]
            [:div.tab-content
              [:div#useradmin.tab-pane.fade.show.active {:role "tabpanel"} (user-admin)]
              [:div#lotradmin.tab-pane.fade {:role "tabpanel"}]
              [:div#aoscadmin.tab-pane.fade {:role "tabpanel"} (aosc-site-admin)]
              [:div#whuwadmin.tab-pane.fade {:role "tabpanel"}]
              [:div#whconqadmin.tab-pane.fade {:role "tabpanel"}]]]]]
      [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/zxcvbn/4.4.2/zxcvbn.js" :integrity "sha256-Znf8FdJF85f1LV0JmPOob5qudSrns8pLPZ6qkd/+F0o=" :crossorigin "anonymous"}]
      (h/include-js "/js/formvalidation.js?v=0.1")
      ]))

(defn home [req]
  (h/html5
    pretty-head
    [:body
      (navbar req)
      [:div.container.my-3
        [:div.h5 "Deckbuilders"]
        (map (fn [{:keys [code desc]}]
          [:div [:a {:href (str "/" code "/decks") } desc]]) model/systems)]]))