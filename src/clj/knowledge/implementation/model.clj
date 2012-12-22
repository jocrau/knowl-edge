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
   knowledge.implementation.model
  (:refer-clojure :exclude [namespace])
  (:require [clojure.contrib.str-utils2 :as string]
            [knowledge.transformation])
  (:import (com.hp.hpl.jena.rdf.model ModelFactory)
           (com.hp.hpl.jena.datatypes TypeMapper)))

(extend-type String
  knowledge.model/Value
  (value [this] this)
  knowledge.model/Resource
  (identifier [this] this))

;; clj-rdfa Implementation

(extend-type rdfa.core.IRI
  knowledge.model/Value
  (knowledge.model/value [this] (:id this))
  knowledge.model/Resource
  (knowledge.model/identifier [this] (:id this)))

(extend-type rdfa.core.BNode
  knowledge.model/Value
  (knowledge.model/value [this] (:id this))
  knowledge.model/Resource
  (knowledge.model/identifier [this] (:id this)))

(extend-type rdfa.core.Literal
  knowledge.model/Value
  (knowledge.model/value [this] (:value this))
  knowledge.model/Literal
  (knowledge.model/datatype [this] (let [tag (:tag this)]
                                     (if (instance? rdfa.core.IRI tag) tag)))
  (knowledge.model/language [this] (let [tag (:tag this)]
                                     (if-not (instance? rdfa.core.IRI tag) tag))))

;; TODO move this to a dedicated namespace
(defn serialize-resource [resource]
  (str "<" (knowledge.model/identifier resource) ">"))

(defn serialize-literal [literal]
  (let [value (str "\"" (knowledge.model/value literal) "\"")
        tag (or (if (seq (knowledge.model/datatype literal)) (str "^^" (knowledge.model/identifier (knowledge.model/datatype literal))))
                (if (seq (knowledge.model/language literal)) (str "@" (knowledge.model/language literal))))]
    (str value tag)))

(defmethod knowledge.transformation/serialize [rdfa.core.IRI :turtle] [thing _] (serialize-resource thing))
(defmethod knowledge.transformation/serialize [rdfa.core.BNode :turtle] [thing _] (serialize-resource thing))
(defmethod knowledge.transformation/serialize [rdfa.core.Literal :turtle] [thing _] (serialize-literal thing))
  
;; Apache Jena Implementation

(extend-type com.hp.hpl.jena.rdf.model.impl.ResourceImpl
  knowledge.model/Value
  (knowledge.model/value [this] (.getURI this))
  knowledge.model/Resource
  (knowledge.model/identifier [this] (.getURI this))
  (knowledge.model/namespace [this] (.getNamespace this))
  (knowledge.model/local-name [this] (.getLocalName this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDBaseNumericType
  knowledge.model/Value
  (knowledge.model/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDDateTimeType
  knowledge.model/Value
  (knowledge.model/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDFloat
  knowledge.model/Value
  (knowledge.model/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.BaseDatatype
  knowledge.model/Value
  (knowledge.model/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.rdf.model.impl.LiteralImpl
  knowledge.model/Value
  (knowledge.model/value [this] (.getLexicalForm this))
  knowledge.model/Literal
  (knowledge.model/datatype [this] (.getDatatype this))
  (knowledge.model/language
    [this]
    (let [language (.getLanguage this)]
      (if (string/blank? language)
        nil
        language))))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XMLLiteralType
  knowledge.model/Value
  (knowledge.model/value [this] (.getURI this))
  knowledge.model/Literal
  (knowledge.model/datatype [this] (.getDatatype this))
  (knowledge.model/language [this] nil)) ;; TODO Check spec again (see http://jena.apache.org/documentation/notes/typed-literals.html)

(extend-type com.hp.hpl.jena.rdf.model.impl.StatementImpl
  knowledge.model/Statement
  (knowledge.model/subject [statement] (.getSubject statement))
  (knowledge.model/predicate [statement] (.getPredicate statement))
  (knowledge.model/object [statement] (.getObject statement)))

(extend-protocol knowledge.model/RDFFactory
  String
  (knowledge.model/create-resource [this] (with-open [model (ModelFactory/createDefaultModel)]
                            (if (string/blank? this)
                              (.createResource model)
                              (.createResource model this))))
  (knowledge.model/create-literal
    ([this] (with-open [model (ModelFactory/createDefaultModel)]
              (.createLiteral model this)))
    ([this language-or-datatype]
      (with-open [model (ModelFactory/createDefaultModel)]
        (if (knowledge.model/iri-string? (name language-or-datatype))
          (.createTypedLiteral model this (.getTypeByName (TypeMapper/getInstance) language-or-datatype))
          (.createLiteral model this (name language-or-datatype))))))
  clojure.lang.IPersistentVector
  (knowledge.model/create-resource
    [this]
    (let [[prefix local-name] this
          prefix (or (knowledge.model/resolve-prefix prefix) prefix)
          local-name (name local-name)]
      (with-open [model (ModelFactory/createDefaultModel)]
        (.createProperty model prefix local-name))))
  clojure.lang.Keyword
  (knowledge.model/create-resource
    [this]
    (let [iri (str knowledge.model/ontology-base-iri (name this))]
      (with-open [model (ModelFactory/createDefaultModel)]
        (.createResource model iri))))
  nil
  (knowledge.model/create-resource [this] (with-open [model (ModelFactory/createDefaultModel)]
                            (.createResource model))))
