(ns knowl.edge
  (:use
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params
    ring.middleware.reload)
  (:require
    [compojure.route :as route]))

(defroutes route
  (ANY "*" [:as request]
       "Hello World!"))

(def app
  (-> #'route
    (wrap-reload '(knowl.edge))
    (wrap-params)
    (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))