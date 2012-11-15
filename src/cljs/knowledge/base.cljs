(ns knowledge.base
  (:require [cljs.core :as cljs]
            [clojure.browser.dom :as dom]
            [knowledge.store :as store]
            [knowledge.rdfa :as rdfa]))

(def default-store 
  (store/MemoryStore.
    (try 
      (js/rdfstore.Store.
        (cljs/js-obj :name "core" :overwrite true)
        (fn [store] (.load store "text/turtle" (rdfa/serialize (rdfa/get-triples)) nil)))
      (catch js/Error _))
    {}))
