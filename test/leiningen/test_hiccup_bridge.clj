(ns hozumi.test-html2hiccup
  (:use [hozumi.html2hiccup] :reload)
  (:use [clojure.test])
  (:require [net.cgrand.enlive-html :as en]
	    [hiccup.core :as hic]
	    [hiccup.page-helpers :as hich]))

(defn view-layout [& content]
  (hic/html
   (hich/doctype :xhtml-strict)
   (hich/xhtml-tag
    "en"
    [:head
     [:meta {:http-equiv "Content-type"
	     :content "text/html; charset=utf-8"}]
     [:title "adder"]]
    [:body content])))

(defn view-input []
  (view-layout
    [:h2 "add two numbers"]
    [:form {:method "post" :action "/"}
      [:input.math {:type "text" :name "a"}] [:span.math " + "]
      [:input.math {:type "text" :name "b"}] [:br]
      [:input.action {:type "submit" :value "add"}]]))

(deftest test-conv
  (is false "No tests have been written."))
