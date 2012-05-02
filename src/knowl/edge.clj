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
  ^{:doc "This namespace defines an entry point for the know:ledge management system."
    :author "Jochen Rau"}
  knowl.edge
  (:gen-class)
  (:use    
    compojure.core
    ring.adapter.jetty
    ring.middleware.stacktrace
    ring.middleware.params)
  (:require
    [compojure.route :as route]
    [knowl.edge.representation :as representation]
    [knowl.edge.base :as base]))

(def ^:dynamic *base-iri* "http://knowl-edge.net/ontology/")

(defn resource [thing]
  (cond
    (map? thing) (base/create-resource
                   (str (name (:scheme thing))
                        "://" (get-in thing '(:headers "host"))
                        (:uri thing)
                        (if-let [query-string (:query-string thing)]
                          (str "?" query-string))))
    (string? thing) (base/create-resource thing)))

(defroutes route 
  (GET "/resources*" {{uri-string "uri"} :params :as request}
       (knowl.edge.representation/process (resource uri-string)))
  (GET "*" [:as request]
       (knowl.edge.representation/process (resource request)))
  (route/files "/" {:root "resources/public/"})
  (route/not-found "<h1>Unknown Resource :-(</h1>"))

(def app
  (-> route
    (wrap-params)
    (wrap-stacktrace)))

(defn boot []
  (run-jetty #'app {:port 8080}))

(defn -main [& args]
  (boot))

;; Load implementations (should be configurable)
(use 'knowl.edge.base.jena)