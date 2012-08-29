(defproject knowledge "0.0.1-SNAPSHOT" 
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [ring "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [enlive "1.0.1"]
                 [re-rand "0.1.0"]
                 [clj-time "0.4.1"]
                 [org.apache.jena/jena-arq "2.9.0-incubating"]]
  :profiles {:dev {:dependencies [[midje "1.4.0"]]}}
  :resource-paths ["resources"]
  :aot [knowledge.server]
  :main knowledge.server
  :uberjar-exclusions [#"META-INF/ECLIPSEF.SF"]
  :description "This is a knowl:edge Management System")