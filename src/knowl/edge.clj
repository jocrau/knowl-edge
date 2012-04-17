(ns knowl.edge
  (:use    
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params
    ring.middleware.reload)
  (:require
    [compojure.route :as route]
    [knowl.edge.view :as view]
    [knowl.edge.base :as base]))

(defn resource-from [thing]
  (cond
    (map? thing)
    (base/create-uri (str (name (:scheme thing))
                          "://" (get-in thing '(:headers "host"))
                          (:uri thing)
                          (if-let [query-string (:query-string thing)]
                            (str "?" query-string))))
    (string? thing)
    (base/create-uri thing)))

(defroutes route
  (GET "/resources*" {{uri-string "uri"} :params :as request}
       (view/render (resource-from uri-string)))
  (GET "*" [:as request]
       (view/render (resource-from request))))

(def app
  (-> route
    (wrap-reload '(knowl.edge knowl.edge.base knowl.edge.view))
    (wrap-params)
    (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))