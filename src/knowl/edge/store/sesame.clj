(ns knowl.edge.store.sesame
  (:import 
    (org.openrdf.repository Repository RepositoryException)
    (org.openrdf.repository.sail SailRepository)
    (org.openrdf.sail.memory MemoryStore)
    (org.openrdf.sail.inferencer.fc ForwardChainingRDFSInferencer)
    (org.openrdf.repository RepositoryConnection)
    (org.openrdf.rio Rio RDFFormat)
    (org.openrdf.rio.turtle TurtleWriter)
    (org.openrdf.query TupleQuery TupleQueryResult BindingSet QueryLanguage GraphQueryResult)))

(def no-contexts (make-array org.openrdf.model.URI 0))

(def format-map
  {:xml RDFFormat/RDFXML
   :ntriples RDFFormat/NTRIPLES
   :n3 RDFFormat/N3
   :turtle RDFFormat/TURTLE
   :ttl RDFFormat/TURTLE
   :trig RDFFormat/TRIG
   :trix RDFFormat/TRIX})

(defn- translate-format
  "Translates an RDF format from a clojure keyword into the sesame object."
  [value] (value format-map))

(defn- build-repository [& options]
  (let [backend (MemoryStore.)
        inferencer (ForwardChainingRDFSInferencer. backend)
        repository (SailRepository. inferencer)]
    (do (.initialize repository)
      repository)))

(def ^:dynamic *repository* (build-repository))

(defn load-document
  ([source]
    (load-document source :xml "" no-contexts))
  ([source format]
    (load-document source format "" no-contexts))
  ([source format baseUri]
    (load-document source format baseUri no-contexts))
  ([source format baseUri contexts]
    (let [connection (.getConnection *repository*)
          format (or (translate-format format)
                     (translate-format :xml))]
      (try
        (.setAutoCommit connection false)
        (.add connection
              (java.io.StringBufferInputStream. (slurp source :encoding "ISO-8859-1")) ;; TODO auto-detect encoding
              baseUri format contexts)
        (.commit connection)
        (catch RepositoryException e (.rollback connection))
        (finally (.close connection))))))
