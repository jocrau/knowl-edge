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

(ns knowledge.transformation.turtle
  (:require [knowledge.syntax.rdf :as rdf]
            [knowledge.syntax.turtle :as turtle]
            [clojure.string :as string]
            [knowledge.syntax.curie :as curie]))

(defmulti transform (fn [thing format] [(or (get (meta thing) :type) (type thing)) format])) ;; TODO Issue in core.cljs.type: added (get (meta x) :type)

(defn serialize-resource [resource]
  (let [iri (rdf/identifier resource)]
    (if-let [[prefix namespace] (curie/resolve-iri iri)]
      (str prefix ":" (subs iri (count namespace)))
      (str "<" iri ">"))))

(defn serialize-bnode [resource]
  (str "_:" (rdf/identifier resource)))

(defn serialize-literal [literal]
  (let [value (string/escape (rdf/value literal)
                             turtle/escape-characters)
        quotes (if (some #(seq (re-find (re-pattern %) value)) turtle/long-string-characters) "\"\"\"" "\"")
        quoted-value (str quotes value quotes)
        tag (or (if (seq (rdf/datatype literal)) (str "^^" (transform (rdf/datatype literal) :turtle)))
                (if (seq (rdf/language literal)) (str "@" (rdf/language literal))))]
    (str quoted-value tag)))

(defn- serialize-triples* [level grouped-triples]
  (loop [current-grouped-triples grouped-triples
         accu []]
    (if-not (seq current-grouped-triples)
      (apply str accu)
      (let [grouped-triples-rest (rest current-grouped-triples)
            [prefix suffix] (get turtle/separators level)]
        (recur
          grouped-triples-rest
          (let [[resource triples] (first current-grouped-triples)]
            (into accu
                  [(if (> (count grouped-triples) 1) prefix)
                   (transform resource :turtle)
                   (if (> (count triples) 1) "\n" " ")
                   (if (< level 2)
                     (serialize-triples* (+ level 1)
                                         (group-by #(get % (+ level 1)) (into #{} triples))))
                   (if (seq grouped-triples-rest) suffix)])))))))

(defn prefix-definitions []
  (str (apply str (map (fn[[prefix scope]]
                         (str "@prefix " prefix ": <" scope "> .\n"))
                       curie/prefix-namespace-map))
       "\n"))

(defn serialize-triples [triples]
  (apply str [(prefix-definitions)
              (serialize-triples* 0 (group-by #(get % 0) (into #{} triples)))
              "."]))

(defmethod transform [rdfa.core.IRI :turtle] [thing _] (serialize-resource thing))
(defmethod transform [rdfa.core.BNode :turtle] [thing _] (serialize-bnode thing))
(defmethod transform [rdfa.core.Literal :turtle] [thing _] (serialize-literal thing))
(defmethod transform [knowledge.syntax.rdf.Graph :turtle] [thing _] (serialize-triples thing))