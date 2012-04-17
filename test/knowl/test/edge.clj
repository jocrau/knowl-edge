(ns knowl.test.edge
  (:use [knowl.edge] :reload)
  (:use midje.sweet))

(def mock-request
  {:scheme :http
   :query-params {}
   :form-params {}
   :request-method :get
   :query-string "baz=boom&zack=zoom"
   :route-params {:* "/foo/bar"}
   :content-type nil
   :uri "/foo/bar"
   :server-name "localhost"
   :params {:* "/foo/bar", "baz" "boom", "zack" "zoom"}
   :headers {"accept-encoding" "gzip, deflate"
             "user-agent" "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.7; rv:11.0) Gecko/20100101 Firefox/11.0"
             "connection" "keep-alive"
             "accept-language" "en"
             "accept" "text/html"
             "host" "localhost:8080"
             "cookie" "foo"}
   :content-length nil
   :server-port 8080
   :character-encoding nil
   :body ""})

(fact "A resource can be created from a given request."
      (resource-from mock-request) => (knowl.edge.base.URI. "http://localhost:8080/foo/bar?baz=boom&zack=zoom"))
