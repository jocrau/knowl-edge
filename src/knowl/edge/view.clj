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
  ^{:doc "This namespace provides functionailty to render a given data structure recursively. It is part of the know:ledge cms."
    :author "Jochen Rau"}  
  knowl.edge.view
  (:require
    [knowl.edge.base :as base]
    [net.cgrand.enlive-html :as html]))

(defprotocol View
  "Provides functions to generate a view of the subject."
  (render [this] "Renders the output recursively."))

(defn transform [this]
  (html/emit* {:tag :div
               :content (str "Jochen says: " this)}))

(defn transform-literal [this]
  (:value this))

(defn transform-uri [this]
  (:value this))

(extend-protocol View
  knowl.edge.base.Statement
  (render [this] (transform this))
  knowl.edge.base.BlankNode
  (render [this] (transform this))
  knowl.edge.base.URI
  (render [this] (transform-uri this))
  knowl.edge.base.Literal
  (render [this] (transform-literal this))
  java.lang.String
  (render [this] (transform this)))