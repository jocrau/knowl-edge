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
  ^{:doc "This namespace provides functionailty to transform a given resource recursively into a representation. It is part of the knowl:edge management system."
    :author "Jochen Rau"}  
  knowledge.transformation
  (:refer-clojure :exclude [namespace])
  (:use 
    [clojure.contrib.core :only (-?>)]
    knowledge.model)
  (:require
    [knowledge.store :as store]
    [knowledge.base :as base]
    [clojure.set :as set]
    [clojure.contrib.str-utils2 :as string]
    [clj-time.format :as time]
    [ring.util.codec :as codec]
    [net.cgrand.enlive-html :as enlive])
  (:import (org.joda.time.format PeriodFormat ISOPeriodFormat)))

(def base-iri (or (System/getenv "BASE_IRI") "http://localhost:8080/"))
(def default-template-iri (str base-iri "templates/page.html"))

;; Predicates

(defn- type= [resource]
  #{(enlive/attr-has :typeof (identifier resource))
    (enlive/attr-has :about (identifier resource))})

(defn- predicate= [resource]
  #{(enlive/attr-has :property (identifier resource))
    (enlive/attr-has :rel (identifier resource))})

(defn- predicate? []
  #{(enlive/attr? :property)
    (enlive/attr? :rel)})

(defn- set-datatype [datatype]
  (enlive/set-attr :datatype (value datatype)))

(defn- set-language [language]
  (comp (enlive/set-attr :lang (value language)) (enlive/set-attr :xml:lang (value language))))

(defn- set-content-attr [content]
  (enlive/set-attr :content content))

(defn- set-content [content]
  (fn [node]
    (let [existing-content (:content node)]
      (if (seq existing-content)
        (assoc node :content (enlive/flatten-nodes-coll content))
        (if (string? content)
          ((enlive/set-attr :content content) node)
          node)))))

(defn- set-resource [resource]
  (if-let [iri (identifier resource)]
    (enlive/set-attr :about iri)
    (enlive/remove-attr :about)))

(defn- set-attr
 "Assocs attributes on the selected element."
 [& kvs]
  #(assoc % :attrs (apply assoc (:attrs % {}) kvs)))

(defn- set-predicate [resource]
  #(let [current-attrs (:attrs % {})
         current-attr-names (-> current-attrs keys set)
         iri (identifier resource)]
     (if-let [attr-name (first (set/intersection current-attr-names #{:rel :property}))]
       (assoc % :attrs (assoc current-attrs attr-name iri)))))

(defn- set-reference [resource]
  #(let [current-attrs (:attrs % {})
         current-attr-names (-> current-attrs keys set)
         iri (identifier resource)]
     (if-let [attr-name (first (set/intersection current-attr-names #{:href :src}))]
       (assoc % :attrs (assoc current-attrs attr-name iri)))))

(defn- set-types [types]
  (enlive/set-attr :typeof (string/join " " (map str types))))

;; Helper functions

(defn- extract-predicates [snippet]
  (filter #(string/contains? % ":")
          (map #(or (-> % :attrs :property)
                    (-> % :attrs :rel))
               (enlive/select snippet [(predicate?)]))))

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
  (time/unparse (time/formatter "MMMM d, yyyy") (time/parse (time/formatters :date-time-no-ms) (value literal))))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#duration" [literal context]
  (let [parser (ISOPeriodFormat/standard)
        unparser (PeriodFormat/getDefault)
        duration (.parsePeriod parser (value literal))]
    (.print unparser (.normalizedStandard duration))))

(defn literal? [object]
  (satisfies? knowledge.model/Literal object))

(defn transform-statement [statement context]
  (let [predicate (predicate statement)
        object (object statement)]
    (condp = (identifier predicate)
      "http://dbpedia.org/property/homepage"
      (enlive/do->
        (set-reference object)
        (set-content (value object)))      
      know:externalLink
      (enlive/do->
        (set-reference object)
        (set-content (value object)))
      dbo:wikiPageExternalLink
      (enlive/do->
        (set-reference object)
        (set-content (value object)))
      know:internalLink
      (set-reference object)
      foaf:depiction
      (do
        #(println %)
        (set-reference object))
      schema:image
      (set-reference object)
      "http://www.w3.org/ns/ma-ont#locator"
      (set-reference object)
      (enlive/do->
        (set-content (transform object context))
        (if (literal? object)
          (enlive/do->
            (if-let [datatype (datatype object)]
              (enlive/do->
                (set-datatype datatype)
                (if-not (= (value datatype) rdf:XMLLiteral)
                  (set-content-attr (value object))
                  identity))
              identity)
            (set-language (language object)))
          identity)))))

