(ns knowl.edge
  (:use
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params
    ring.middleware.reload)
  (:require
    [compojure.route :as route]
    [net.cgrand.enlive-html :as html]))

(defn transform [this resource]
  (html/emit* {:tag :div
               :content (str "Jochen says: " this)}))

(defroutes route
  (ANY "*" [:as request]
       (let [resource "Hello World!"]
         (transform resource request))))

(def app
  (-> #'route
    (wrap-reload '(knowl.edge))
    (wrap-params)
    (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))