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
   knowledge.syntax.rdf.jena
  (:require [clojure.contrib.str-utils2 :as string]
            [knowledge.syntax.rdf :as rdf]
            [knowledge.transformation]
            [knowledge.syntax.curie :as curie]
            [knowledge.syntax.iri :as iri])
  (:import (com.hp.hpl.jena.rdf.model ModelFactory)
           (com.hp.hpl.jena.datatypes TypeMapper)))

(extend-type com.hp.hpl.jena.rdf.model.impl.ResourceImpl
  rdf/Value
  (rdf/value [this] (.getURI this))
  rdf/Resource
  (rdf/identifier [this] (.getURI this))
  (rdf/namespace [this] (.getNamespace this))
  (rdf/local-name [this] (.getLocalName this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDBaseNumericType
  rdf/Value
  (rdf/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDDateTimeType
  rdf/Value
  (rdf/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDFloat
  rdf/Value
  (rdf/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.datatypes.BaseDatatype
  rdf/Value
  (rdf/value [this] (.getURI this)))

(extend-type com.hp.hpl.jena.rdf.model.impl.LiteralImpl
  rdf/Value
  (rdf/value [this] (.getLexicalForm this))
  rdf/Literal
  (rdf/datatype [this] (.getDatatype this))
  (rdf/language
    [this]
    (let [language (.getLanguage this)]
      (if (string/blank? language)
        nil
        language))))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XMLLiteralType
  rdf/Value
  (rdf/value [this] (.getURI this))
  rdf/Literal
  (rdf/datatype [this] (.getDatatype this))
  (rdf/language [this] nil)) ;; TODO Check spec again (see http://jena.apache.org/documentation/notes/typed-literals.html)

(extend-type com.hp.hpl.jena.rdf.model.impl.StatementImpl
  rdf/Statement
  (rdf/subject [statement] (.getSubject statement))
  (rdf/predicate [statement] (.getPredicate statement))
  (rdf/object [statement] (.getObject statement)))

(extend-protocol rdf/RDFFactory
  String
  (rdf/create-resource [this] (with-open [model (ModelFactory/createDefaultModel)]
                            (if (string/blank? this)
                              (.createResource model)
                              (.createResource model this))))
  (rdf/create-literal
    ([this] (with-open [model (ModelFactory/createDefaultModel)]
              (.createLiteral model this)))
    ([this language-or-datatype]
      (with-open [model (ModelFactory/createDefaultModel)]
        (if (iri/iri-string? (name language-or-datatype))
          (.createTypedLiteral model this (.getTypeByName (TypeMapper/getInstance) language-or-datatype))
          (.createLiteral model this (name language-or-datatype))))))
  clojure.lang.IPersistentVector
  (rdf/create-resource
    [this]
    (let [[prefix local-name] this
          prefix (or (curie/resolve-prefix prefix) prefix)
          local-name (name local-name)]
      (with-open [model (ModelFactory/createDefaultModel)]
        (.createProperty model prefix local-name))))
  nil
  (rdf/create-resource [this] (with-open [model (ModelFactory/createDefaultModel)]
                            (.createResource model))))
