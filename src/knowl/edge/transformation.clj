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
  (:refer-clojure :exclude [namespace])
  (:use knowl.edge.store
        knowl.edge.model)
  (:require
    [clojure.contrib.str-utils2 :as string]
    [clj-time.format :as time]
    [net.cgrand.enlive-html :as template]))

(def ^:dynamic *template* (template/html-resource (java.io.File. "resources/private/templates/dbpedia.html")))
(def store (knowl.edge.store.Endpoint. "http://dbpedia.org/sparql" {}))

;; Predicates

(defn- type= [resource]
  #{(template/attr-has :typeof (identifier resource)) (template/attr-has :about (identifier resource))})

(defn- property= [resource]
  #{(template/attr-has :property (identifier resource))
    (template/attr-has :rel (identifier resource))})

(defn- property? []
  #{(template/attr? :property)
    (template/attr-has :rel)})

(defn- set-datatype [datatype]
  (template/set-attr :datatype (value datatype)))

(defn- set-language [language]
  (comp (template/set-attr :lang (value language)) (template/set-attr :xml:lang (value language))))

(defn- set-content [resource]
  (template/set-attr :content (value resource)))

(defn- set-resource [resource]
  (template/set-attr :about (identifier resource)))

(defn- set-types [types]
  (template/set-attr :typeof (string/join " " (map str types))))

;; Helper functions

(defn- extract-predicates [snippet]
  (filter #(string/contains? % ":")
          (map #(or (-> % :attrs :property)
                    (-> % :attrs :rel))
               (template/select snippet [(property?)]))))

;; Context

(defprotocol ContextHandling
  "Functions for dealing with a transformations context (like render depth, the web request, or the current selector chain)."
  (conj-selector [this selector] "Appends a selector to the selector-chain"))

(defrecord Context [depth rootline]
  ContextHandling
  (conj-selector
    [this selector]
    (update-in this [:rootline] #(into % selector))))

;; Transformations

(defprotocol Transformer
  "Provides functions to transform the given subject into a different representation."
  (transform [this context] "Transforms the subject."))

(extend-protocol Transformer
  java.lang.String
  (transform [this context] this)
  nil
  (transform [this context] nil))

(defmulti transform-literal (fn [literal context] (-> literal datatype value)))
(defmethod transform-literal :default [literal context] (value literal))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#dateTime" [literal context]
  (time/unparse (time/formatter "EEEE dd MMMM, yyyy") (time/parse (time/formatters :date-time-no-ms) (value literal))))

(defn transform-statement [statement context]
  (let [object (object statement)]
    (template/do->
      (template/content (transform object context))
      (if (satisfies? knowl.edge.model/Literal object)
        (template/do->
          (if-let [datatype (datatype object)]
            (template/do->
              (set-datatype datatype)
              (set-content (value object)))
            identity)
          (if-let [language ( language object)]
            (set-language language)
            identity))
        identity))))

(defn transform-resource [resource context]
  (if (< (count (:rootline context)) 6)
    (if-let [statements (find-matching store resource)]
      (let [types (find-types-of store resource)
            context (conj-selector context [(into #{} (map #(type= %) types))])
            snippet (template/select *template* (:rootline context))
            snippet-predicates (extract-predicates snippet)
            grouped-statements (group-by #(predicate %) statements)
            query-predicates (keys grouped-statements)]
        (loop [snippet (template/transform snippet [template/root]
                                           (template/do->
                                             (set-types types)
                                             (set-resource resource)))
               grouped-statements grouped-statements]
          (if-not (seq grouped-statements)
            snippet
            (recur
              (template/transform
                snippet [(property= (ffirst grouped-statements))]
                (template/clone-for
                  [statement (second (first grouped-statements))]
                  (transform-statement statement context)))
              (rest grouped-statements)))))
      {:tag :a :attrs {:href (identifier resource)} :content (identifier resource)})
    {:tag :span :content "Max. Depth"}))

;; Entry Point

(defn dereference [resource]
  (if-let [result (transform resource (Context. 0 []))]
    (template/emit* result)))