(defn- transform-statements* [snippet grouped-statements context]
  (fn [snippet [predicate statements]]
    (enlive/at snippet [(predicate= predicate)] (enlive/do->
                                                  (set-predicate predicate)
                                                  (enlive/clone-for
                                                    [statement statements]
                                                    (transform-statement statement context))))))

(defn transform-statements [statements snippet context]
  (let [grouped-statements (group-by #(predicate %) statements)]
    (reduce
      (transform-statements* snippet grouped-statements context)
      snippet grouped-statements)))

(defn- extract-types-from [statements]
  (when-let [type-statements (-> (filter #(= (-> % predicate identifier) rdf:type) statements))]
    (into #{} (map #(-> % object value) type-statements))))

(defn- extract-template-iri-from [statements]
  (when-let [statement (seq (filter #(= (-> % predicate identifier) know:template) statements))]
    (-> statement first object value)))

(defn- extract-query-from [statements]
  (-> (filter #(= (-> % predicate identifier) know:query) statements) first object value))

(defn- extract-service-from [statements]
  (when-let [service-statements (filter #(= (-> % predicate identifier) know:sparqlEndpoint) statements)]
    (-> service-statements first object value)))

(defn- pmap-set
  "This function takes the same arguments as clojures (p)map and flattens the first level 
   of the resulting lists of lists into a set."
  [f & colls]
  (into #{} (apply concat (apply pmap f colls))))

(defn fetch-statements
  "This function takes a resource and fetches statements with the given resource 
   as subject in all stores."
  [resource context]
  (let [stores (store/stores-for resource base/default-store)]
    (pmap-set #(store/find-matching % resource) stores)))

(defn set-base [template]
  (enlive/at template
    [:base] (enlive/substitute {:tag :base :attrs {:href base-iri}})))

(defn- fetch-template [iri]
  (set-base (enlive/html-resource (java.net.URL. iri))))

(defn- transform-resource* [resource statements context]
  (let [context (if-let [template-iri (extract-template-iri-from statements)]
                   (assoc context :template (fetch-template template-iri))
                   (if (contains? context :template)
                     context
                     (assoc context :template (fetch-template default-template-iri)))) ;; TODO memoize
        types (extract-types-from statements)
        context (assoc context :rootline (conj (:rootline context) (reduce #(conj %1 (type= %2)) #{} types)))
        snippet (enlive/transform (enlive/select (:template context) (:rootline context))
                                  [enlive/root]
                                  (enlive/do->
                                    (set-types types)
                                    (set-resource resource)))]
    
    (transform-statements statements snippet context)))

(defn transform-query [query store context]
  (when-let [grouped-statements (group-by #(subject %) (store/find-by-query store query))]
    (pmap-set
      (fn [[resource statements]]
        (transform-resource* resource statements context))
      grouped-statements)))

(defmacro match
  [statements & clauses]
  `(condp
     (fn [pattern# statements#]
       (some (fn [statement#]
               (and
                 (or (= (nth pattern# 0) nil) (= (-> statement# subject identifier) (nth pattern# 0)))
                 (or (= (nth pattern# 1) nil) (= (-> statement# predicate identifier) (nth pattern# 1)))
                 (or (= (nth pattern# 2) nil) (= (-> statement# object identifier) (nth pattern# 2)))))
             statements#))
     ~statements
     ~@clauses))

(defn transform-resource [resource context]
  (if (< (count (:rootline context)) 6)
    (when-let [statements (fetch-statements resource context)]
      (match statements
             [nil rdf:type spin:Construct] (when-let [query (extract-query-from statements)]
                                             (when-let [service (extract-service-from statements)]
                                               (let [store (knowledge.store.Endpoint. service {})]
                                                 (transform-query query store context))))
             (transform-resource* resource statements context)))))

;; Entry Point

(defn dereference [resource]
  (when-let [document (transform resource {:rootline []})]
    (enlive/emit* document)))

;; Fixes a problem with elive escaping strings
(in-ns 'net.cgrand.enlive-html)
(defn- xml-str [x] x)
