(ns knowl.edge.base)

(defrecord Statement [subject predicate object context])
(defrecord Literal [value language datatype])
(defrecord URI [value])
(defrecord BlankNode [value])
