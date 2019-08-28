(in-ns 'danuraidb.pages)

(defn get-authentications [req]
  (#(-> (friend/identity %) :authentications (get (:current (friend/identity %)))) req))

(defn- nav-link [ req uri title ]
  [:li.nav-item 
    [:a.nav-link {
      :href uri
      :class (if (= uri (:uri req)) "active")}  title]])
;; TODO Match part of URI for sub-menus e.g. decks/new

(defn navbar 
  ([ iconsrc title root links req & {:keys [style]}]
    [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark {:style style}
      [:div.container
      ;; Home Brand with Icon
        [:div.nav-item.dropdown
          [:a..navbar-brand.h1.mb-0.dropdown-toggle {:href "#" :role "button" :data-toggle "dropdown"} 
            [:img.mr-1 {:src iconsrc :style "width: 1em;"}] title]
          [:div.dropdown-menu
            (map (fn [s]
              [:a.dropdown-item {:href (str "/" (:code s) "/decks")} (:desc s)]) model/systems)]]
      ;; Collapse Button for smaller viewports
        [:button.navbar-toggler {:type "button" :data-toggle "collapse" :data-target "#navbarSupportedContent" 
                              :aria-controls "navbarSupportedContent" :aria-label "Toggle Navigation" :aria-expanded "false"}
          [:span.navbar-toggler-icon]]
      ;; Collapsable Content
        [:div#navbarSupportedContent.collapse.navbar-collapse
      ;; List of Links
          [:ul.navbar-nav.mr-auto
            (for [link links]
              (nav-link req (str "/" root "/" (clojure.string/lower-case link)) (clojure.string/capitalize link)))]
      ;; Login Icon
            [:span.nav-item.dropdown
              [:a#userDropdown.nav-link.dropdown-toggle.text-white {:href="#" :role "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
                [:i.fas.fa-user]]
                (if-let [identity (friend/identity req)]
                  [:div.dropdown-menu {:aria-labelledby "userDropdown"}
                    (if (friend/authorized? #{::db/admin} (friend/identity req))
                      [:a.dropdown-item {:href "/admin"} "Admin Console"])
                    [:a.dropdown-item {:href "/logout"} "Logout"]]
                  [:div.dropdown-menu {:aria-labelledby "userDropdown"}
                    [:a.dropdown-item {:href "/login"} "Login"]])]]]])
  ([req] (navbar "/img/danuraidb.png" "DanuraiDB" "/" [] req)))

(def pretty-head
  [:head
  ;; Meta Tags
    [:meta {:charset "UTF-8"}]
    [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
  ;; icon
    [:link {:rel "icon" :href "/img/danuraidb.png"}]
  ;; jquery
    [:script {
      :src "https://code.jquery.com/jquery-3.3.1.min.js" 
      :integrity "sha256-FgpCb/KJQlLNfOu91ta32o/NMZxltwRo8QtmkMRdAu8=" 
      :crossorigin "anonymous"}]
  ;; popper tooltip.js
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/tooltip.js/1.3.1/umd/tooltip.min.js" :integrity "sha256-5hYn1dYaPW5VRitzMTQ8UsMvqSPqCiqwtQbT77tyEso=" :crossorigin="anonymous"}]
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.14.6/umd/popper.min.js" :integrity "sha256-WHwIASWxNdKakx7TceUP/BqWQYMcEIfeLNdFMoFfRWA=" :crossorigin "anonymous"}]
  ;; Bootstrap  
    [:link {:rel "stylesheet" :href "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css" :integrity "sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T" :crossorigin "anonymous"}]
    [:script {:src "https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/js/bootstrap.min.js" :integrity "sha384-JjSmVgyd0p3pXB1rRibZUAYoIIy6OrQ6VrjIEaFf/nJGzIxFDsf4x0xIM+B07jRM" :crossorigin "anonymous"}]
  ;; Bootstrap Select
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.8/css/bootstrap-select.css" :integrity "sha256-OejstTtgpWqwtX/gwUbICEQz8wbdVWpVrCwqZ29apg4=" :crossorigin "anonymous"}]
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.8/js/bootstrap-select.js" :integrity "sha256-/X1l5JQfBqlJ1nW6V8EhZJsnDycL6ocQDWd531nF2EI=" :crossorigin "anonymous"}]
  ;; Font Awesome
    [:script {:defer true :src "https://use.fontawesome.com/releases/v5.4.0/js/all.js"}]
  ;; TAFFY JQuery database
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/taffydb/2.7.2/taffy-min.js"}]
  ;; Showdown markup 
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/showdown/1.9.0/showdown.min.js" :integrity "sha256-LSUpTY0kkXGKvcBC9kbmgibmx3NVVgJvAEfTZbs51mU=" :crossorigin "anonymous"}]
  ;; font 
    [:link {:href "https://fonts.googleapis.com/css?family=Eczar" :rel "stylesheet"}]
  ;; Site Specific
    (h/include-css "/css/danuraidb-style.css?v=1")
    ])
    


(defn show-alert []
  (let [type (-> @model/alert :type)
        msg  (-> @model/alert :message)]
    (when (some? type)
      (reset! model/alert {})
      [:div.alert.alert-dismissible.fade.show {:class type :role "alert"} msg
        [:button.close {:type"button" :data-dismiss "alert" :aria-label "Close"} [:span {:aria-hidden "true"} "&#10799;"]]])))

(defn optgroup-togglenone
  ([group id]
    (optgroup-togglenone group id nil))
  ([group id default]
    [:div.btn-group.btn-group-toggle.btn-group-toggle-none.mb-1 {:id id}
      (map (fn [btn]
            [:label.btn.btn-outline-primary {:title (:name btn) :class (if (= default (:name btn)) "active")}
              [:input {:id (:name btn) :checked (= default (:name btn)) :type "checkbox"}
                (if (some? (:img btn))
                    [:img.button-icon {:src (:img btn)}]
                    (:symbol btn))]]) group)]))
    
(defn btngroup 
  ([group id]
    (btngroup group id nil))
  ([group id default]
    [:div.btn-group.btn-group-toggle.mb-1 {:id id :data-toggle "buttons"}
      (map (fn [btn]
            [:label.btn.btn-outline-primary {:title (:name btn) :class (if (= default (:name btn)) "active")}
              [:input {:id (:name btn) :data-code (:code btn) :checked (= default (:name btn)) :type "checkbox"}
                (if (some? (:img btn))
                    [:img.button-icon {:src (:img btn)}]
                    (:symbol btn))]]) group)]))
              
(defn optgroup 
  ([group id]
    (optgroup group id nil))
  ([group id default]
    [:div.btn-group.btn-group-toggle.mb-1 {:id id :data-toggle "buttons"}
      (map (fn [btn]
            [:label.btn.btn-outline-primary {:title (:name btn) :class (if (= default (:name btn)) "active")}
              [:input {:id (:name btn) :type "radio" :name id :checked (= default (:name btn))} 
                (if (some? (:img btn))
                  [:img.button-icon {:src (:img btn)}]
                  (:symbol btn))]]) group)]))


(defn- markdown [ txt ]
  txt
)