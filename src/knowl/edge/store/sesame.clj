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
  ^{:doc "This namespace provides the implementation of the RDF store functionality based on Sesame. It is part of the know:ledge cms."
    :author "Jochen Rau"}
  knowl.edge.store.sesame
  (:import 
    (org.openrdf.repository Repository RepositoryException)
    (org.openrdf.repository.sail SailRepository)
    (org.openrdf.sail.memory MemoryStore)
    (org.openrdf.sail.inferencer.fc ForwardChainingRDFSInferencer)
    (org.openrdf.repository RepositoryConnection)
    (org.openrdf.rio Rio RDFFormat)
    (org.openrdf.rio.turtle TurtleWriter)
    (org.openrdf.query TupleQuery TupleQueryResult BindingSet QueryLanguage GraphQueryResult)))

(def no-contexts (make-array org.openrdf.model.URI 0))

(def format-map
  {:xml RDFFormat/RDFXML
   :ntriples RDFFormat/NTRIPLES
   :n3 RDFFormat/N3
   :turtle RDFFormat/TURTLE
   :ttl RDFFormat/TURTLE
   :trig RDFFormat/TRIG
   :trix RDFFormat/TRIX})

(defn- translate-format
  "Translates an RDF format from a clojure keyword into the sesame object."
  [value] (value format-map))

(defn- build-repository [& options]
  (let [backend (MemoryStore.)
        inferencer (ForwardChainingRDFSInferencer. backend)
        repository (SailRepository. inferencer)]
    (do (.initialize repository)
      repository)))

(def ^:dynamic *repository* (build-repository))

(defn- iteration-seq
  "Creates and returns a lazy sequence of objects implementing info.aduna.iteration.Iteration"
  [^info.aduna.iteration.Iteration resultSet]
  (let [statements (fn thisfn []
                     (when (. resultSet (hasNext))
                       (let [statement (. resultSet (next))]
                         (cons
                           (let [subject (knowl.edge.base/translate (.getSubject statement))
                                 predicate (knowl.edge.base/translate (.getPredicate statement))
                                 object (knowl.edge.base/translate (.getObject statement))
                                 context (knowl.edge.base/translate (.getContext statement))]
                             (knowl.edge.base.Statement. subject predicate object context))
                           (lazy-seq (thisfn))))))]
    (statements)))

(defn resolve-prefix [curie]
  (let [connection (.getConnection *repository*)]
    (.getNamespace connection (name curie))))

(defn load-document
  ([source]
    (load-document source :xml "" no-contexts))
  ([source format]
    (load-document source format "" no-contexts))
  ([source format baseUri]
    (load-document source format baseUri no-contexts))
  ([source format baseUri contexts]
    (let [connection (.getConnection *repository*)
          format (or (translate-format format)
                     (translate-format :xml))]
      (try
        (.setAutoCommit connection false)
        (.add connection
              (java.io.StringBufferInputStream. (slurp source :encoding "ISO-8859-1")) ;; TODO auto-detect encoding
              baseUri format contexts)
        (.commit connection)
        (catch RepositoryException e (.rollback connection))
        (finally (.close connection))))))

(defn find-matching
  ([subject] (find-matching subject nil nil nil false))
  ([subject predicate] (find-matching subject predicate nil nil false))
  ([subject predicate object] (find-matching subject predicate object nil false))
  ([subject predicate object contexts] (find-matching subject predicate object contexts false))
  ([subject predicate object contexts infered]
    (let [subject (knowl.edge.base/translate subject)
          predicate (knowl.edge.base/translate predicate)
          object (knowl.edge.base/translate object)
          infered (if (nil? infered) false infered)
          contexts no-contexts ;; TODO Implement support of contexts
          connection (.getConnection *repository*)]
      (iteration-seq (.getStatements connection subject predicate object infered contexts)))))

(defn find-all []
  (find-matching nil nil nil))

(defn find-by-subject
  ([uri] (find-matching uri))
  ([uri context] (find-matching uri nil nil context))
  ([uri context infered] (find-matching uri nil nil context infered)))

(defn find-by-query ;; only CONSTRUCT allowed by now
  ([query-string] (find-by-query query-string false))
  ([query-string infered]
    (let [connection (.getConnection *repository*)
          query (.prepareGraphQuery connection QueryLanguage/SPARQL query-string)]
      (do
        (.setIncludeInferred query infered)
        (.setMaxQueryTime query 10) ;; TODO make this configurable
        (iteration-seq (.evaluate query))))))

(extend-protocol knowl.edge.base/BabelFish
  org.openrdf.model.URI
  (translate [value]
                   (let [namespace (.getNamespace value)
                         local-name (.getLocalName value)]
                     (knowl.edge.base.URI. (str namespace local-name))))
  org.openrdf.model.BNode
  (translate [value]
                   (let [identifier (.getID value)]
                     (knowl.edge.base.BlankNode. identifier)))
  org.openrdf.model.Literal
  (translate [value]
                   (let [label (.getLabel value)
                         language (.getLanguage value)
                         datatype (if-let [datatype (.getDatatype value)]
                                    (knowl.edge.base/u (str datatype)))]
                     (knowl.edge.base.Literal. label language datatype)))
  knowl.edge.base.URI
  (translate [value]
                   (let [connection (.getConnection *repository*)
                         vf (.getValueFactory connection)]
                     (.createURI vf (:value value))))
  knowl.edge.base.BlankNode
  (translate [value]
                   (let [connection (.getConnection *repository*)
                         vf (.getValueFactory connection)]
                     (.createBNode vf (:value value))))
  knowl.edge.base.Literal
  (translate [value]
                   (let [connection (.getConnection *repository*)
                         vf (.getValueFactory connection)]
                     (if-let [lang-or-type (or (:language value) (knowl.edge.base/translate (:datatype value)))]
                       (.createLiteral vf (:value value) lang-or-type)
                       (.createLiteral vf (:value value))))))

(defn load-demo-data []
  "This is a little helper function to populate the in-memory store"
  (load-document "resources/private/data/abox.n3" :n3)
  #_(store/load-document "http://sebastian.kurfuerst.eu/index.tt" :ttl)
  #_(store/load-document "http://www.heppnetz.de/ontologies/goodrelations/v1.owl" :xml)
  #_(store/load-document "http://www.w3.org/People/Berners-Lee/card#i" :xml)
  #_(store/load-document "http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf" :xml))

(load-demo-data)