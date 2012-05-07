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
  ^{:doc "This namespace provides functionailty to transform a given resource recursively into a representation. It is part of the know:ledge management system."
    :author "Jochen Rau"}  
  knowl.edge.transformation
  (:use knowl.edge.store)
  (:require
    [clj-time.format :as time]
    [knowl.edge.model :as model]
    [knowl.edge.store :as store]
    [net.cgrand.enlive-html :as enlive]))

(def ^:dynamic *template* (enlive/html-resource (java.io.File. "resources/private/templates/page.html")))
(def store (knowl.edge.store.Endpoint. "http://dbpedia.org/sparql" {}))

;; Predicates

(defn- type= [resource]
  (enlive/attr= :typeof (:value resource)))

(defn- property= [resource]
  (enlive/attr= :property (:value resource)))

;; Context

(defprotocol ContextHandling
  "Functions for dealing with a transformations context (like render depth, the web request, or the current selector chain)."
  (conj-selector [this selector] "Appends a selector to the selector-chain"))

(defrecord Context [depth selector-chain]
  ContextHandling
  (conj-selector
    [this selector]
    (update-in this [:selector-chain] #(into % selector))))

;; Transformations

(defprotocol Transformer
  "Provides functions to transform the given subject into a different representation."
  (transform [this context] "Transforms the subject."))

(defmulti transform-literal (fn [literal context] (-> literal model/datatype model/value)))
(defmethod transform-literal :default [literal context] (model/value literal))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#dateTime" [literal context]
  (time/unparse (time/formatters :rfc822) (time/parse (time/formatters :date-time-no-ms) (model/value literal))))

(defn transform-resource [resource context]
  (if-let [statements (seq (find-by-subject store resource))]
    (let [context (conj-selector context [(type= (first (find-types-of store resource)))])
          snippet (enlive/select *template* (:selector-chain context))
          grouped-statements (group-by #(model/predicate %) statements)]
      (loop [snippet (enlive/transform snippet [enlive/root] (enlive/set-attr :resource (model/identifier resource)))
             grouped-statements grouped-statements]
        (if-not (seq grouped-statements)
          snippet
          (recur
            (enlive/transform snippet [(property= (first (first grouped-statements)))]
                              (enlive/clone-for [statement (second (first grouped-statements))]
                                                (enlive/do->
                                                  (enlive/content (transform (model/object statement) context))
                                                  (if-let [datatype (-> statement model/object model/datatype model/identifier)]
                                                    (enlive/do->
                                                      (enlive/set-attr :datatype datatype)
                                                      (enlive/set-attr :content (-> statement model/object model/value)))
                                                    identity))))
            (rest grouped-statements)))))))

(extend-protocol Transformer
  java.lang.String
  (transform [this context] this)
  nil
  (transform [this context] nil))

;; Entry Point

(defn dereference [resource]
  (if-let [result (transform resource (Context. 0 []))]
    (enlive/emit* result)))