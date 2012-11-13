(ns knowledge.implementation.store
  (:require [knowledge.store]
            [clojure.browser.dom :as dom]))

(extend-type knowledge.store/MemoryStore
  knowledge.store/Store
  (find-by-query
    ([this query-string]
      (let [impl (.-model this)]
        (.execute impl query-string
          (fn [success results]
            (js/console.log results)))))))