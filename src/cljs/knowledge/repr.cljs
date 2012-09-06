(ns knowledge.repr
  (:require [knowledge.core]))

(defn repr-term [term]
  (condp = (type term)
    knowledge.core.IRI (str "<" (:id term) ">")
    knowledge.core.Literal (let [{value :value tag :tag} term
                  qt (if (re-find #"\n|\"" value) "\"\"\"", \")]
              (str qt value qt
                   (cond
                     (= (type tag) knowledge.core.IRI) (str "^^" (repr-term tag))
                     (not-empty tag) (str "@" tag))))
    knowledge.core.BNode (str "_:" (:id term))
    (throw (js/Error. (str "Cannot repr term: " term "(type: " (type term) ")")))))

(defn repr-triple [[s p o]]
  (str (repr-term s) " " (repr-term p) " " (repr-term o) " ."))

(defn print-triples [triples]
  (loop [triples triples
         representation ""]
    (if (seq triples)
      (recur (rest triples)
             (str representation (repr-triple (first triples))))
      representation)))
