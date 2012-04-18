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

(defn- build-store [& options]
  (let [backend (MemoryStore.)
        inferencer (ForwardChainingRDFSInferencer. backend)
        store (SailRepository. inferencer)]
    (do (.initialize store)
      store)))

(def ^:dynamic *store* (build-store))

