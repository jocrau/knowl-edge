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
  knowledge.model
  (:refer-clojure :exclude [namespace])
  (:require [clojure.contrib.str-utils2 :as string]))

(def ontology-base-iri "http://knowl-edge.net/ontology/core#")

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

(defprotocol Statement
  (subject [statement])
  (predicate [statement])
  (object [statement]))

(extend-type nil
  Value
  (value [this] nil)
  Resource
  (identifier [this] nil)
  (namespace [this] nil)
  (local-name [this] nil)
  Literal
  (datatype [this] nil)
  (language [this] nil))
  

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
(def know:query (str know "query"))
(def know:sparqlEndpoint (str know "sparqlEndpoint"))
(def know:internalLink (str know "internalLink"))
(def know:externalLink (str know "externalLink"))
(def know:template (str know "template"))
(def schema "http://schema.org/")
(def schema:image (str schema "image"))
(def schema:encoding (str schema "encoding"))
(def spin:Construct "http://spinrdf.org/sp#Construct")
(def dbo:wikiPageExternalLink "http://dbpedia.org/ontology/wikiPageExternalLink")
(def bibo:Webpage "http://purl.org/ontology/bibo/Webpage")

(def curies {"" "urn:uuid:"
             "xml" "http://www.w3.org/XML/1998/namespace"
             "xmlns" "http://www.w3.org/2000/xmlns/"
             "xsd" "http://www.w3.org/2001/XMLSchema#"
             "xhv" "http://www.w3.org/1999/xhtml/vocab#"
             "rdfa" "http://www.w3.org/ns/rdfa#"
             "rdf" "http://www.w3.org/1999/02/22-rdf-syntax-ns#"
             "rdfs" "http://www.w3.org/2000/01/rdf-schema#"
             "owl" "http://www.w3.org/2002/07/owl#"
             "rif" "http://www.w3.org/2007/rif#"
             "skos" "http://www.w3.org/2004/02/skos/core#"
             "skosxl" "http://www.w3.org/2008/05/skos-xl#"
             "grddl" "http://www.w3.org/2003/g/data-view#"
             "sd" "http://www.w3.org/ns/sparql-service-description#"
             "wdr" "http://www.w3.org/2007/05/powder#"
             "wdrs" "http://www.w3.org/2007/05/powder-s#"
             "sioc" "http://rdfs.org/sioc/ns#"
             "cc" "http://creativecommons.org/ns#"
             "vcard" "http://www.w3.org/2006/vcard/ns#"
             "schema" "http://schema.org/"
             "void" "http://rdfs.org/ns/void#"
             "dc" "http://purl.org/dc/elements/1.1/"
             "dcterms" "http://purl.org/dc/terms/"
             "dbr" "http://dbpedia.org/resource/"
             "dbp" "http://dbpedia.org/property/"
             "dbo" "http://dbpedia.org/ontology/"
             "foaf" "http://xmlns.com/foaf/0.1/"
             "geo" "http://www.w3.org/2003/01/geo/wgs84_pos#"
             "gr" "http://purl.org/goodrelations/v1#"
             "cal" "http://www.w3.org/2002/12/cal/ical#"
             "og" "http://ogp.me/ns#"
             "v" "http://rdf.data-vocabulary.org/#"
             "bibo" "http://purl.org/ontology/bibo/"
             "cnt" "http://www.w3.org/2011/content#"})

;; This (scary) regular expression matches arbritrary URLs and URIs). It was taken from http://daringfireball.net/2010/07/improved_regex_for_matching_urls.
;; Thanks to john Gruber who made this public domain.
(def iri-regex #"(?i)\b((?:[a-z][\w-]+:(?:/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'\".,<>?«»“”‘’]))")

(defn iri-string? [thing]
  (and (string? thing) (re-find iri-regex (name thing))))

(defn resolve-prefix [prefix]
  (if-let [iri (get curies (name prefix))]
    iri
    prefix))

(defn resolve-iri [iri]
  {:pre [(iri-string? iri)]}
  (first (filter (fn [[prefix scope]] (.startsWith iri scope)) curies)))

(defn iri->curie [iri]
  {:pre [(iri-string? iri)]}
  (if-let [[prefix scope] (resolve-iri iri)]
    (string/replace-first iri (re-pattern scope) (str prefix ":"))
    iri))
