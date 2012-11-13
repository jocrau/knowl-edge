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
   knowledge.implementation.store
  (:use knowledge.store
        [clojure.contrib.core :only (-?>)])
  (:require [clojure.contrib.str-utils2 :as string]
            [knowledge.model :as model])
  (:import (com.hp.hpl.jena.query QueryExecutionFactory)
           (com.hp.hpl.jena.rdf.model ModelFactory Resource Property RDFNode)
           (knowledge.store Endpoint MemoryStore)))

(defn get-base-iri* [] (or (System/getenv "BASE_IRI") "http://localhost:8080/"))

;; Endpoint Implementation

(extend-type Endpoint
  Store
  (get-base-iri [this] (get-base-iri*))
  (clear-all [this] nil)
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
  (find-types-of [this resource]  (let [statements (find-by-query this (str "CONSTRUCT { <" resource "> a ?type . } WHERE { <" resource "> a ?type . }"))]
                                    (map #(model/object %) statements)))
  (find-matching
    ([this] (find-matching this nil nil nil))
    ([this subject] (find-matching this subject nil nil))
    ([this subject predicate] (find-matching this subject predicate nil))
    ([this subject predicate object] (let [subject (or (-?> subject (string/join ["<" ">"])) "?s")
                                           predicate (or (-?> predicate (string/join ["<" ">"])) predicate "?p")
                                           language-filter (if (nil? object)
                                                             " FILTER (!isLiteral(?o) || langMatches(lang(?o), \"en\") || langMatches(lang(?o), \"\"))")
                                           object (or (-?> object (string/join ["<" ">"])) "?o")
                                           pattern (string/join " " [subject predicate object])
                                           statement (str "CONSTRUCT { " pattern " . } WHERE { " pattern " . " language-filter " }")]
                                       (find-by-query this statement)))))

;; MemoryStore Implementation

(extend-type com.hp.hpl.jena.rdf.model.impl.ModelCom
  Store
  (get-base-iri [this] (get-base-iri*))
  (clear-all [this] (.removeAll this))
  (add-statements
    ([this statements]
      (add-statements this statements {}))
    ([this statements options]
      (.read this statements (get-base-iri this) (serialization-format options))))
  (find-by-query
    ([this query-string]
      (with-open [query-execution (QueryExecutionFactory/create query-string this)]
        (try
          (iterator-seq (.listStatements (.execConstruct query-execution)))
          (catch Exception e nil)))))
  (find-types-of [this resource] (let [predicate (.createProperty this "http://www.w3.org/1999/02/22-rdf-syntax-ns#" "type")
                                       statements (find-matching this resource predicate)]
                                   (map #(model/object %) statements)))
  (find-matching
    ([this] (find-matching this nil nil nil))
    ([this subject] (find-matching this subject nil nil))
    ([this subject predicate] (find-matching this subject predicate nil))
    ([this subject predicate object] (iterator-seq (.listStatements this subject predicate object))))
  Exporter
  (import-into
    [this source options]
    (with-open [stream (clojure.java.io/input-stream source)]
      (.read this stream (get-base-iri*) (serialization-format options))))
  (export-from
    [this target options]
    (with-open [stream (clojure.java.io/output-stream target)]
      (.write this stream (serialization-format options)))))
