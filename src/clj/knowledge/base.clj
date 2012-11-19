(ns knowledge.base
  (:require
    [knowledge.store :as store])
  (:import (com.hp.hpl.jena.rdf.model ModelFactory)))

(def default-store (ModelFactory/createDefaultModel))

;; Helper functions

(defn import-core-data []
  (store/import-into default-store (clojure.java.io/resource "private/data/core.ttl") {}))

(defn reload-core-data []
  (do
    (store/clear-all default-store)
    (import-core-data)))

(defn export-core-data []
  (store/export-from default-store "resources/private/data/out.ttl" {}))
