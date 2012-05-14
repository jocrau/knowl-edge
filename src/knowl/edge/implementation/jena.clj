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
  ^{:doc "This namespace provides the jena wrapper to manipulate RDF. It is part of the know:ledge Management System."
    :author "Jochen Rau"}
   knowl.edge.implementation.jena)

(in-ns 'knowl.edge.model)
(require '[clojure.contrib.str-utils2 :as string])
(import '(com.hp.hpl.jena.rdf.model ModelFactory)
        '(com.hp.hpl.jena.datatypes TypeMapper))

(extend-type com.hp.hpl.jena.rdf.model.impl.ResourceImpl
  Value
  (value [this] (.getURI this))
  Resource
  (identifier [this] (.getURI this))
  (namespace [this] (.getNamespace this))
  (local-name [this] (.getLocalName this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDBaseNumericType
  Value
  (value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDDateTimeType
  Value
  (value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.rdf.model.impl.LiteralImpl
  Value
  (value [this] (.getLexicalForm this))
  Literal
  (datatype [this] (.getDatatype this))
  (language
    [this]
    (let [language (.getLanguage this)]
      (if (string/blank? language)
        nil
        language))))

(extend-type com.hp.hpl.jena.rdf.model.impl.StatementImpl
  Statement
  (subject [statement] (.getSubject statement))
  (predicate [statement] (.getPredicate statement))
  (object [statement] (.getObject statement)))

(extend-protocol RDFFactory
  String
  (create-resource [this] (with-open [model (ModelFactory/createDefaultModel)]
                            (if (string/blank? this)
                              (.createResource model)
                              (.createResource model this))))
  (create-literal
    ([this] (with-open [model (ModelFactory/createDefaultModel)]
              (.createLiteral model this)))
    ([this language-or-datatype]
      (with-open [model (ModelFactory/createDefaultModel)]
        (if (iri-string? (name language-or-datatype))
          (.createTypedLiteral model this (.getTypeByName (TypeMapper/getInstance) language-or-datatype))
          (.createLiteral model this (name language-or-datatype))))))
  clojure.lang.IPersistentVector
  (create-resource
    [this]
    (let [[prefix local-name] this
          iri (str (resolve-prefix prefix) (name local-name))]
      (with-open [model (ModelFactory/createDefaultModel)]
        (.createResource model iri))))
  clojure.lang.Keyword
  (create-resource
    [this]
    (let [iri (str *base* (name this))]
      (with-open [model (ModelFactory/createDefaultModel)]
        (.createResource model iri))))
  nil
  (create-resource [this] (with-open [model (ModelFactory/createDefaultModel)]
                            (.createResource model))))

(in-ns 'knowl.edge.transformation)

(extend-protocol Transformer
  com.hp.hpl.jena.rdf.model.impl.LiteralImpl
  (transform [this context] (transform-literal this context))
  com.hp.hpl.jena.rdf.model.impl.ResourceImpl
  (transform
    [this context]
    (transform-resource this context)))


(in-ns 'knowl.edge.store)
(use '[clojure.contrib.core :only (-?>)])
(require '[clojure.contrib.str-utils2 :as string])
(import '(com.hp.hpl.jena.query QueryExecutionFactory)
        '(knowl.edge.store Endpoint))

(extend-type knowl.edge.store.Endpoint
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
  (find-types-of
    [this resource]
    (map
      #(knowl.edge.model/object %)
      (find-by-query this (str "CONSTRUCT { <" resource "> a ?type . } WHERE { <" resource "> a ?type . }"))))
  (find-matching
    ([this] (find-matching this nil nil nil))
    ([this subject] (find-matching this subject nil nil))
    ([this subject predicate] (find-matching this subject predicate nil))
    ([this subject predicate object]
      (let [subject (or (-?> subject (string/join ["<" ">"])) "?s")
            predicate (or (-?> predicate (string/join ["<" ">"])) predicate "?p")
            object (or (-?> object (string/join ["<" ">"])) "?o")
            statement (string/join " " [subject predicate object])]
        (find-by-query this (str "CONSTRUCT { " statement " . } WHERE { " statement " . }"))))))
