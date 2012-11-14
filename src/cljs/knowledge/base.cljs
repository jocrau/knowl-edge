(ns knowledge.base
  (:require [cljs.core :as cljs]
            [knowledge.store :as store]
            [knowledge.rdfa :as rdfa]))

(def default-store (store/MemoryStore.
                     (js/rdfstore.Store.
                       (cljs/js-obj :name "core" :overwrite true)
                       (fn [store] (.load store "text/turtle" (rdfa/serialize (rdfa/get-triples)) nil)))
                     {}))
