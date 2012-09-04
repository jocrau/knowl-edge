; Copyright (c) 2012 Jochen Rau
; 
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
; 
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.
; 
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
; THE SOFTWARE.

(ns
  ^{:doc "This namespace provides the jena wrapper to manipulate RDF. It is part of the knowl:edge Management System."
    :author "Jochen Rau"}
   knowledge.implementation.jena.store)

(in-ns 'knowledge.store)
(require '[clojure.contrib.str-utils2 :as string])
(require '[knowledge.model :as model])
(import '(com.hp.hpl.jena.query QueryExecutionFactory)
        '(com.hp.hpl.jena.rdf.model ModelFactory Resource Property RDFNode)
        '(com.hp.hpl.jena.ontology OntModelSpec)
        '(knowledge.store Endpoint MemoryStore))

(defn- find-types-of* [this resource]
  (map #(model/object %)
    (find-by-query this (str "CONSTRUCT { <" resource "> a ?type . } WHERE { <" resource "> a ?type . }"))))

(defn- find-matching* [this subject predicate object]
  (let [subject (or (-?> subject (string/join ["<" ">"])) "?s")
        predicate (or (-?> predicate (string/join ["<" ">"])) predicate "?p")
        language-filter (if (nil? object)
                          " FILTER (!isLiteral(?o) || langMatches(lang(?o), \"en\") || langMatches(lang(?o), \"\"))")
        object (or (-?> object (string/join ["<" ">"])) "?o")
        pattern (string/join " " [subject predicate object])
        statement (str "CONSTRUCT { " pattern " . } WHERE { " pattern " . " language-filter " }")]
    (find-by-query this statement)))

;; Endpoint Implementation

(extend-type knowledge.store.Endpoint
  Store
  (find-by-query
    ([this query-string] (find-by-query this query-string (.service this)))
    ([this query-string service]
      (with-open [query-execution (QueryExecutionFactory/sparqlService service query-string)]
        (let [options (.options this)]
          (if (and (:username options) (:password options))
            (.setBasicAuthentication query-execution (:username options) (.toCharArray (:password options))))
          (try
            (iterator-seq (.listStatements (.execConstruct query-execution)))
            (catch Exception e nil))))))
  (find-types-of [this resource] (find-types-of* this resource))
  (find-matching
    ([this] (find-matching this nil nil nil))
    ([this subject] (find-matching this subject nil nil))
    ([this subject predicate] (find-matching this subject predicate nil))
    ([this subject predicate object] (find-matching* this subject predicate object))))

;; MemoryStore Implementation

(defn- serialization-format [options]
  (name (or (:format options) "TTL")))

(defn- base-iri []
  (or (System/getenv "BASE_IRI") "http://localhost/"))

(extend-type knowledge.store.MemoryStore
  knowledge.store/Store
  (add [this statements]
       (.add (.model this) statements (base-iri) "TTL"))
  (find-by-query
    ([this query-string]
      (with-open [query-execution (QueryExecutionFactory/create query-string (.model this))]
        (let [options (.options this)]
          (try
            (iterator-seq (.listStatements (.execConstruct query-execution)))
            (catch Exception e nil))))))
  (find-types-of [this resource] (find-types-of* this resource))
  (find-matching
    ([this] (find-matching this nil nil nil))
    ([this subject] (find-matching this subject nil nil))
    ([this subject predicate] (find-matching this subject predicate nil))
    ([this subject predicate object] (find-matching* this subject predicate object)))
  knowledge.store/Exporter
  (import-into
    [this source options]
    (with-open [stream (clojure.java.io/input-stream source)]
      (.read (.model this) stream (base-iri) (serialization-format options))))
  (export-from
    [this target options]
    (with-open [stream (clojure.java.io/output-stream target)]
      (.write (.model this) stream (serialization-format options)))))

;; Load the default graph into the in-memory store
(def default-store (MemoryStore. (ModelFactory/createOntologyModel (OntModelSpec/OWL_MEM)) {}))

(defn load-core-data []
  (import-into default-store (clojure.java.io/resource "private/data/core.ttl") {}))

(defn reload-core-data []
  (do
    (.removeAll (.model default-store))
    (load-core-data)))

