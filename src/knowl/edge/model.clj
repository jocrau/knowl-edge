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
  ^{:doc "This namespace provides the basic functions to manipulate RDF. It is part of the know:ledge cms."
    :author "Jochen Rau"}
  knowl.edge.model
  (:refer-clojure :exclude [namespace]))

(def ^:dynamic *base* "http://knowl-edge.net/ontology/")

(def curies {"xml" "http://www.w3.org/XML/1998/namespace"
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

(defn resolve-prefix [prefix]
  (if-let [uri (get curies (name prefix))]
    uri
    *base*))

;; This (scary) regular expression matches arbritrary URLs and URIs). It was taken from http://daringfireball.net/2010/07/improved_regex_for_matching_urls.
;; Thanks to john Gruber who made this public domain.
(def iri-regex #"(?i)\b((?:[a-z][\w-]+:(?:/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'\".,<>?«»“”‘’]))")

(defn iri-string? [thing]
  (if (and (string? thing) (re-find iri-regex (name thing)))
    true
    false))

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

(extend-type String
  Value
  (value [this] this))

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