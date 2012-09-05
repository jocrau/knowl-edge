(defproject knowledge "0.0.1-SNAPSHOT"
  :description "This is a knowl:edge Management System"
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [enlive "1.0.1"]
                 [re-rand "0.1.0"]
                 [clj-time "0.4.1"]
                 [org.apache.jena/jena-arq "2.9.0-incubating"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}
  :plugins [[lein-cljsbuild "0.2.7"]]
  :source-paths ["src/clj"]
  :resource-paths ["resources"]
  :cljsbuild {
      :builds [{
          :source-path "src/cljs"
          :compiler {
            :output-to "resources/public/js/application.js"
            :optimizations :simple
			:pretty-print true}}]}
  :hooks [leiningen.cljsbuild]
  :main knowledge.server
  :uberjar-exclusions [#"META-INF/ECLIPSEF.SF"])