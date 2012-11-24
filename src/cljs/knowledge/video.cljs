(ns knowledge.video
  (:require [cljs.core :as cljs]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [knowledge.rdfa.api :as api]
            [knowledge.effects :as effects]))


(defprotocol PlayerInfo
  (current-position [player])
  (current-state [player]))

#_(defprotocol PlayerControls
  (play [player])
  (current-state [player]))

#_(deftype VideoPlayer [implementation])
