(defproject knowledge "0.0.1-SNAPSHOT"
  :description "This is a knowl:edge Management System"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring "1.1.5"]
                 [compojure "1.1.3"]
                 [enlive "1.0.1"]
                 [re-rand "0.1.0"]
                 [clj-time "0.4.4"]
                 [org.apache.jena/jena-arq "2.9.0-incubating"]]
  :plugins [[lein-cljsbuild "0.2.7"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]
                                  [rdfa "0.5.0-SNAPSHOT"]]}}
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:id "dev"
                        :source-path "src/cljs"
                        :jar true
                        :compiler {:output-to "resources/public/js/app.js"
                                   :optimizations :whitespace
                                   :pretty-print true
                                   :print-input-delimiter true}}
                       {:id "prod"
                        :source-path "src/cljs"
                        :jar true
                        :compiler {:output-to "resources/public/js/app.js"
                                   :optimizations :advanced
                                   :pretty-print false}}]}
  :aot [knowledge]
  :main knowledge.server
  :uberjar-exclusions [#"META-INF/ECLIPSEF.SF"]
  :min-lein-version "2.0.0")