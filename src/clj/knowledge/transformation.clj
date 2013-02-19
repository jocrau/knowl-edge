;; Copyright (c) 2012 Jochen Rau
;; 
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;; 
;; The above copyright notice and this permission notice shall be included in
;; all copies or substantial portions of the Software.
;; 
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
;; THE SOFTWARE.

(ns
  ^{:doc "This namespace provides functionailty to transform a given resource recursively into a representation. It is part of the knowl:edge management system."
    :author "Jochen Rau"}  
  knowledge.transformation
  (:use [clojure.contrib.core :only (-?>)])
  (:require
    [clojure.set :as set]
    [clojure.contrib.str-utils2 :as string]
    [clj-time.format :as time]
    [net.cgrand.enlive-html :as enlive]
    [rdfa.parser :as parser]
    [knowledge.store :as store]
    [knowledge.utilities :as util]
    [knowledge.security :as security]
    [knowledge.syntax.rdf :as rdf]
    [knowledge.transformation.turtle :as turtle])
  (:import (org.joda.time.format PeriodFormat ISOPeriodFormat)
           (java.io StringReader)
           (knowledge.store Endpoint)
           (knowledge.syntax.rdf Graph)))

(def base-iri (or (System/getenv "BASE_IRI") "http://localhost:8080/"))
(def default-template-iri (str base-iri "templates/todo.html"))

(def know "http://knowl-edge.org/ontology/core#")
(def foaf "http://xmlns.com/foaf/0.1/")
(def foaf:depiction (str foaf "depiction"))
(def foaf:primaryTopic (str foaf "primaryTopic"))
(def rdf "http://www.w3.org/1999/02/22-rdf-syntax-ns#")
(def rdf:type (str rdf "type"))
(def rdf:List (str rdf "List"))
(def rdf:first (str rdf "first"))
(def rdf:rest (str rdf "rest"))
(def rdf:nil (str rdf "nil"))
(def rdf:XMLLiteral (str rdf "XMLLiteral"))
(def rdfs "http://www.w3.org/2000/01/rdf-schema#")
(def rdfs:Resource (str rdfs "Resource"))
(def owl "http://www.w3.org/2002/07/owl#")
(def owl:Thing (str owl "Thing"))
(def know:sparqlEndpoint (str know "sparqlEndpoint"))
(def know:internalLink (str know "internalLink"))
(def know:externalLink (str know "externalLink"))
(def know:template (str know "template"))
(def schema "http://schema.org/")
(def schema:image (str schema "image"))
(def schema:encoding (str schema "encoding"))
(def spin:Construct "http://spinrdf.org/sp#Construct")
(def spin:text "http://spinrdf.org/sp#text")
(def dbo:wikiPageExternalLink "http://dbpedia.org/ontology/wikiPageExternalLink")
(def bibo:Webpage "http://purl.org/ontology/bibo/Webpage")

;; Predicates

(defn- type= [resource]
  #{(enlive/attr-has :typeof (rdf/identifier resource))
    (enlive/attr-has :about (rdf/identifier resource))})

(defn- predicate= [resource]
  #{(enlive/attr-has :property (rdf/identifier resource))
    (enlive/attr-has :rel (rdf/identifier resource))})

(defn- predicate? []
  #{(enlive/attr? :property)
    (enlive/attr? :rel)})

(defn- set-datatype [datatype]
  (enlive/set-attr :datatype (rdf/value datatype)))

(defn- set-language [language]
  (comp (enlive/set-attr :lang (rdf/value language)) (enlive/set-attr :xml:lang (rdf/value language))))

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
  (if-let [iri (rdf/identifier resource)]
    (enlive/set-attr :about iri)
    (enlive/remove-attr :about)))

