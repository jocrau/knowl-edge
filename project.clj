(defproject knowl "0.0.1-SNAPSHOT"
  :description "Knowl is a Semantic Content Management System"
  :main knowl.edge
  :aot [knowl.edge]
  :dependencies [[org.clojure/clojure "1.3.0"]
                 [ring "1.1.0-SNAPSHOT"]
                 [compojure "1.0.1"]
                 [enlive "1.0.0"]
                 [re-rand "0.1.0"]
                 [clj-time "0.4.1"]
                 [org.openrdf.sesame/sesame-runtime "2.6.3"]]
  :dev-dependencies [[midje "1.3.1"]]
  :repositories {"aduna (sesame)"
                 "http://repo.aduna-software.org/maven2/releases/"}
  :resources-path "resources/public/")