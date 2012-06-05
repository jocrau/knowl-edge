; Copyright (c) 2012 Jochen Rau
; 
; Permission is hereby granted, free of charge, to any person obtaining a copy
; of this software and associated documentation files (the "Software"), to deal
; in the Software without restriction, including without limitation the rights
; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
; copies of the Software, and to permit persons to whom the Software is
; furnished to do so, subject to the following conditions:
; 
; The above copyright notice and this permission notice shall be included in
; all copies or substantial portions of the Software.
; 
; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
; THE SOFTWARE.

(ns
  ^{:doc "This namespace defines an entry point for the knowl:edge management system."
    :author "Jochen Rau"}
  knowl.edge.server
  (:gen-class)
  (:refer-clojure :exclude [namespace])
  (:use
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params
    knowl.edge.model
    knowl.edge.transformation)
  (:require
    [compojure.route :as route]))

(defn resource [thing]
  (cond
    (map? thing) (create-resource
                   (str (name (:scheme thing))
                        "://" (:server-name thing)
                        (:uri thing)
                        (if-let [query-string (:query-string thing)]
                          (str "?" query-string))))
    (string? thing) (create-resource thing)))

(defroutes route
  (route/files "/static/" {:root "resources/public/"})
  (GET "/resource" {{iri "iri"} :params :as request}
       (dereference (resource iri)))
  (GET "*" [:as request]
       (dereference (resource request)))
  (route/not-found "<html><body><h1>Unknown Resource :-(</h1></body></html>"))

(def app
  (-> route
    (wrap-params)
    (wrap-stacktrace)))

(defonce server
  (let [port (Integer. (or (System/getenv "PORT") 8080))]
    (run-jetty #'app {:port port :join? false})))

(defn -main []
  (do
    (knowl.edge.store/load-core-data)
    (.start server)))