(defn- set-attr
 "Assocs attributes on the selected element."
 [& kvs]
  #(assoc % :attrs (apply assoc (:attrs % {}) kvs)))

(defn- set-predicate [resource]
  #(let [current-attrs (:attrs % {})
         current-attr-names (-> current-attrs keys set)
         iri (rdf/identifier resource)]
     (if-let [attr-name (first (set/intersection current-attr-names #{:rel :property}))]
       (assoc % :attrs (assoc current-attrs attr-name iri)))))

(defn- set-reference [resource]
  #(let [current-attrs (:attrs % {})
         current-attr-names (-> current-attrs keys set)
         iri (rdf/identifier resource)]
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
  nil
  (transform [this context] nil))

(defmulti transform-literal (fn [literal context] (-> literal rdf/datatype rdf/value)))
(defmethod transform-literal :default [literal context] (rdf/value literal))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#dateTime" [literal context]
  (time/unparse (time/formatter "MMMM d, yyyy") (time/parse (time/formatters :date-time-no-ms) (rdf/value literal))))
(defmethod transform-literal "http://www.w3.org/2001/XMLSchema#duration" [literal context]
  (let [parser (ISOPeriodFormat/standard)
        unparser (PeriodFormat/getDefault)
        duration (.parsePeriod parser (rdf/value literal))]
    (.print unparser (.normalizedStandard duration))))

(defn literal? [object]
  (satisfies? rdf/Literal object))

(defn transform-statement [statement context]
  (let [predicate (rdf/predicate statement)
        object (rdf/object statement)]
    (condp = (rdf/identifier predicate)
      "http://dbpedia.org/property/homepage"
      (enlive/do->
        (set-reference object)
        (set-content (rdf/value object)))      
      know:externalLink
      (enlive/do->
        (set-reference object)
        (set-content (rdf/value object)))
      dbo:wikiPageExternalLink
      (enlive/do->
        (set-reference object)
        (set-content (rdf/value object)))
      know:internalLink
      (set-reference object)
      "http://xmlns.com/foaf/0.1/isPrimaryTopicOf"
      (set-reference object)
      foaf:depiction
      (set-reference object)
      schema:image
      (set-reference object)
      "http://www.w3.org/ns/ma-ont#locator"
      (set-reference object)
      (enlive/do->
        (set-content (transform object context))
        (if (literal? object)
          (enlive/do->
            (if-let [datatype (rdf/datatype object)]
              (enlive/do->
                (set-datatype datatype)
                (if-not (= (rdf/value datatype) rdf:XMLLiteral)
                  (set-content-attr (rdf/value object))
                  identity))
              identity)
            (set-language (rdf/language object)))
          identity)))))

(defn- identity-statements [context]
  (when-let [current-identity (-?> context :request :session :cemerick.friend/identity :current)]
    (let [bnode (rdf/create-resource nil)]
      [(rdf/create-statement bnode
                             (rdf/create-resource "http://www.w3.org/1999/02/22-rdf-syntax-ns#type")
                             (rdf/create-resource "http://xmlns.com/foaf/0.1/OnlineAccount"))
       (rdf/create-statement bnode
                             (rdf/create-resource "http://knowl-edge.org/ontology/core#username")
                             (rdf/create-literal current-identity))])))

