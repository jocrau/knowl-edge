(ns knowledge.rdfa
  (:require [cljs.core :as cljs]
            [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]))

(defn ^:export test-rdfa []
  (let [nodes (.getElementsByProperty js/document "http://www.w3.org/2011/content#rest")]
    (doseq [node nodes]
      (dom/set-value node "foo"))))

(defn serialize-object [object]
  (let [value (object "value")
        language (object "language")
        datatype (object "type")]
    (cond
      (= (str datatype) "http://www.w3.org/1999/02/22-rdf-syntax-ns#PlainLiteral")
      (str "\"\"\"" value "\"\"\"@" language)
      (= (str datatype) "http://www.w3.org/1999/02/22-rdf-syntax-ns#object")
      (str "<" value ">")
      :else
      (if (seq datatype)
        (str "\"\"\"" value "\"\"\"^^<" datatype ">")))))

(defn serialize-objects [subject predicate objects]
  (loop [objects objects
         representation ""]
    (if-not (seq objects)
      representation
      (recur (rest objects)
             (str representation "<" subject "> <" predicate "> " (serialize-object (first objects)) ".\n")))))

(defn serialize-predicates [subject predicates]
  (loop [predicates predicates
         representation ""]
    (if-not (seq predicates)
      representation
      (recur (rest predicates)
             (let [predicate (ffirst predicates)
                   objects ((second (first predicates)) "objects")]
               (serialize-objects subject predicate objects))))))

(defn serialize-triples [graph]
  (loop [graph graph
         representation ""]
    (if-not (seq graph)
      representation
      (recur (rest graph)
             (let [subject (ffirst graph)
                   predicates ((second (first graph)) "predicates")]
             (str representation (serialize-predicates subject predicates)))))))

(defn export-graph [graph]
  (let [connection (net/xhr-connection)
        representation (serialize-triples (cljs/js->clj graph))
        headers (cljs/js-obj "Content-Type" "text/turtle;charset=utf-8")]
    (net/transmit connection "http://localhost:8080/resource" "POST" representation headers)))

(defn get-editables []
  (.getElementsBySubject js/document "urn:uuid:67fa60f4-96ee-4ce1-9abf-5108ec308228"))

(defn attach-handler [handler]
  (event/listen-once
    (dom/get-element "edit-btn")
    goog.events.EventType.CLICK
    handler))

(defn attach-editor []
  (let [elements (get-editables)]
    (.aloha (Aloha.jQuery elements))
    true))

(defn detach-editor []
  (let [elements (get-editables)]
    (.mahalo (Aloha.jQuery elements))
    true))

(declare edit->save)

(def save->edit
  (fn [event]
    (let [target (.-target event)]
      (dom/set-text target "Edit")
      (RDFa/attach js/document true)
      (export-graph (cljs/js->clj js/document.data._data_.triplesGraph))
      (attach-handler edit->save)
      (detach-editor)
      true)))

(def edit->save
  (fn [event]
    (let [target (.-target event)]
      (dom/set-text target "Save")
      (attach-handler save->edit)
      (attach-editor)
      true)))

(defn attach-content-change-handler []
  (Aloha.bind
    "aloha-smart-content-changed"
    (fn [event info]
      (dom/log (str "smart edit detected " (.-obj (.-editable info)))))))

(defn init []
  (attach-handler edit->save)
  (attach-content-change-handler)
  true)

(set! (.-onload js/window) init)
