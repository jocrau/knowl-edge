(defproject com.neomantics/knowledge "0.0.2-SNAPSHOT"
  :description "This is a knowl:edge Management System"
  :license {:name "MIT License"
            :url "http://www.opensource.org/licenses/mit-license.php"
            :distribution :repo}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [enlive "1.1.1"]
                 [com.cemerick/friend "0.1.3"]
                 [re-rand "0.1.0"]
                 [clj-time "0.5.0"]
                 [joda-time "2.1"]
                 [rdfa "0.5.1-SNAPSHOT"]
                 [org.apache.jena/jena-arq "2.9.0-incubating"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}
  :source-paths ["src/clj"
                 "src/cljs"
                 "cljs-checkouts/clojurescript/src/clj"
                 "cljs-checkouts/clojurescript/src/cljs"]
  :resource-paths ["resources"]
  :cljsbuild {:crossovers [knowledge.store knowledge.syntax.rdf knowledge.syntax.rdf.clj-rdfa]
              :crossover-jar true
              :crossover-path "src/crossover"
              :builds [{:source-path "src/cljs"
                        :compiler {:optimization :whitespace
                                   :output-to "resources/public/js/knowledge.js"}}]}
  :uberjar-exclusions [#"META-INF/ECLIPSEF.SF"]
  :min-lein-version "2.0.0")