(defn transform-statements [statements snippet context]
  (let [statements (into statements (identity-statements context))
        grouped-statements (group-by #(rdf/predicate %) statements)]
    (reduce
      (fn [snippet [predicate statements]]
        (enlive/at snippet [(predicate= predicate)] (enlive/do->
                                                      (set-predicate predicate)
                                                      (enlive/clone-for
                                                        [statement statements]
                                                        (transform-statement statement context)))))
      snippet grouped-statements)))

(defn- extract-types-from [statements]
  (when-let [type-statements (-> (filter #(= (-> % rdf/predicate rdf/identifier) rdf:type) statements))]
    (into #{} (map #(-> % rdf/object rdf/value) type-statements))))

(defn- extract-template-iri-from [statements]
  (when-let [statement (seq (filter #(= (-> % rdf/predicate rdf/identifier) know:template) statements))]
    (-> statement first rdf/object rdf/value)))

(defn- extract-query-from [statements]
  (-> (filter #(= (-> % rdf/predicate rdf/identifier) spin:text) statements) first rdf/object rdf/value))

(defn- extract-service-from [statements]
  (when-let [service-statements (seq (filter #(= (-> % rdf/predicate rdf/identifier) know:sparqlEndpoint) statements))]
    (-> service-statements first rdf/object rdf/value)))

(defn- extract-first [statements]
  (-> (filter #(= (-> % rdf/predicate rdf/identifier) rdf:first) statements) first rdf/object))

(defn- extract-rest [statements]
  (-> (filter #(= (-> % rdf/predicate rdf/identifier) rdf:rest) statements) first rdf/object))

(defn set-base [template]
  (enlive/at template
    [:base] (enlive/substitute {:tag :base :attrs {:href base-iri}})))

(defn- fetch-template [iri]
  (set-base (enlive/html-resource (java.net.URL. iri))))

(def fetch-template-memo (memoize fetch-template))

(defn- transform-resource* [resource statements context]
  (security/authorize
    resource
    context
    (util/pmap-set
      (fn [[resource statements]]
        (let [context (if-let [template-iri (extract-template-iri-from statements)]
                        (assoc context :template (fetch-template-memo template-iri))
                        (if (contains? context :template)
                          context
                          (assoc context :template (fetch-template-memo default-template-iri))))
              types (extract-types-from statements)
              context (assoc context :rootline (conj (:rootline context) (reduce #(conj %1 (type= %2)) #{} types)))
              snippet (enlive/transform (enlive/select (:template context) (:rootline context))
                                        [enlive/root]
                                        (enlive/do->
                                          (set-types types)
                                          (set-resource resource)))]
          (transform-statements statements snippet context)))
      (group-by #(rdf/subject %) statements))))

(declare transform-resource)

(defn transform-list [statements nodes context]
  (let [first (extract-first statements)
        rest (extract-rest statements)
        nodes (conj nodes (transform-resource first context))]
    (if (= (rdf/identifier rest) rdf:nil)
      nodes
      (transform-list (store/fetch-statements rest context) nodes context))))

(defmacro match
  [statements & clauses]
  `(condp
     (fn [pattern# statements#]
       (some (fn [statement#]
               (and
                 (or (= (nth pattern# 0) nil) (= (-> statement# rdf/subject rdf/identifier) (nth pattern# 0)))
                 (or (= (nth pattern# 1) nil) (= (-> statement# rdf/predicate rdf/identifier) (nth pattern# 1)))
                 (or (= (nth pattern# 2) nil) (= (-> statement# rdf/object rdf/identifier) (nth pattern# 2)))))
             statements#))
     ~statements
     ~@clauses))

(defn transform-resource [resource context]
  (if (< (count (:rootline context)) 6)
    (when-let [statements (store/fetch-statements resource context)]
      (match statements
             [nil rdf:first nil] (transform-list statements [] context)
             [nil rdf:type spin:Construct] (when-let [query (extract-query-from statements)]
                                             (if-let [service (extract-service-from statements)]
                                               (let [store (Endpoint. service {})]
                                                 (when-let [statements (store/find-by-query store query)]
                                                   (transform-resource* resource statements context)))
                                               (when-let [statements (store/find-by-query (:default-store context) query)]
                                                   (transform-resource* resource statements context))))
             (transform-resource* resource statements context)))))

;; Entry Point

(defn dereference
  ([context store]
    (let [resource (-?> context :request :knowledge.middleware.resource/resource)
          media-type (-?> context :representation :media-type)]
      (when-let [document (transform resource (merge context {:rootline [] :default-store store}))]
        (when-let [html (seq (enlive/emit* document))]
          (condp = media-type
            "text/html" (apply str html)
            "text/turtle" (let [root (.getDocumentElement (parser/html-dom-parse (StringReader. (apply str html))))
                                result (rdfa.core/extract-rdfa :html root (:identifier resource))
                                triples (with-meta (:triples result) {:type Graph})]
                            (turtle/transform triples :turtle))))))))

;; Fixes a problem with elive escaping strings
(in-ns 'net.cgrand.enlive-html)
(defn- xml-str [x] x)
