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
  (:refer-clojure :exclude [namespace]))

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
