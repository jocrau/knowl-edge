(ns knowledge.video
  (:require [cljs.core :as cljs]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]))

(def position)

(def states
  {-1 :unstarted
   0 :ended
   1 :playing
   2 :paused
   3 :buffering
   4 :cued})

(defn current-time [player] (.getCurrentTime player))
(defn current-state [player] (get states (.getPlayerState player)))

(defn update-related-content [player]
  #(if (= (current-state player) :playing)
     (let [current-position (.round js/Math (current-time player))]
       (if-not (= current-position position)
         (do (dom/log position) (set! position current-position))))))

(defn current-time [player]
  (.getCurrentTime player))

(defn ^:export on-player-ready [event]
  (.playVideo (.-target event)))

(defn ^:export on-player-state-change [event]
  (let [player (.-target event)]
    (js/setInterval (update-related-content player) 600)))
