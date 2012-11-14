(ns knowledge.implementation.store
  (:require [knowledge.store :as store]
            [knowledge.rdfa :as rdfa]
            [clojure.browser.dom :as dom]))

(extend-type store/MemoryStore
  store/Store
  (find-by-query
    ([this query-string callback]
      (let [impl (.-model this)]
        (.execute impl query-string
          (fn [success results]
            (if success
              (callback results)
              (dom/log "No results."))))))))
