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
  ^{:doc "This namespace provides . It is part of the know:ledge cms."
    :author "Jochen Rau"}
  knowl.edge.store.endpoint
  (:import
    (com.hp.hpl.jena.rdf.model Model StmtIterator)
    (com.hp.hpl.jena.query QueryExecutionFactory)))

(def default-service "http://dbpedia.org/sparql")

(defn find-by-query
  ([query-string] (find-by-query query-string default-service))
  ([query-string service]
    (with-open [query-execution (QueryExecutionFactory/sparqlService service query-string)]
      (iterator-seq (.listStatements (.execConstruct query-execution))))))

(defn find-by-subject [uri]
    (find-by-query (str "CONSTRUCT { <" uri "> ?p ?o . } WHERE { <" uri "> ?p ?o . }")))

(defn find-types-of [uri]
  (map #(.getObject %) (find-by-query (str "CONSTRUCT { <" uri "> a ?type . } WHERE { <" uri "> a ?type . }"))))
