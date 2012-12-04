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
  knowledge.server
  (:gen-class)
  (:use
    compojure.core
    compojure.route
    ring.adapter.jetty
	  ring.middleware.stacktrace
    ring.middleware.params)
  (:require
    [knowledge.base :as base]
    [knowledge.model :as model]
    [knowledge.implementation.model]
    [knowledge.store :as store]
    [knowledge.implementation.store]
    [knowledge.transformation :as transform]
    [knowledge.implementation.transformation]
    [com.tnrglobal.bishop.core :as bishop]))

(def page404 "<html><body><h1>Unknown Resource :-(</h1></body></html>")

(defn resource [thing]
  (cond
    (map? thing) (let [uri (str (name (:scheme thing))
                                "://" (:server-name thing)
                                (if-let [port (:server-port thing)] (str ":" port))
                                (:uri thing)
                                (if-let [query-string (:query-string thing)]
                                  (str "?" query-string)))]
                   (model/create-resource uri))
    (string? thing) (model/create-resource thing)))

(def handler
  (bishop/raw-handler
    (bishop/resource {"text/html"
                      (fn [request]
                        (if-let [response (seq (transform/dereference (resource request)))]
                          response
                          (not-found page404)))
                      "text/turtle"
                      (fn [request]
                        "foo")})))

(defroutes route
  (files "/static/" {:root "resources/public/"})
  (files "/data/" {:root "resources/private/data/" :mime-types {"ttl" "text/turtle"}})
  (files "/templates/" {:root "resources/private/templates/"})
  (POST "*" {body :body :as request}
        (store/add-statements base/default-store body {})
        {:status 200 :headers {}})
  (GET "/" {{iri "iri"} :params :as request}
       (transform/dereference (resource iri)))
  (GET "*" [:as request] handler)
  (not-found page404))

(def app
  (-> route
    (wrap-params)
    (wrap-stacktrace)))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") 8080))]
    (base/import-core-data)
    (.start (run-jetty #'app {:port port :join? false}))))
