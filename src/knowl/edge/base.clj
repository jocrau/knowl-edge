(ns knowl.edge.base
  (:use re-rand))

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

(defn create-uri
  ([^String value]
      {:pre [(uri-string? value)]}
      (URI. value))
  ([curie localname] (let [prefix (store/resolve-prefix curie)]
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