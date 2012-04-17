(ns knowl.edge.view
  (:use knowl.edge.base)
  (:require
    [net.cgrand.enlive-html :as html]))

(defprotocol Transformer
  "Provides functions to generate a view of the subject."
  (render [this] "Renders the output recursively."))

(defn transform [this]
  (html/emit* {:tag :div
               :content (str "Jochen says: " this)}))

(defn transform-literal [this]
  (:value this))

(defn transform-uri [this]
  (:value this))

(extend-protocol Transformer
  knowl.edge.base.Statement
  (render [this] (transform this))
  knowl.edge.base.BlankNode
  (render [this] (transform this))
  knowl.edge.base.URI
  (render [this] (transform-uri this))
  knowl.edge.base.Literal
  (render [this] (transform-literal this))
  java.lang.String
  (render [this] (transform this)))