(ns knowl.edge
  (:use
    knowl.edge.view
    knowl.edge.base
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params
    ring.middleware.reload)
  (:require
    [compojure.route :as route]))

(defroutes route
  (GET "*" [:as request]
       (let [resource "Hello World!"]
         (render resource))))

(def app
  (-> #'route
    (wrap-reload '(knowl.edge knowl.edge.base knowl.edge.view))
    (wrap-params)
    (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))