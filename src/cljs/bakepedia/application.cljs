(ns bakepedia.application
  (:require [clojure.browser.dom :as dom]
            [knowledge.rdfa.api :as api]
            [knowledge.effects :as effects]))

(def jquery (js* "$"))

(defn init []
  (do (effects/initialize-tooltip (api/get-elements-by-type "http://bakepedia.org/ontology#Component")
                                  {:title "Info"
                                   :content (fn [element] (.html (jquery "*[rel~=\"http://www.w3.org/2000/01/rdf-schema#seeAlso\"]" (jquery element))))
                                   :html true
                                   :trigger "click"
                                   :placement "bottom"})
    #_(effects/hide (api/get-elements-by-property "http://www.w3.org/ns/ma-ont#hasFragment"))))

(.addEventListener js/document "DOMContentLoaded" init)