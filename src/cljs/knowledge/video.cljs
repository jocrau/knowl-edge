(ns knowledge.video
  (:require [cljs.core :as cljs]
            [clojure.browser.dom :as dom]
            [goog.dom :as gdom]
            [knowledge.rdfa.api :as api]
            [knowledge.effects :as effects]))

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

(defn highlight-results [results]
  (let [mentioned-resources (map #(if-let [mentioned-resource (.-mentioned %)]
                                    (.-value mentioned-resource))
                                 results)]
    (doseq [resource mentioned-resources]
      (effects/highlight (api/get-elements-by-subject resource)))))

(defn update-related-content [player]
  #(let [current-position (.round js/Math (current-time player))]
     (if-not (= current-position position)
       (do
         #_(dom/log position)
         (set! position current-position)
         (let [query (str "SELECT ?mentioned WHERE {?s a <http://www.w3.org/ns/ma-ont#MediaFragment> ; <http://schema.org/mentions> ?mentioned ; <http://knowl-edge.org/ontology/core#start> ?start ; <http://knowl-edge.org/ontology/core#end> ?end . FILTER (?start < " position " && ?end > " position ")}")]
           (api/find-by-query query highlight-results))))))

(defn current-time [player]
  (.getCurrentTime player))

(defn ^:export on-player-ready [event]
  #_(.playVideo (.-target event)))

(defn ^:export on-player-state-change [event]
  (let [player (.-target event)]
    (js/setInterval (update-related-content player) 600)))
