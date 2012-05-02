(ns knowl.edge.base.jena
  (:import (com.hp.hpl.jena.rdf.model ModelFactory)))

(def model (ModelFactory/createDefaultModel))

(defn resolve-prefix [prefix]
  (if-let [uri (get knowl.edge.base/curies (name prefix))]
    uri
    knowl.edge/*base-iri*))

(extend-protocol knowl.edge.base/Resource
  String
  (create-resource [this] (.createResource model this))
  clojure.lang.IPersistentVector
  (create-resource [this] (let [[prefix local-name] this
                                iri (str (resolve-prefix prefix) (name local-name))]
                            (.createResource model iri)))
  clojure.lang.Keyword
  (create-resource [this] (let [iri (str knowl.edge/*base-iri* (name this))]
                            (.createResource model iri))))

(extend-protocol knowl.edge.representation/Transformer
  com.hp.hpl.jena.rdf.model.impl.ResourceImpl
  (transform [this context] (str this)))
