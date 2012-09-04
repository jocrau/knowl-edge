(ns knowledge.rdfa
  (:require [goog.dom :as dom]
            [cljs.core :as cljs]
            [clojure.browser.net :as net]))

(defn ^:export test-rdfa []
  (let [nodes (.getElementsByProperty js/document "http://www.w3.org/2011/content#rest")]
    (doseq [node nodes]
      (dom/setTextContent node "foo"))))

(defn serialize-object [object]
  (let [value (object "value")
        language (object "language")
        datatype (object "type")]
    (cond
      (= (str datatype) "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral")
      (str "\"\"\"" value "\"\"\"@" language)
      (= (str datatype) "http://www.w3.org/1999/02/22-rdf-syntax-ns#object")
      (str "<" value ">")
      :else
      (if (seq datatype)
        (str "\"\"\"" value "\"\"\"^^<" datatype ">")))))

(defn serialize-objects [subject predicate objects]
  (loop [objects objects
         representation ""]
    (if-not (seq objects)
      representation
      (recur (rest objects)
             (str representation "<" subject "> <" predicate "> " (serialize-object (first objects)) ".\n")))))

(defn serialize-predicates [subject predicates]
  (loop [predicates predicates
         representation ""]
    (if-not (seq predicates)
      representation
      (recur (rest predicates)
             (let [predicate (ffirst predicates)
                   objects ((second (first predicates)) "objects")]
               (serialize-objects subject predicate objects))))))

(defn serialize-triples [graph]
  (loop [graph graph
         representation ""]
    (if-not (seq graph)
      representation
      (recur (rest graph)
             (let [subject (ffirst graph)
                   predicates ((second (first graph)) "predicates")]
             (str representation (serialize-predicates subject predicates)))))))

(defn ^:export export-graph []
  (let [connection (net/xhr-connection)
        graph (cljs/js->clj js/document.data._data_.triplesGraph)
        representation (serialize-triples graph)
        headers {"Content-Type" "text/turtle;charset=UTF-8"}]
    (net/transmit connection "http://localhost:8080/resource" "POST" representation)))
