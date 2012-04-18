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
  ^{:doc "This namespace provides functionailty to render a given data structure recursively. It is part of the know:ledge cms."
    :author "Jochen Rau"}  
  knowl.edge.view
  (:require
    [knowl.edge.base :as base]
    [net.cgrand.enlive-html :as html]))

(defn init []
  "Initialize the RDF Store with the configured implementation)."
  (-> "config.clj" slurp read-string eval))

(init)

(defprotocol Transformer
  "Provides functions to generate a view of the subject."
  (transform [this] "Renders the output recursively."))

(defn transform-literal [this]
  (:value this))

(defn transform-resource [resource]
  (if-let [statements (store/find-by-subject resource)]
    (map transform statements)
    (:value resource)))

(defn transform-statement [statement]
  {:tag :div :content [(:value (:predicate statement)) " " (transform (:object statement))]})

(extend-protocol Transformer
  knowl.edge.base.Statement
  (transform [this] (transform-statement this))
  knowl.edge.base.BlankNode
  (transform [this] (transform-resource this))
  knowl.edge.base.URI
  (transform [this] (transform-resource this))
  knowl.edge.base.Literal
  (transform [this] (transform-literal this))
  java.lang.String
  (transform [this] this))

;; Entry Point
(defn render [this]
  (if-let [result (transform this)]
    (html/emit* result)))