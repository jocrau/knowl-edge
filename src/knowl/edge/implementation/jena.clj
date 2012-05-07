(ns knowl.edge.implementation.jena
  (:import (com.hp.hpl.jena.rdf.model ModelFactory)
           (com.hp.hpl.jena.datatypes TypeMapper)
           (com.hp.hpl.jena.rdf.model Model StmtIterator)
           (com.hp.hpl.jena.rdf.model.impl ModelCom)
           (com.hp.hpl.jena.query QueryExecutionFactory)
           (knowl.edge.store Endpoint)))

(def model (ModelFactory/createDefaultModel))

(extend-type com.hp.hpl.jena.rdf.model.impl.ResourceImpl
  knowl.edge.model/Value
  (value [this] (.getURI this))
  knowl.edge.model/Resource
  (identifier [this] (.getURI this))
  (namespace [this] (.getNamespace this))
  (local-name [this] (.getLocalName this))
  knowl.edge.transformation/Transformer
  (transform [this context] (knowl.edge.transformation/transform-resource this context)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDBaseNumericType
  knowl.edge.model/Value
  (value [this] (.toString this))
  knowl.edge.transformation/Transformer
  (transform [this context] (knowl.edge.transformation/transform-literal this context)))

(extend-type com.hp.hpl.jena.datatypes.xsd.impl.XSDDateTimeType
  knowl.edge.model/Value
  (value [this] (.toString this))
  knowl.edge.transformation/Transformer
  (transform [this context] (knowl.edge.transformation/transform-literal this context)))

(extend-type com.hp.hpl.jena.rdf.model.impl.LiteralImpl
  knowl.edge.model/Value
  (value [this] (.getValue this))
  knowl.edge.model/Literal
  (datatype [this] (.getDatatype this))
  (language [this] (let [language (.getLanguage this)]
                     (if (clojure.string/blank? language)
                       nil
                       language)))
  knowl.edge.transformation/Transformer
  (transform [this context] (knowl.edge.transformation/transform-literal this context)))

(extend-type com.hp.hpl.jena.rdf.model.impl.StatementImpl
  knowl.edge.model/Statement
  (subject [statement] (.getSubject statement))
  (predicate [statement] (.getPredicate statement))
  (object [statement] (.getObject statement)))

(extend-protocol knowl.edge.model/RDFFactory
  String
  (create-resource [this] (.createResource model this))
  (create-literal
    ([this] (.createLiteral model this))
    ([this language-or-datatype] (if (knowl.edge.model/iri-string? (name language-or-datatype))
                                   (.createTypedLiteral model this (.getTypeByName (TypeMapper/getInstance) language-or-datatype))
                                   (.createLiteral model this (name language-or-datatype)))))
  clojure.lang.IPersistentVector
  (create-resource [this] (let [[prefix local-name] this
                                iri (str (knowl.edge.model/resolve-prefix prefix) (name local-name))]
                            (.createResource model iri)))
  clojure.lang.Keyword
  (create-resource [this] (let [iri (str knowl.edge.model/*base* (name this))]
                            (.createResource model iri))))

(extend-type knowl.edge.store.Endpoint
  knowl.edge.store/Store
  (find-by-query
    ([this query-string] (knowl.edge.store/find-by-query this query-string (.service this)))
    ([this query-string service]
      (with-open [query-execution (QueryExecutionFactory/sparqlService service query-string)]
        (let [options (.options this)]
          (if (and (:username options) (:password options))
            (.setBasicAuthentication query-execution (:username options) (.toCharArray (:password options))))
          (iterator-seq (.listStatements (.execConstruct query-execution)))))))
  (find-by-subject
    [this resource]
    (knowl.edge.store/find-by-query this (str "CONSTRUCT { <" resource "> ?p ?o . } WHERE { <" resource "> ?p ?o . }")))
  (find-types-of
    [this resource]
    (map #(knowl.edge.model/object %) (knowl.edge.store/find-by-query this (str "CONSTRUCT { <" resource "> a ?type . } WHERE { <" resource "> a ?type . }")))))