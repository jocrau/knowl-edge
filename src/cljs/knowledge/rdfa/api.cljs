(ns knowledge.rdfa.api
  (:require [knowledge.store :as store]
            [knowledge.base :as base]))

;; Data Access

(defn find-by-query [query callback] (store/find-by-query base/default-store query callback))

;; DocumentData (see http://www.w3.org/TR/rdfa-api/#document-data)

;; DataDocument (see http://www.w3.org/TR/rdfa-api/#the-document-interface)

;; Expose the API to JavaScript

(set! js/document.data (fn []))
(set! js/document.data.query find-by-query)
