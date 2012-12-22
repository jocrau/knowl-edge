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
  ^{:doc "This namespace provides functions to query a SPARQL endpoint. It is part of the knowl:edge Management System."
    :author "Jochen Rau"}
  knowledge.store
  (:require [knowledge.model :as model]))

(defn serialization-format [options]
  (name (or (:format options) "TTL")))

(defprotocol Store
  (get-base-iri [this])
  (clear-all [this])
  (add-statements [this statements] [this statements options])
  (find-by-query [this query-string] [this query-string callback])
  (find-types-of [this resource])
  (find-matching [this] [this subject] [this subject predicate] [this subject predicate object]))

(defprotocol Exporter
  (import-into [this source options])
  (export-from [this target options]))

(deftype Endpoint [service options])
(deftype MemoryStore [model options])

(defn stores-for 
  ([resource default-store]
    (let [query-string (str "PREFIX void: <http://rdfs.org/ns/void#>
														CONSTRUCT {
														?s void:sparqlEndpoint ?endpoint .
														}
														WHERE {
														?s a void:Dataset .
														?s void:sparqlEndpoint ?endpoint .
														?s void:uriSpace ?uriSpace .
														FILTER strStarts(\"" (model/identifier resource) "\", ?uriSpace)
														}")
          stores (find-by-query default-store query-string)]
      (conj (map #(Endpoint. (-> % model/object model/value) {})
                 stores)
            default-store))))
