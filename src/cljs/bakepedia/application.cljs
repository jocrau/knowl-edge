(ns bakepedia.application
  (:require [cljs.core :as cljs]
            [knowledge.rdfa.api :as api]
            [knowledge.effects :as effects]))

(defn init []
  #_(effects/hide (api/get-elements-by-property "http://www.w3.org/ns/ma-ont#hasFragment")))

(.addEventListener js/document "DOMContentLoaded" init)