(ns bakepedia.application
  (:require [clojure.browser.dom :as dom]
            [knowledge.rdfa.api :as api]
            [knowledge.effects :as effects]
            [knowledge.implementation.youtube :as video]
            [knowledge.states :as states]))

(def jquery (js* "$"))

(defn- highlight-results [results]
  (let [mentioned-resources (map #(if-let [mentioned-resource (.-mentioned %)]
                                    (.-value mentioned-resource))
                                 results)]
    (doseq [resource mentioned-resources]
        (effects/highlight (api/get-elements-by-subject resource)))))

(defn- update-related-content [position]
    (effects/remove-all-highlights)
    (let [query (str "SELECT ?mentioned WHERE {?s a <http://www.w3.org/ns/ma-ont#MediaFragment> ; <http://schema.org/mentions> ?mentioned ; <http://knowl-edge.org/ontology/core#start> ?start ; <http://knowl-edge.org/ontology/core#end> ?end . FILTER (?start < " position " && ?end > " position ")}")]
      (api/find-by-query query highlight-results)))

(defn init []
  (do
    (effects/initialize-tooltip (api/get-elements-by-type "http://bakepedia.org/ontology#Component")
                                  {:title "Info"
                                   :content (fn [element] (.html (jquery "*[rel~=\"http://www.w3.org/2000/01/rdf-schema#seeAlso\"]" (jquery element))))
                                   :html true
                                   :trigger "hover"
                                   :placement "bottom"})
    (states/subscribe (fn [event-id] (= event-id :knowledge.video.position-changed))
                      (fn [event-id data] (update-related-content (:position data))))
    #_(states/subscribe (fn [event-id] (= event-id :knowledge.video.player-ready))
                      (fn [event-id data] (video/play (:player data))))
    #_(effects/hide (api/get-elements-by-property "http://www.w3.org/ns/ma-ont#hasFragment"))))

(.addEventListener js/document "DOMContentLoaded" init)