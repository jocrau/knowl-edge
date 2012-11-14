(ns knowledge.rdfa.api
  (:require [knowledge.store :as store]
            [knowledge.base :as base]))

(defn find-by-query [query callback] (store/find-by-query base/default-store query callback))

(set! js/document.data (fn []))
(set! js/document.data.find-by-query find-by-query)
