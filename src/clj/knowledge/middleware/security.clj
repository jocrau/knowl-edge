;; Copyright (c) 2012 Jochen Rau
;; 
;; Permission is hereby granted, free of charge, to any person obtaining a copy
;; of this software and associated documentation files (the "Software"), to deal
;; in the Software without restriction, including without limitation the rights
;; to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
;; copies of the Software, and to permit persons to whom the Software is
;; furnished to do so, subject to the following conditions:
;; 
;; The above copyright notice and this permission notice shall be included in
;; all copies or substantial portions of the Software.
;; 
;; THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
;; IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
;; FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
;; AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
;; LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
;; OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
;; THE SOFTWARE.

(ns
  ^{:doc "This namespace defines ring middleware functions."
    :author "Jochen Rau"}
  knowledge.middleware.security
  (:require
    [knowledge.store :as store]
    [knowledge.syntax.rdf :as rdf]
    [cemerick.friend :as friend]
    (cemerick.friend [workflows :as workflows]
                     [credentials :as creds])))

(defn- find-password [store username]
  (when-let [statements (store/find-by-query store (str "
												    PREFIX foaf: <http://xmlns.com/foaf/0.1/>
														PREFIX know: <http://knowl-edge.org/ontology/core#>
														CONSTRUCT WHERE {
														  ?account a foaf:OnlineAccount ;
												               know:username \"" username "\" ;
												               know:password ?password .
														}"))]
    (let [password (some #(when (= (-> % rdf/predicate rdf/identifier)
                                   "http://knowl-edge.org/ontology/core#password")
                            (-> % rdf/object rdf/value))
                         statements)]
      password)))

(defn- credential-fn
  [store {:keys [username password]}]
  (when-let [encypted-password (find-password store username)]
    (when (creds/bcrypt-verify password encypted-password)
      {:username username})))

(defn wrap-authentication
  ([handler store]
    (friend/authenticate
      handler
      {:credential-fn (partial credential-fn store)
       :workflows [(workflows/interactive-form)]})))
