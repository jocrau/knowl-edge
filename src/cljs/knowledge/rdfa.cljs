(ns knowledge.rdfa
  (:require [cljs.core :as cljs]
            [clojure.browser.net :as net]
            [clojure.browser.event :as event]
            [clojure.browser.dom :as dom]
            [knowledge.core :as core]
            [knowledge.repr :as repr]
            [knowledge.dom :as kdom]))

(declare rdfa)
(declare base)

(defn export-graph [graph]
  (let [connection (net/xhr-connection)
        representation (repr/print-triples graph)
        headers (cljs/js-obj "Content-Type" "text/turtle;charset=utf-8")]
    (net/transmit connection (str base "/resource") "POST" representation headers)))

(defn get-triples []
  (let [document-element (.-documentElement js/document)
        location (.-URL js/document)]
    (:triples (core/extract-rdfa :html document-element location))))

(defn get-editables []
  (.getElementsByProperty rdfa "http://www.w3.org/2011/content#rest"))

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

(declare edit->save)

(def save->edit
  (fn [event]
    (let [target (.-target event)]
      (dom/set-text target "Edit")
      (export-graph (get-triples))
      (attach-handler "edit-btn" edit->save)
      (detach-editor))))

(def edit->save
  (fn [event]
    (let [target (.-target event)]
      (dom/set-text target "Save")
      (attach-handler "edit-btn" save->edit)
      (attach-editor))))

(def add-test-content
  (fn [event]
    (let [response (.-target event)
          content (.getResponseText response)
          parent (dom/get-element "test-content")]
      (set! (.-innerHTML parent) content))))

(def load-test-content
  (fn [event]
    (let [connection (net/xhr-connection)
          iri (.-value (dom/get-element "iri-field"))]
      (event/listen connection goog.net.EventType.COMPLETE add-test-content)
      (net/transmit connection (str base "/resource?iri=" iri) "GET"))))

(defn attach-content-change-handler []
  (Aloha.bind
    "aloha-smart-content-changed"
    (fn [event info]
      (dom/log (str "smart edit detected " (.-obj (.-editable info)))))))

(defn init []
  (def rdfa (.init js/RDFaDOM))
  (def base (.-origin (.-location js/document)))
  (attach-handler "edit-btn" edit->save)
  (attach-handler "load-btn" load-test-content)
  (attach-content-change-handler))

(.addEventListener js/document "DOMContentLoaded" init)


