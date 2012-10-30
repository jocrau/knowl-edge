(ns knowledge.rdfa
  (:require [cljs.core :as cljs]
            [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [rdfa.core :as core]
            [rdfa.repr :as repr]
            [rdfa.dom :as rdfadom]
            [rdfa.stddom :as stddom]))

(declare rdfa)
(declare base)

(defn export-graph [graph]
  (let [connection (net/xhr-connection)
        location (.-URL js/document)
        method "POST"
        representation (repr/print-triples graph)
        headers (cljs/js-obj "Content-Type" "text/turtle;charset=utf-8")]
    (net/transmit connection location method representation headers)))

(defn get-triples []
  (let [document-element (.-documentElement js/document)
        location (.-URL js/document)]
    (:triples (core/extract-rdfa :html document-element location))))

(defn get-editables []
  (.getElementsByType rdfa "http://rdfs.org/sioc/types#BlogPost"))

(defn attach-handler [id handler]
  (event/listen
    (dom/get-element id)
    goog.events.EventType.CLICK
    handler))

(defn attach-editor []
  (let [elements (get-editables)]
    (.aloha (Aloha.jQuery elements))))

(defn detach-editor []
  (let [elements (get-editables)]
    (.mahalo (Aloha.jQuery elements))))

(def edit
  (fn [event]
    (let [target (.-target event)]
      (if (= (gdom/getTextContent target) "Edit")
        (do
          (gdom/setTextContent target "Save")
          (attach-editor))
        (do
          (gdom/setTextContent target "Edit")
          (export-graph (get-triples))
          (detach-editor))))))

(defn attach-content-change-handler []
  (Aloha.bind
    "aloha-smart-content-changed"
    (fn [event info]
      (dom/log (str "smart edit detected " (.-obj (.-editable info)))))))

(defn init []
  (def rdfa (.init js/RDFaDOM))
  (def base (.-origin (.-location js/document)))
  (attach-handler "edit-btn" edit)
  (attach-content-change-handler))

(.addEventListener js/document "DOMContentLoaded" init)
