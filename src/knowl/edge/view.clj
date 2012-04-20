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
    [clj-time.format :as time]
    [knowl.edge.base :as base]
    [net.cgrand.enlive-html :as html]))

;; Initialization

(defn init []
  "Initialize the RDF Store with the configured implementation)."
  (-> "config.clj" slurp read-string eval))

(init)

(def ^:dynamic *template* (html/html-resource (java.io.File. "resources/private/templates/page.html")))

;; Predicates

(defn- type= [resource]
  (html/attr= :typeof (:value resource)))

(defn- property= [resource]
  (html/attr= :property (:value resource)))

(defn- set-resource [resource]
  (html/set-attr :resource (:value resource)))

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

(defmulti transform-literal (fn [literal context] (:value (:datatype literal))))
(defmethod transform-literal :default [literal context] (:value literal))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#dateTime" [literal context]
  (time/unparse (time/formatters :rfc822) (time/parse (time/formatters :date-time-no-ms) (:value literal))))

(defn transform-resource [resource context]
  (if-let [statements (store/find-by-subject resource)]
    (let [context (conj-selector context [(type= (first (store/find-types-of resource)))])
          snippet (html/select *template* (:selector-chain context))]
      (loop [nodes (html/transform snippet [html/root] (set-resource resource))
             statements statements]
        (if-not (seq statements)
          nodes
          (let [statement (first statements)]
            (recur (html/transform nodes [[html/root] (property= (:predicate statement))] (html/content (transform (:object statement) context)))
                   (rest statements))))))))

(extend-protocol Transformer
  knowl.edge.base.BlankNode
  (transform [this context] (transform-resource this context))
  knowl.edge.base.URI
  (transform [this context] (transform-resource this context))
  knowl.edge.base.Literal
  (transform [this context] (transform-literal this context))
  java.lang.String
  (transform [this context] this)
  nil
  (transform [this context] nil))

;; Entry Point

(defn render [this]
  (if-let [result (transform this (Context. 0 []))]
    (html/emit* result)))