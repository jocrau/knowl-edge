(ns knowl.edge.base
  (:use re-rand))

(def default-namespaces
  "The default RDF namespaces (curie => prefix)."
  {"xml" "http://www.w3.org/XML/1998/namespace"
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

(defrecord Statement [subject predicate object context])
(defrecord Literal [value language datatype])
(defrecord URI [value])
(defrecord BlankNode [value])

(defprotocol BabelFish
  "Provide functions to translate a given value into a type the Store backend can understand and vice versa."
  (translate [value]))

(declare l)
(declare u)

(defn init []
  "Initialize the RDF Store with the configured implementation)."
  (-> "src/knowl/edge/store/config.clj" slurp read-string eval))

(init)

;; This (scary) regular expression matches arbritrary URLs and URIs). It was taken from http://daringfireball.net/2010/07/improved_regex_for_matching_urls.
;; Thanks to john Gruber who made this public domain.
(def uri-regex #"(?i)\b((?:[a-z][\w-]+:(?:/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'\".,<>?«»“”‘’]))")

(defn uri-instance? [thing]
  (instance? URI))

(defn uri-string? [thing]
  (if (string? thing)
    (re-find uri-regex (name thing))
    false))

(defn resolve-prefix [curie]
  (if-let [prefix (get default-namespaces (name curie))]
    prefix
    "http://knowl-edge.net/ontology/"))

(defn create-uri
  ([^String value]
      {:pre [(uri-string? value)]}
      (URI. value))
  ([curie localname] (let [prefix (resolve-prefix curie)]
                        (URI. (str prefix (name localname))))))

(defn u [& params] (apply create-uri params))

(defn create-literal
  ([^String value] (create-literal value nil))
  ([^String value lang-or-type]
    (if (nil? lang-or-type)
      (Literal. value nil nil)
      (let [datatype (cond
                       (uri-instance? lang-or-type) lang-or-type
                       (uri-string? lang-or-type) (u lang-or-type))
            language (when-not (seq datatype) (name lang-or-type))]
        (Literal. value language datatype)))))
  
(defn l [& params] (apply create-literal params))

(defn create-bnode
  ([] (let [identifer (re-rand #"[a-zA-Z0-9]{16}")]
        (BlankNode. identifer)))
  ([^String identifier]
    {:pre [(re-find #"^[a-zA-Z0-9]{12,32}$" identifier)]}
    (BlankNode. identifier)))

(defn b
  ([] (create-bnode))
  ([^String identifier] (create-bnode identifier)))

(defn create-statement
  ([subject predicate object] (create-statement subject predicate object nil))
  ([subject predicate object context]
    (let [subject (translate subject)
          predicate (translate predicate)
          object (translate object)
          context (translate context)]
      (Statement. subject predicate object context))))

(defn s [& params] (apply create-statement params))

(extend-protocol BabelFish
  java.lang.String
  (translate [value] (if (uri-string? value) (u value) (l value)))
  clojure.lang.PersistentVector
  (translate [value] (let [[curie localname] value] (u curie localname)))
  clojure.lang.Keyword
  (translate [value] (l (name value)))
  nil
  (translate [value] nil))

(defn load-demo-data []
  "This is a little helper function to populate the in-memory store"
  (store/load-document "resources/private/data/abox.n3" :n3)
  #_(store/load-document "http://sebastian.kurfuerst.eu/index.tt" :ttl)
  #_(store/load-document "http://www.heppnetz.de/ontologies/goodrelations/v1.owl" :xml)
  #_(store/load-document "http://www.w3.org/People/Berners-Lee/card#i" :xml)
  #_(store/load-document "http://dig.csail.mit.edu/2008/webdav/timbl/foaf.rdf" :xml))

