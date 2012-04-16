(ns knowl.test.edge.base
  (:use [knowl.edge.base] :reload)
  (:use midje.sweet))

(fact "An RDF Literal can be created."
      (l "lorem ipsum") => (knowl.edge.base.Literal. "lorem ipsum" nil nil)
      (l "lorem ipsum" "en") => (knowl.edge.base.Literal. "lorem ipsum" "en" nil)
      (l "lorem ipsum" :en) => (knowl.edge.base.Literal. "lorem ipsum" "en" nil)
      (l "lorem ipsum" 'en) => (knowl.edge.base.Literal. "lorem ipsum" "en" nil)
      (l "lorem ipsum" "http://example.com/datatype") => (knowl.edge.base.Literal. "lorem ipsum" nil (knowl.edge.base.URI. "http://example.com/datatype")))

(fact "An RDF BlankNode can be created."
      (b "loremipsum") => (knowl.edge.base.BlankNode. "loremipsum")
      (:value (b)) => (just #"[a-zA-Z0-9]{12,}"))

(fact "An RDF URI can be created."
      (u "http://example.com/foo") => (knowl.edge.base.URI. "http://example.com/foo")
      (u :prefix1 "foo") => (knowl.edge.base.URI. "http://example1.com/foo")	
      (provided
        (resolve-namespace :prefix1) => "http://example1.com/")
      (u "prefix2" :foo) => (knowl.edge.base.URI. "http://example2.com/foo")
      (provided
        (resolve-namespace "prefix2") => "http://example2.com/")
      (u 'prefix3 'foo) => (knowl.edge.base.URI. "http://example3.com/foo")
      (provided
        (resolve-namespace 'prefix3) => "http://example3.com/"))

(fact "Applying str to a URI, BlankNode, or literal returns a plain string value."
      (:value (u "http://example.com/foo")) => "http://example.com/foo"
      (:value (b "loremipsum")) => "loremipsum"
      (:value (l "Lorem Ipsum" :en)) => "Lorem Ipsum")

