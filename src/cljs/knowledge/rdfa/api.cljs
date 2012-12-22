(ns knowledge.rdfa.api
  (:require [cljs.core :as cljs]
            [knowledge.store :as store]
            [knowledge.implementation.store]))

(declare RDFaDOM)

;; Data Access

(defn find-by-query [store query callback] (store/find-by-query store query callback))

;; DocumentData (see http://www.w3.org/TR/rdfa-api/#document-data)

;; DataDocument (see http://www.w3.org/TR/rdfa-api/#the-document-interface)

(defn get-elements-by-type [type] (.getElementsByType RDFaDOM type))
(defn get-elements-by-subject [subject] (.getElementsBySubject RDFaDOM subject))
(defn get-elements-by-property
  ([property] (.getElementsByProperty RDFaDOM property))
  ([property value] (.getElementsByProperty RDFaDOM property value)))

;; Expose the API to JavaScript

(if-not js/document.data (set! js/document.data (js* "{}")))
(set! js/document.data.query find-by-query)
(set! js/document.getElementsByType get-elements-by-type)
(set! js/document.getElementsBySubject get-elements-by-subject)
(set! js/document.getElementsByProperty get-elements-by-property)

(.addEventListener js/document "DOMContentLoaded"
  #(def RDFaDOM (.init js/RDFaDOM)))
