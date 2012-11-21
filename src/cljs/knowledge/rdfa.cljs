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

(declare base)

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn ^:export get-triples []
  (let [document-element (.-documentElement js/document)
        location (.-URL js/document)]
    (:triples (core/extract-rdfa :html document-element location))))

(defn ^:export serialize [triples]
  (repr/print-triples triples))

(defn export-graph []
  (let [connection (net/xhr-connection)
        location (.-URL js/document)
        method "POST"
        representation (serialize (get-triples))
        headers (cljs/js-obj "Content-Type" "text/turtle;charset=utf-8")]
    (net/transmit connection location method representation headers)))

(defn get-editables []
  (knowledge.rdfa.api/get-elements-by-type "http://rdfs.org/sioc/types#BlogPost"))

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
          #_(attach-editor))
        (do
          (gdom/setTextContent target "Edit")
          (export-graph)
          #_(detach-editor))))))

(defn attach-content-change-handler []
  #_(Aloha.bind
    "aloha-smart-content-changed"
    (fn [event info]
      (dom/log (str "smart edit detected " (.-obj (.-editable info)))))))

(defn init []
  (do 
    (def base (.-origin (.-location js/document)))
    (attach-handler "edit-btn" edit)
    (attach-content-change-handler)))

(.addEventListener js/document "DOMContentLoaded" init)
