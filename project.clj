(defproject danuraidb "0.1.0-SNAPSHOT"
  :description "danuraidb"
  :url "https://github.com/Danurai/danurai.github.io"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :min-lein-version "2.7.1"
  
  :main         danuraidb.system

  :jar-name     "danuraidb.jar"
  :uberjar-name "danuraidb-standalone.jar"
  
  :repl-options {:init-ns user
                 :timeout 120000}
  
  :dependencies [[org.clojure/clojure "1.10.0"]
                [org.clojure/clojurescript "1.10.520"]
                [org.clojure/core.async  "0.3.443"]
                ; Web server
                [http-kit "2.3.0"]
                [com.stuartsierra/component "0.3.2"]
                ; routing
                [compojure "1.6.0"]
                [ring/ring-defaults "0.3.1"]
                [ring-cors "0.1.11"]
                [clj-http "3.7.0"]
                ; Websocket sente
                ; [com.taoensso/sente "1.12.0"]
                ; page rendering
                [hiccup "1.0.5"]
								[reagent "0.7.0"]
                [cljs-http "0.1.46"]
                ; user management
                [com.cemerick/friend "0.2.3"]
                ; Databasing
                [org.clojure/java.jdbc "0.7.5"]
                [org.xerial/sqlite-jdbc "3.7.2"]
                [org.postgresql/postgresql "9.4-1201-jdbc41"]
                [funcool/octet "1.1.2"]]

  :plugins [[lein-figwheel "0.5.14"]
           [lein-cljsbuild "1.1.7" :exclusions [[org.clojure/clojure]]]
           [lein-autoexpect "1.9.0"]]

  :source-paths ["src"]

  :cljsbuild {
    :builds {
      :solo {
        :source-paths ["src/cljs-lotrsolo"]
        :figwheel true
        :compiler {
          :main danuraidb.lotrsolocore
          :asset-path "/js/compiled/soloout"
          :output-to "resources/public/js/compiled/lotrsolo.js"
          :output-dir "resources/public/js/compiled/soloout"
          :source-map-timestamp true
          :preloads [devtools.preload]}}
      :fellowship {
        :source-paths ["src/cljs-lotrfellowship"]
        :figwheel true
        :compiler {
          :main danuraidb.fellowshipcore
          :asset-path "/js/compiled/fellowshipout"
          :output-to "resources/public/js/compiled/fellowship.js"
          :output-dir "resources/public/js/compiled/fellowshipout"
          :source-map-timestamp true
          :preloads [devtools.preload]}}
      :solo-min {
        :source-paths ["src/cljs-lotrsolo"]
        :compiler {
          :main danuraidb.lotrsolocore
          :output-to "resources/public/js/compiled/lotrsolo.js"
          :output-dir "resources/public/js/compiled/solooutmin"
          :optimizations :advanced :pretty-print false}}
      :fellowship-min {
        :source-paths ["src/cljs-lotrfellowship"]
        :compiler {
          :main danuraidb.fellowshipcore
          :output-to "resources/public/js/compiled/fellowship.js"
          :output-dir "resources/public/js/compiled/fellowshipoutmin"
          :optimizations :advanced :pretty-print false}}}}

  :figwheel { :css-dirs ["resources/public/css"]}

  ;; Setting up nREPL for Figwheel and ClojureScript dev
  ;; Please see:
  ;; https://github.com/bhauman/lein-figwheel/wiki/Using-the-Figwheel-REPL-within-NRepl
  :profiles {:uberjar {:aot :all
                     :source-paths ["src"]
                     :prep-tasks ["compile" ["cljsbuild" "once" "fellowship-min"]
                                 "compile" ["cljsbuild" "once" "solo-min"]]
                     }
            :dev {:dependencies [[reloaded.repl "0.2.4"]
                               [expectations "2.2.0-rc3"]
                               [binaryage/devtools "0.9.4"]
                               [figwheel-sidecar "0.5.14"]
                               [com.cemerick/piggieback "0.2.2"]]
                   ;; need to add dev source path here to get user.clj loaded
                   :source-paths ["src" "dev"]
                   ;; for CIDER
                   ;; :plugins [[cider/cider-nrepl "0.12.0"]]
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}
                   ;; need to add the compliled assets to the :clean-targets
                   :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                                     :target-path]}})
