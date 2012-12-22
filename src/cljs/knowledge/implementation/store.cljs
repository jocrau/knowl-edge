(ns knowledge.implementation.store
  (:require [cljs.core :as cljs]
            [knowledge.store :as store]
            [knowledge.rdfa :as rdfa]))

(extend-type store/MemoryStore
  store/Store
  (find-by-query
    ([this query-string callback]
        (if-let [impl (.-model this)]
          (.execute impl query-string
            (fn [success results]
              (if success
                (callback (cljs/js->clj results :keywordize-keys true)))))))))

(defn create-default-store []
  (store/MemoryStore.
      (js/rdfstore.Store.
        (cljs/js-obj :name "core" :overwrite true)
        (fn [store] (.load store "text/turtle" (rdfa/serialize (rdfa/get-triples)) nil)))
    {}))