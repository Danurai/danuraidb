(in-ns 'danuraidb.pages)

(defn login [req]
  (let [refer (or (-> req :session :cemerick.friend/unauthorized-uri) (-> req :headers (get "referer")))]
    (h/html5
      pretty-head
      [:body  
        (if refer
          (case (re-find #"lotrdb|aosc|whuw|whconq" refer)
            "lotrdb" (lotrdb-navbar req)
            "aosc" (aosc-navbar req)
            "whuw" (whuw-navbar req)
            "whconq" (whconq-navbar req)
            [:navbar.nav.nav-dark.bg-dark])
          [:navbar.nav.nav-dark.bg-dark])
        ; TODO Failed Login
        [:div.container
          [:div.row.my-2
            [:div.col-sm-6.mx-auto
              [:ul.nav.nav-tabs.nav-fill {:role "tablist"}
                [:li.nav-item [:a.nav-link.active {:data-toggle "tab" :role "tab" :href "#logintab"} "Login"]]
                [:li.nav-item [:a.nav-link {:data-toggle "tab" :role "tab" :href "#registertab"} "Register"]]]
              [:div.tab-content.my-2
                [:div#logintab.tab-pane.fade.show.active {:role "tabpanel"}
                  [:form {:action "login" :method "post"}
                    [:div.form-group
                      [:label {:for "username"} "Name"]
                      [:input.form-control {:type "text" :name "username" :placeholder "Username" :auto-focus true}]]
                    [:div.form-group
                      [:label {:for "password"} "Password"]
                      [:input.form-control {:type "password" :name "password" :placeholder "Password"}]]
                    [:button.btn.btn-warning.mr-2 {:type "submit"} "Login"]]]
                [:div#registertab.tab-pane.fade {:role "tabpanel"}
                  [:form.needs-validation.was-validated {:action "register" :method "post" :novalidate true}
                    [:div.form-group
                      [:label {:for "username"} "User Name"]
                      [:input#username.form-control {:type "text" :name "username" :placeholder "Username" :auto-focus true :required true}]
                      [:div.invalid-feedback "Username is required"]]
                    [:div.form-group
                      [:labeltext-muted {:for "email"} "Email"]
                      [:input#email.form-control {:type "text" :name "email" :placeholder "Email" :auto-focus true}]]
                    [:div.form-group
                      [:label {:for "password"} "Password"]
                      [:div.input-group
                        [:input#password.form-control {:type "password" :name "password" :placeholder "password" :auto-focus true :required true}]
                        [:div.invalid-feedback "Password is required"]]
                      [:small [:meter#pwdscoremeter.mr-1 {:value 0 :min 0 :max 5 :low 2 :high 4 :optimum 5}][:span#pwdscoretxt "Very Weak"]]]
                    [:div.form-group
                      [:label {:for "password1"} "Confirm Password"]
                      [:input#password1.form-control {:type "password" :name "password1" :placeholder "password" :auto-focus true :required true}]
                      [:div.invalid-feedback "Passwords required, passwords must match"]]
                    [:button.btn.btn-warning.mr-2.disabled {:type "submit"} "Register"]]]]]]]
        (h/include-js "/js/formvalidation.js?v=0.1")
        [:script {
          :src "https://cdnjs.cloudflare.com/ajax/libs/zxcvbn/4.4.2/zxcvbn.js" 
          :integrity "sha256-Znf8FdJF85f1LV0JmPOob5qudSrns8pLPZ6qkd/+F0o="
          :crossorigin "anonymous"}]])))
        
(defn user-admin []
  [:div.my-3
    [:ul.list-group
      [:li.list-group-item ;"Add user"
        [:form.form-inline.justify-content-between.needs-validation {:action "admin/adduser" :method "post" :novalidate true}
          [:div.form-row.align-items-center
            [:div.col-auto.mb-auto
              [:input#username.form-control {:name "username" :type "text" :placeholder "Username" :required true}]
              [:div.invalid-feedback "Username Required"]
              [:div.valid-feedback "Good to go"]]
            [:div.col-auto.mb-auto
              [:input#password.form-control {:name "password" :type "password" :placeholder "Password" :required true}]
              [:div.invalid-feedback "Password Required"]]
            [:div.col-auto.mb-auto
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
                    ;[:button.btn.btn-danger.float-right [:i.fas.fa-times.mr-1] "Delete"]
                    ])]
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
        [:div.mb-2
          [:div.btn-group-toggle {:data-toggle "buttons"}
            [:label.btn.btn-outline-dark.active.mr-1
              [:input#tblcard {:type "radio" :name "tblopt" :checked true} "Cards"]]
            [:label.btn.btn-outline-dark.mr-1
              [:input#tbltags {:type "radio" :name "tblopt"} "Tags"]]
            [:label.btn.btn-outline-dark.mr-1
              [:input#tblcorners {:type "radio" :name "tblopt"} "Corners"]]
            [:label.btn.btn-outline-dark.mr-1
              [:input#tblsubjects {:type "radio" :name "tblopt"} "Subjects"]]
            [:label.btn.btn-outline-dark.mr-1
              [:input#tblalliances {:type "radio" :name "tblopt"} "Alliances+"]]]]
        [:div#results]]
      [:div#aoscdatatab.tab-pane.fade {:role "tabpanel"}
        [:div.d-flex.mb-2
          [:div#datasrcinfo]
          [:button#copydata.btn.btn-secondary.ml-auto {:title "Copy to Clipboard"} [:i.fas.fa-clipboard]]]
        [:textarea#aoscdata.form-control {:rows 20}]]]
    (h/include-js "/js/aosc/aosc_admin.js?v=2")])
    
(defn whuw-site-admin []
  (let [carddata model/whuwdata]
    [:div.my-3
      [:ul.nav.nav-tabs.nav-justified {:role "tablist"}
        [:li.nav-item [:a.nav-link {:data-toggle "tab" :href "#warbands"} "Warbands"]]
        [:li.nav-item [:a.nav-link {:data-toggle "tab" :href "#sets"} "Sets"]]
        [:li.nav-item [:a.nav-link {:data-toggle "tab" :href "#cardtypes"} "Card-types"]]
        [:li.nav-item.active [:a.nav-link.active.show {:data-toggle "tab" :href "#cards"} "Cards"]]]
      [:div.tab-content.my-3
        [:div#warbands.tab-pane.fade {:role "tabpanel"}
          [:table.table.table-sm
            [:thead]
            [:tbody]]]
        [:div#sets.tab-pane.fade {:role "tabpanel"}
          [:table.table.table-sm
            [:thead]
            [:tbody]]]
        [:div#cardtypes.tab-pane.fade {:role "tabpanel"}
          [:table.table.table-sm
            [:thead]
            [:tbody]]]
        [:div#cards.tab-pane.fade.active.show {:role "tabpanel"}
          [:form.form-inline.mb-2
            [:label.mr-2 "Set Filter"]
            [:select#selectset.selectpicker {:multiple true :data-width "fit"}
              (for [item (:sets carddata)]
                (let [imgtag (str "<img class=\"icon-sm\" src=\"/img/whuw/icons/" (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]
            [:label.mx-2 "Warband Filter"]
            [:select#selectwarband.selectpicker {:multiple true :data-width "fit"}
              (for [item (:warbands carddata)]
                (let [imgtag (str "<img class=\"icon-sm\" src=\"/img/whuw/icons/" (-> item :icon :filename) "\" title=\"" (:name item) "\"></img>")]
                ^{:key (gensym)}[:option {:data-content imgtag :data-subtext (:name item) } (:id item)]))]]
          [:table.table.table-sm
            [:thead
              [:tr 
                [:th "code"]
                [:th "gw_id"]
                [:th "name"]
                [:th "Set"]
                [:th "Warband"]
                [:th "Rule"]
                [:th "Glory"]
                [:th "filename"]
                [:th "url"]
                [:th "img"]
                ]]
            [:tbody]]]
            ]
        (h/include-js "/js/whuw/whuw_admin.js?v=2") ;newdata.js
        ]))
  
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
              [:li.nav-item [:a.nav-link {:href "#aoscadmin" :data-toggle "tab" :role "tab"} "AoSC Admin"]]
              ;[:li.nav-item [:a.nav-link {:href "#lotradmin" :data-toggle "tab" :role "tab"} "LotR Admin"]]
              [:li.nav-item [:a.nav-link {:href "#whuwadmin" :data-toggle "tab" :role "tab"} "WHUW Admin"]]
              ;[:li.nav-item [:a.nav-link {:href "#whconqadmin" :data-toggle "tab" :role "tab"} "WHConq Admin"]]
              ]
            [:div.tab-content
              [:div#useradmin.tab-pane.fade.show.active {:role "tabpanel"} (user-admin)]
              [:div#aoscadmin.tab-pane.fade {:role "tabpanel"} (aosc-site-admin)]
              ;[:div#lotradmin.tab-pane.fade {:role "tabpanel"}]
              [:div#whuwadmin.tab-pane.fade {:role "tabpanel"} (whuw-site-admin)]
              ;[:div#whconqadmin.tab-pane.fade {:role "tabpanel"}]
              ]]]]
      [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/zxcvbn/4.4.2/zxcvbn.js" :integrity "sha256-Znf8FdJF85f1LV0JmPOob5qudSrns8pLPZ6qkd/+F0o=" :crossorigin "anonymous"}]
      (h/include-js "/js/formvalidation.js?v=0.1")
      ]))