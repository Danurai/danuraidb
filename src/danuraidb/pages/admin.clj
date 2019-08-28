(in-ns 'danuraidb.pages)



(defn login [req]
  (h/html5
    (into pretty-head (h/include-js "/js/formvalidation.js?v=0.1"))
    [:body  
      (if-let [unauth (-> req :session :cemerick.friend/unauthorized-uri)]
        (case (re-find #"lotrdb|aosc|whuw|whkc" unauth)
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
      [:script {
        :src "https://cdnjs.cloudflare.com/ajax/libs/zxcvbn/4.4.2/zxcvbn.js" 
        :integrity "sha256-Znf8FdJF85f1LV0JmPOob5qudSrns8pLPZ6qkd/+F0o="
        :crossorigin "anonymous"}]]))
        
(defn useradmin [req]
  (h/html5
    (into pretty-head (h/include-js "/js/formvalidation.js?v=0.1"))
    [:body  
      [:div.container.mt-1
        [:h2.mt-2 "User Admin"]
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
                      [:button.btn.btn-warning [:i.fas.fa-edit.mr-1] "Reset"]]]
                ]])]]
      [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/zxcvbn/4.4.2/zxcvbn.js" 
               :integrity "sha256-Znf8FdJF85f1LV0JmPOob5qudSrns8pLPZ6qkd/+F0o=" 
               :crossorigin "anonymous"}]]))