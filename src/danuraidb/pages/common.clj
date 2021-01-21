(in-ns 'danuraidb.pages)

(defn- nav-link [ req root link ]
  (let [uri (str "/" root "/" (-> link (clojure.string/lower-case) (clojure.string/replace #"\s" "")))]
    [:li.nav-item 
      [:a.nav-link {
        :href uri
        :style "text-transform: capitalize;"
        :class (if (= uri (:uri req)) "active")}  link]]))
        
;; ---- Match part of URI for sub-menus e.g. decks/new "search|physical|digital"
     
(defn- dropdown-link [ req & args ]
  (let [subroot (str "/" (clojure.string/join "/" args))]
    [:a.dropdown-item {:href subroot}  (-> args last clojure.string/capitalize)]))
        
(defn navbar 
  ([ iconsrc title root links req & {:keys [style]}]
    [:nav.navbar.navbar-expand-lg.navbar-dark.bg-dark {:style style}
      [:div.container-fluid
      ;; Home Brand with Icon
        [:div.nav-item.dropdown
          [:a..navbar-brand.h1.mb-0.dropdown-toggle {:href "#" :role "button" :data-toggle "dropdown"} 
            [:img.mr-1 {:src iconsrc :style "width: 1em;"}] title]
          [:div.dropdown-menu
            (map (fn [s]
              [:a.dropdown-item {:href (str "/" (:code s) "/decks")} [:img.mr-1 {:src (:icon s) :style "width: 1em;"}] (:desc s)]) model/systems)
            [:a.dropdown-item {:href "/staging"} [:i.fas.fa-file-upload.text-primary.mr-2] "Staging"]]]
      ;; Collapse Button for smaller viewports
        [:button.navbar-toggler {:type "button" :data-toggle "collapse" :data-target "#navbarSupportedContent" 
                              :aria-controls "navbarSupportedContent" :aria-label "Toggle Navigation" :aria-expanded "false"}
          [:span.navbar-toggler-icon]]
      ;; Collapsable Content
        [:div#navbarSupportedContent.collapse.navbar-collapse
      ;; List of Links
          [:ul.navbar-nav.mr-auto
            (for [link links :let [dd (clojure.string/split link #"\|")]]
              (if (-> dd count (> 1))
                  [:li.nav-item.dropdown
                    [:a.nav-link.dropdown-toggle {:href "#" :data-toggle "dropdown"} (-> dd first clojure.string/capitalize)]
                    [:div.dropdown-menu
                      (for [sublink (rest dd) :let [subroot (str "/" root "/" (first dd) "/" sublink)]]
                        (dropdown-link req root (first dd) sublink))]]
                  (nav-link req root (clojure.string/lower-case link))))]
      ;; Login Icon
            [:span.nav-item.dropdown
              [:a#userDropdown.nav-link.dropdown-toggle.text-white {:href="#" :role "button" :data-toggle "dropdown" :aria-haspopup "true" :aria-expanded "false"}
                [:i.fas.fa-user]
                (if-let [identity (friend/identity req)]
                  [:span.h5.mx-2 (:current identity)])]
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
    [:script {:src "https://code.jquery.com/jquery-3.5.1.min.js" :integrity "sha256-9/aliU8dGd2tb6OSsuzixeV4y/faTqgFtohetphbbj0=" :crossorigin "anonymous"}]
  ;; popper tooltip.js
    [:script {:src "https://cdn.jsdelivr.net/npm/popper.js@1.16.1/dist/umd/popper.min.js" :integrity "sha384-9/reFTGAW83EW2RDu2S0VKaIzap3H66lZH81PoYlFhbGU+6BZp6G7niu735Sk7lN" :crossorigin "anonymous"}]
  ;; Bootstrap  
    [:script {:src "https://cdn.jsdelivr.net/npm/bootstrap@4.6.0/dist/js/bootstrap.min.js" :integrity "sha384-+YQ4JLhjyBLPDQt//I+STsc9iw4uQqACwlvpslubQzn4u2UU2UFM80nGisd026JF" :crossorigin "anonymous"}]
    [:link {:rel "stylesheet" :href "https://cdn.jsdelivr.net/npm/bootstrap@4.6.0/dist/css/bootstrap.min.css" :integrity "sha384-B0vP5xmATw1+K9KRQjQERJvTumQW0nPEzvF6L/Z6nronJ3oUOFUFpCjEUQouq2+l" :crossorigin "anonymous"}]
  ;; Bootstrap Select
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.8/css/bootstrap-select.css" :integrity "sha256-OejstTtgpWqwtX/gwUbICEQz8wbdVWpVrCwqZ29apg4=" :crossorigin "anonymous"}]
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/bootstrap-select/1.13.8/js/bootstrap-select.js" :integrity "sha256-/X1l5JQfBqlJ1nW6V8EhZJsnDycL6ocQDWd531nF2EI=" :crossorigin "anonymous"}]
  ;; Font Awesome
    [:script {:defer true :src "https://use.fontawesome.com/releases/v5.13.0/js/all.js"}]
  ;; TAFFY JQuery database
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/taffydb/2.7.2/taffy-min.js"}]
  ;; Showdown markup 
    [:script {:src "https://cdnjs.cloudflare.com/ajax/libs/showdown/1.9.0/showdown.min.js" :integrity "sha256-LSUpTY0kkXGKvcBC9kbmgibmx3NVVgJvAEfTZbs51mU=" :crossorigin "anonymous"}]
  ;; font 
    [:link {:href "https://fonts.googleapis.com/css?family=Eczar|Exo+2" :rel "stylesheet"}]
  ;; Site Specific
    (h/include-css "/css/danuraidb-style.css?v=1")
    ])
    
(defn optgroup-togglenone
  ([group id]
    (optgroup-togglenone group id nil))
  ([group id default]
    [:div.btn-group.btn-group-toggle.btn-group-toggle-none.mb-1 {:id id}
      (map (fn [btn]
            [:label.btn.btn-outline-secondary {:title (:name btn) :class (if (= default (:name btn)) "active")}
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
            [:label.btn.btn-outline-secondary {:title (:name btn) :class (if (= default (:name btn)) "active")}
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
            [:label.btn.btn-outline-secondary {:title (:name btn) :class (if (= default (:name btn)) "active")}
              [:input {:id (:name btn) :type "radio" :name id :checked (= default (:name btn))} 
                (if (some? (:img btn))
                  [:img.button-icon {:src (:img btn)}]
                  (:symbol btn))]]) group)]))

(defn- deletemodal []
  [:div#deletemodal.modal {:tabindex -1 :role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header
          [:h5.modal-title "Confirm Delete"]
          [:button {:type "button" :class "close" :data-dismiss "modal"} 
            [:span "x"]]]
        [:div.modal-body]
        [:div.modal-footer
          [:button.btn.btn-primary {:data-dismiss "modal"} "Cancel"]
          [:form {:action "/decks/delete" :method "post"}
            [:input#deletemodaldeckname {:name "name" :hidden true}]
            [:input#deletemodaldeckuid {:name "uid" :hidden true}]
            [:button.btn.btn-danger {:submit "true"} "OK"]]]]]])
            
(defn- importdeckmodal []
  [:div#importdeck.modal {:role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header 
          [:h5 "Load Deck"]
          [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
            [:span {:aria-hidden "true"} "x"]]]
        [:div.modal-body
          [:div.mb-2 "Paste Decklist or Sharing Code below"]
          [:input#importdeckname.form-control.mb-2 {:placeholder "Deck Name"}]
          [:textarea#importdecklist.form-control {:rows "10"}]]
        [:div.modal-footer
          [:form {:action "/decks/import" :method "post"}
            [:input#deckname {:hidden true :name "name" :value "Imported Deck"}]
            [:input#decksystem {:hidden true :name "system"}]
            [:input#deckdata {:hidden true :name "data"}]
            [:button.btn.btn-primary {:type "submit"} "Load Deck"]]
          [:button.btn.btn-secondary {:type "button" :data-dismiss "modal"} "Close"]]]]])
          
(defn- importallmodal []
  [:div#importallmodal.modal {:tabindex -1 :role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header 
          [:div.modal-title "Import from JSON: [{name: \"name\" deck: \"sharing code\"}]"]
          [:button {:type "button" :class "close" :data-dismiss "modal"} [:span "x"]]]
        [:div.modal-body
          [:textarea#importalldata.form-control.mb-2 {:rows "5"}]
          [:div.progress
            [:div.progress-bar {:role "progressbar"}]]]
        [:div.modal-footer
          [:button#importallsubmit.btn.btn-primary "Load"]
          [:button.btn.btn-secondary {:type "button" :data-dismiss "modal"} "Close"]]]]] )
          
(defn- exportdeckmodal []
  [:div#exportdeck.modal {:role "dialog"}
    [:div.modal-dialog {:role "document"}
      [:div.modal-content
        [:div.modal-header "Export Deck:" [:span.ml-1]
          [:button.close {:type "button" :data-dismiss "modal" :aria-label "Close"}
            [:span {:aria-hidden "true"} "x"]]]
        [:div.modal-body 
          [:textarea.form-control.mb-2 {:rows "10"}]
          [:div.input-group {:title "Copy sharing code to clipboard" :style "cursor: pointer;"}
            [:input#sharecode.form-control {:type "input" :style "cursor: pointer;"}]
            [:div.input-group-append
              [:span.input-group-text [:i.fas.fa-clipboard]]]]]
        [:div.modal-footer
          [:button.btn.btn-secondary {:type "button" :data-dismiss "modal"} "Close"]]]]])

(defn- toast [{:keys [type msg]}]
  [:div.toast {:role "alert" :aria-live "assertive" :aria-atomic "true" :data-delay 5000 :style "min-width: 200px;"}
    [:div.toast-header 
      (case type
        "fatal" [:i.fas.fa-times.text-danger.mr-2]
        "warning" [:i.fas.fa-exclamation.text-warning.mr-2]
        [:i.fas.fa-exclamation.text-primary.mr-2])
      [:b.mr-auto (clojure.string/capitalize type)]
      [:button.close {:role "button" :data-dismiss "toast" :aria-label "Close"} [:span {:aria-hidden true} "&times;"]]]
   [:div.toast-body
    [:span msg]]])
          
(defn- toaster []
  (when-let [alerts @model/alert]
    (reset! model/alert [])
    [:div#toaster {:style "position: fixed; top: 10px; right: 10px; z-index: 1050"}
      (for [a alerts]
        (toast a))
      [:script "$('.toast').toast('show');"]]))