(ns knowledge.rdfa
  (:require [cljs.core :as cljs]
            [clojure.browser.net :as net]
            [rdfa.core :as core]
            [knowledge.syntax.rdf :as rdf]
            [knowledge.syntax.turtle.serialize :as turtle]))

(defn ^:export extract-statements []
  (let [document-element (.-documentElement js/document)
        location (.-URL js/document)]
    (with-meta (:triples (core/extract-rdfa :html document-element location)) {:type knowledge.syntax.rdf/Graph})))

(defn ^:export serialize-statements [statements]
  (turtle/serialize statements :turtle))

(defn ^:export export-statements []
  (let [connection (net/xhr-connection)
        location (.-URL js/document)
        method "POST"
        representation (serialize-statements (extract-statements))
        headers (cljs/js-obj "Content-Type" "text/turtle;charset=utf-8")]
    (net/transmit connection location method representation headers)))
