(ns knowl.edge.base.jena
  (:import (com.hp.hpl.jena.rdf.model ModelFactory)
           (com.hp.hpl.jena.datatypes TypeMapper)))

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

(extend-protocol knowl.edge.base/Literal
  com.hp.hpl.jena.rdf.model.impl.LiteralImpl
  (value [this] (.getValue this))
  (datatype [this] (.getDatatype this))
  (language [this] (let [language (.getLanguage this)]
                     (if (clojure.string/blank? language)
                       nil
                       language))))

(extend-protocol knowl.edge.base/Statement
  com.hp.hpl.jena.rdf.model.impl.StatementImpl
  (subject [statement] (.getSubject statement))
  (predicate [statement] (.getPredicate statement))
  (object [statement] (.getObject statement)))
  
(extend-protocol knowl.edge.base/RDFFactory
  String
  (create-literal
    ([this] (.createLiteral model this))
    ([this language-or-datatype] (if (knowl.edge.base/iri-string? (name language-or-datatype))
                                   (.createTypedLiteral model this (.getTypeByName (TypeMapper/getInstance) language-or-datatype))
                                   (.createLiteral model this (name language-or-datatype))))))