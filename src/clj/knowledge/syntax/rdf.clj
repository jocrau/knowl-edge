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
  ^{:doc "This namespace provides the basic functions to manipulate RDF. It is part of the knowl:edge Management System."
    :author "Jochen Rau"}
  knowledge.syntax.rdf
  (:refer-clojure :exclude [namespace])
  (:require [rdfa.core :as rdfa]))

(defprotocol RDFFactory
  (create-resource [value])
  (create-literal [value] [value language-or-datatype]))

(defprotocol Value
  (value [this]))

(defprotocol Literal
  (datatype [this])
  (language [this]))

(defprotocol Resource
  (identifier [this])
  (namespace [this])
  (local-name [this]))

(defprotocol StatementProtocol
  (subject [statement])
  (predicate [statement])
  (object [statement]))

(defprotocol Graph
  (statements [graph]))

(extend-type nil
  Value
  (value [this] nil)
  Resource
  (identifier [this] nil)
  (namespace [this] nil)
  (local-name [this] nil)
  Literal
  (datatype [this] nil)
  (language [this] nil)
  Graph
  (statements [this] nil))

(deftype Statement [subject predicate object])

(extend-type Statement
  StatementProtocol
  (subject [statement] (.subject statement))
  (predicate [statement] (.predicate statement))
  (object [statement] (.object statement)))

(defn create-statement
  [subject predicate object]
  (Statement. subject predicate object))


;; TODO move this to knowledge.syntax.rdf.clj-rdfa

(extend-type rdfa.core.IRI
  Value
  (value [this] (:id this))
  Resource
  (identifier [this] (:id this)))

(extend-type rdfa.core.BNode
  Value
  (value [this] (:id this))
  Resource
  (identifier [this] (:id this)))

(extend-type rdfa.core.Literal
  Value
  (value [this] (:value this))
  Literal
  (datatype [this] (let [tag (:tag this)]
                         (if (instance? rdfa.core.IRI tag) tag)))
  (language [this] (let [tag (:tag this)]
                         (if-not (instance? rdfa.core.IRI tag) tag))))
