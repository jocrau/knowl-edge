(ns knowledge.effects
   (:require [clojure.browser.dom :as dom]
             [goog.dom :as gdom]
             [cljs.core :as cljs]))

(def jquery (js* "$"))

(defn hide [elements]
  (jquery #(.hide (jquery elements))))

(defn show [elements]
  (jquery #(.show (jquery elements))))

(defn highlight [elements]
  (jquery #(.addClass (jquery elements) "highlight")))

(defn remove-all-highlights []
  (jquery #(.removeClass (jquery "*") "highlight")))

(defn initialize-tooltip [elements options]
  (let [options (assoc options :content (fn [] (this-as element ((:content options) element))))]
    (.popover (jquery elements) (cljs/clj->js options))))

(defn toggle-tooltip [element]
  (.popover (jquery element) "toggle"))

(defn remove-all-overlays []
  (jquery #(.remove (jquery ".overlay"))))

(defn add-overlay [parent-element link-iri top left width height]
  ;; TODO curate input
  (let [overlay-element (gdom/htmlToDocumentFragment (str "<a href=\"" link-iri "\" style=\"position: absolute; top: " top "%; left: " left "%; width: " width "%; height: " height "%;\" target=\"_blank\"><i style=\"position: absolute; top: 48%; left: 48%; background-color: white;\" class=\"icon-info-sign\"></i></a>"))]
    (gdom/appendChild parent-element overlay-element)))
