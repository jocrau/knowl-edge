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
    ring.adapter.jetty
	  ring.middleware.stacktrace
    ring.middleware.params
    [ring.util.response :only (file-response)]
    [ring.util.codec :only (url-decode)]
    ring.middleware.content-type
    ring.middleware.file-info
    ring.middleware.head)
  (:require
    [knowledge.base :as base]
    [knowledge.transformation :as transform]
    [knowledge.implementation.transformation]
    [knowledge.model :as model]
    [knowledge.implementation.model]
    [knowledge.store :as store]
    [knowledge.implementation.store]
    [com.tnrglobal.bishop.core :as bishop]))

(defn- static-file-handler [root]
  (bishop/resource {"*/*"
                    (fn [request]
                      (-> (file-response (-> request :path-info :path) {:root root})))}))

(def resource-handler
  (bishop/resource {"text/html" (fn [request] (transform/dereference (:resource request) :html))
                    "text/turtle" (fn [request] (transform/dereference (:resource request) :turtle))}))

(bishop/defroutes routes
  ["static" "img" :path] (static-file-handler "resources/public/img/")
  ["static" "img" "placeholder" :path] (static-file-handler "resources/public/img/placeholder/")
  ["static" "css" :path] (static-file-handler "resources/public/css/")
  ["static" "js" :path] (static-file-handler "resources/public/js/")
  ["templates" :path] (static-file-handler "resources/private/templates/")
  ["*"] resource-handler)

(defn- wrap-resource [handler]
  (fn [request]
    (let [uri (str (name (:scheme request))
                   "://" (:server-name request)
                   (if-let [port (:server-port request)] (str ":" port))
                   (:uri request)
                   (if-let [query-string (:query-string request)]
                     (str "?" query-string)))]
      (handler (merge-with merge request {:resource (model/create-resource uri)})))))

(def app
  (-> (bishop/handler #'routes)
    (wrap-resource)
    (wrap-file-info)
    (wrap-head)
    (wrap-params)
    (wrap-stacktrace)))

(defn -main []
  (let [port (Integer. (or (System/getenv "PORT") 8080))]
    (.start (run-jetty #'app {:port port :join? false}))))
