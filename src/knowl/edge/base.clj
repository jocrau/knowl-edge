(ns knowl.edge.base
    (:use re-rand))

(defrecord Statement [subject predicate object context])
(defrecord Literal [value language datatype])
(defrecord URI [value])
(defrecord BlankNode [value])

(defn uri-instance? [thing]
  (instance? knowl.edge.base.URI))

;; This (scary) regular expression matches arbritrary URLs and URIs). It was taken from http://daringfireball.net/2010/07/improved_regex_for_matching_urls.
;; Thanks to john Gruber who made this public domain.
(def uri-regex #"(?i)\b((?:[a-z][\w-]+:(?:/{1,3}|[a-z0-9%])|www\d{0,3}[.]|[a-z0-9.\-]+[.][a-z]{2,4}/)(?:[^\s()<>]+|\(([^\s()<>]+|(\([^\s()<>]+\)))*\))+(?:\(([^\s()<>]+|(\([^\s()<>]+\)))*\)|[^\s`!()\[\]{};:'\".,<>?«»“”‘’]))")

(defn uri-string? [thing]
  (if (string? thing)
    (re-find uri-regex (name thing))
    false))

(defn create-literal
  ([^String value] (create-literal value nil))
  ([^String value lang-or-type]
    (if (nil? lang-or-type)
      (Literal. value nil nil)
      (let [datatype (cond
                       (uri-instance? lang-or-type) lang-or-type
                       (uri-string? lang-or-type) lang-or-type)
            language (when-not (seq datatype) (name lang-or-type))]
        (Literal. value language datatype)))))
  
(defn l [& params] (apply create-literal params))

(defn create-bnode
  ([] (let [identifer (re-rand #"[a-zA-Z0-9]{16}")]
        (BlankNode. identifer)))
  ([^String identifier]
    {:pre [(not (re-find #"[a-zA-Z0-9]{32}" identifier))]}
    (BlankNode. identifier)))

(defn b
  ([] (create-bnode))
  ([^String identifier] (create-bnode identifier)))
