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

(ns knowledge.syntax.turtle.serialize
  (:use knowledge.syntax.turtle)
  (:require [clojure.contrib.str-utils2 :as string]
            [knowledge.syntax.curie :as curie]))

(defmulti serialize (fn [thing format] [(type thing) format]))

(defn serialize-resource [resource]
  (let [iri (knowledge.model/identifier resource)]
    (if-let [[prefix scope] (curie/resolve-iri iri)]
      (string/replace-first iri (re-pattern scope) (str prefix ":"))
      (str "<" iri ">"))))

(defn serialize-bnode [resource]
  (str "_:" (knowledge.model/identifier resource)))

(defn serialize-literal [literal]
  (let [value (clojure.string/escape (knowledge.model/value literal)
                                     escape-characters)
        quotes (if (some #(string/contains? value %) long-string-characters) "\"\"\"" "\"")
        quoted-value (str quotes value quotes)
        tag (or (if (seq (knowledge.model/datatype literal)) (str "^^" (serialize (knowledge.model/datatype literal) :turtle)))
                (if (seq (knowledge.model/language literal)) (str "@" (knowledge.model/language literal))))]
    (str quoted-value tag)))

(defn- serialize-triples* [level grouped-triples]
  (loop [current-grouped-triples grouped-triples
         accu []]
    (if-not (seq current-grouped-triples)
      accu
      (let [grouped-triples-rest (rest current-grouped-triples)
            [prefix suffix] (nth separators level)]
        (recur
          grouped-triples-rest
          (let [[resource triples] (first current-grouped-triples)]
            (concat
              accu
              (if (> (count grouped-triples) 1) prefix)
              (serialize resource :turtle)
              (if (> (count triples) 1) "\n" " ")
              (if (< level 2)
                (serialize-triples* (+ level 1)
                                    (group-by #(nth % (+ level 1)) (into #{} triples))))
              (if (seq grouped-triples-rest) suffix))))))))

(defn prefix-definitions []
  (concat (map (fn[[prefix scope]]
                 (str "@prefix " prefix ": <" scope "> .\n"))
               curie/prefix-namespace-map)
          "\n"))

(defn serialize-triples [triples]
  (apply str (concat
               (prefix-definitions)
               (serialize-triples* 0 (group-by #(nth % 0) (into #{} triples)))
               ".")))

(defmethod serialize [rdfa.core.IRI :turtle] [thing _] (serialize-resource thing))
(defmethod serialize [rdfa.core.BNode :turtle] [thing _] (serialize-bnode thing))
(defmethod serialize [rdfa.core.Literal :turtle] [thing _] (serialize-literal thing))
(defmethod serialize [knowledge.model.Graph :turtle] [thing _] (serialize-triples thing))