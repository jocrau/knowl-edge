(ns knowledge.rdfa
  (:require [cljs.core :as cljs]
            [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [knowledge.core :as core]
            [knowledge.repr :as repr]
            [knowledge.dom :as kdom]))

(defn export-graph [graph]
  (let [connection (net/xhr-connection)
        representation (repr/print-triples graph)
        headers (cljs/js-obj "Content-Type" "text/turtle;charset=utf-8")]
    (net/transmit connection "http://localhost:8080/resource" "POST" representation headers)))

(declare rdfa)
(defn get-editables []
  (.getElementsByProperty rdfa "http://www.w3.org/2011/content#rest"))

(defn attach-handler [handler]
  (event/listen-once
    (dom/get-element "edit-btn")
    goog.events.EventType.CLICK
    handler))

(defn attach-editor []
  (let [elements (get-editables)]
    (.aloha (Aloha.jQuery elements))))

(defn detach-editor []
  (let [elements (get-editables)]
    (.mahalo (Aloha.jQuery elements))))

(declare edit->save)

(def save->edit
  (fn [event]
    (let [target (.-target event)]
      (dom/set-text target "Edit")
      (export-graph  (:triples (core/extract-rdfa js/document js/document :html)))
      (attach-handler edit->save)
      (detach-editor))))

(def edit->save
  (fn [event]
    (let [target (.-target event)]
      (dom/set-text target "Save")
      (attach-handler save->edit)
      (attach-editor))))

(defn attach-content-change-handler []
  (Aloha.bind
    "aloha-smart-content-changed"
    (fn [event info]
      (dom/log (str "smart edit detected " (.-obj (.-editable info)))))))

(defn init []
  (def rdfa (.init js/RDFaDOM)) ; this is just a work-around
  (attach-handler edit->save)
  (attach-content-change-handler))

(.addEventListener js/document "DOMContentLoaded" init)


