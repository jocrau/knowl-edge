(defproject knowledge "0.0.1-SNAPSHOT"
  :description "This is a knowl:edge Management System"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring "1.1.5"]
                 [compojure "1.1.3"]
                 [enlive "1.0.1"]
                 [re-rand "0.1.0"]
                 [clj-time "0.4.4"]
                 [org.apache.jena/jena-arq "2.9.0-incubating"]
                 [rdfa "0.5.0-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "0.2.7"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:source-path "src/cljs"
                        :compiler {:output-to "resources/public/js/application.js"
                                   :libs ["resources/public/js/rdfa.js"]
                                   :optimizations :whitespace
                                   :pretty-print true}}]}
  :aot [knowledge]
  :main knowledge.server
  :uberjar-exclusions [#"META-INF/ECLIPSEF.SF"]
  :min-lein-version "2.0.0")