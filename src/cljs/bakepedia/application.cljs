(ns bakepedia.application
  (:require [clojure.browser.dom :as dom]
            [clojure.browser.event :as event]
            [knowledge.rdfa.api :as api]
            [knowledge.effects :as effects]
            [knowledge.implementation.youtube :as video]
            [knowledge.states :as states]))

(def jquery (js* "$"))

(defn- attach-goto-handler []
  (api/find-by-query
    "
SELECT ?mentioned ?fragment ?start WHERE {
	?fragment a <http://knowl-edge.org/ontology/core#TemporalFragment> ;
		<http://schema.org/mentions> ?mentioned ;
		<http://knowl-edge.org/ontology/core#start> ?start .
}"
    (fn [results]
      (doseq [result results]
        (let [elements (api/get-elements-by-subject (-> result :mentioned :value))]
          (do
            (effects/highlight elements)
            (doseq [element elements]
              (event/listen element "click" #(-> js/player
                                               (video/goto (-> result :start :value))
                                               (video/play))))))))))

(defn- update-related-content [position] 
  (do
    (effects/remove-all-overlays)
    (effects/remove-all-highlights)
    (api/find-by-query
     (str "
PREFIX ma: <http://www.w3.org/ns/ma-ont#>
PREFIX schema: <http://schema.org/>
PREFIX know: <http://knowl-edge.org/ontology/core#>
SELECT ?video ?mentions ?top ?left ?width ?height WHERE {
	?video ma:hasFragment ?fragment .
	?fragment a ma:MediaFragment ;
		schema:mentions ?mentions ;
		know:start ?start ;
		know:end ?end .
	FILTER (?start < " position " && ?end > " position ")
	OPTIONAL { 
		?fragment know:top ?top ;
			know:left ?left ;
			know:width ?width ;
			know:height ?height .
	}
}")
      (fn [results]
        (doseq [result results]
          (do
            (let [mentiones (-> result :mentions :value)
                  elements (api/get-elements-by-subject mentiones)]
              (effects/highlight elements)
              (if-let [top (-> result :top :value)]
                (let [left (-> result :left :value)
                      width (-> result :width :value)
                      height (-> result :height :value)
                      video-element (first (api/get-elements-by-subject (-> result :video :value)))]
                  (effects/add-overlay video-element mentiones top left width height))))))))))

(defn- init []
  (do
    (effects/initialize-tooltip (api/get-elements-by-type "http://bakepedia.org/ontology#Component")
                                  {:title "Info"
                                   :content (fn [element] (.html (jquery "*[rel~=\"http://www.w3.org/2000/01/rdf-schema#seeAlso\"]" (jquery element))))
                                   :html true
                                   :trigger "hover"
                                   :placement "bottom"})
    (states/subscribe (fn [[player-id event-id]] (= event-id :knowledge.video.position-changed))
                      (fn [[player-id event-id] data] (update-related-content (:position data))))
    (states/subscribe (fn [[player-id event-id]] (= event-id :knowledge.video.player-ready))
                      (fn [[player-id event-id] data] (attach-goto-handler)))
    #_(effects/hide (api/get-elements-by-property "http://www.w3.org/ns/ma-ont#hasFragment"))))

(.addEventListener js/document "DOMContentLoaded" init)