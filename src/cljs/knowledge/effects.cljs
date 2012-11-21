(ns knowledge.effects
   (:require [cljs.core :as cljs]))

(def jquery (js* "$"))

(defn hide [elements]
  (jquery #(.hide (jquery elements))))

(defn show [elements]
  (jquery #(.show (jquery elements))))

(defn highlight [elements]
  (jquery #(.effect (jquery elements) "highlight")))

(defn initialize-tooltip [elements options]
  (.popover (jquery elements) (cljs/clj->js options)))

(defn toggle-tooltip [element]
  (.popover (jquery element) "toggle"))
