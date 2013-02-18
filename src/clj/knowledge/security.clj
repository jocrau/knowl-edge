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

(ns knowledge.security
  (:require
   [knowledge.store :as store]
   [cemerick.friend :as friend]
   (cemerick.friend [workflows :as workflows]
                    [credentials :as creds])))

(defmacro authorize
  [resource context & body]
  (let [[unauthorized-info & body] (if (map? (first body)) body (cons nil body))]
    `(let [statements# (store/fetch-statements ~resource ~context)
           current-username# (-> friend/*identity* friend/current-authentication :username)
           store# (:default-store ~context)
           query# (str "
							PREFIX foaf: <http://xmlns.com/foaf/0.1/>
							PREFIX knowl: <http://knowl-edge.org/ontology/core#>
							PREFIX acl: <http://www.w3.org/ns/auth/acl#>
							ASK {{
							    <" (rdf/identifier ~resource) "> a ?typeOfResource .
							    ?acl acl:accessToClass ?typeOfResource ;
							         acl:mode acl:Read ;
							         acl:agentClass ?role .
							    ?person a ?role ;
							            foaf:account ?account .
							    ?account knowl:username \"" current-username# "\" .
							} UNION {
                  <" (rdf/identifier ~resource) "> a knowl:PublicResource .
              }}")]
       (if (store/matches? store# query#)
         (do ~@body)
         (friend/throw-unauthorized friend/*identity*
                                    (merge ~unauthorized-info
                                           {:friend/exprs (quote [~@body])}))))))
