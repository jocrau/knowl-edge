(ns knowl.edge
  (:use
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params
    ring.middleware.reload)
  (:require
    [compojure.route :as route]
    [compojure-rest.resource :as rest]))