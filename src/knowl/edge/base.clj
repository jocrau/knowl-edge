(ns knowl.edge.base
    (:use re-rand))

(defrecord Statement [subject predicate object context])
(defrecord Literal [value language datatype])
(defrecord URI [value])
(defrecord BlankNode [value])

(defn create-bnode
  ([] (let [identifer (re-rand #"[a-zA-Z0-9]{16}")]
        (BlankNode. identifer)))
  ([^String identifier]
    {:pre [(not (re-find #"[a-zA-Z0-9]{32}" identifier))]}
    (BlankNode. identifier)))

(defn b
  ([] (create-bnode))
  ([^String identifier] (create-bnode identifier)))
