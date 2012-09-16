(ns leiningen.hicv-test
  (:use clojure.test)
  (:require [leiningen.hicv :as hicv]))

(deftest replace-extension-test
  (is (= "hicv/aaa.html"
         (#'hicv/replace-extension "hicv/aaa.clj" ".html")))
  (is (= "hicv/aaa/bbb.clj"
         (#'hicv/replace-extension "hicv/aaa/bbb.html" ".clj"))))

(deftest id&class-tag-test
  (is (= :img (#'hicv/id&class-tag "img" {})))
  (is (= :img.myclass (#'hicv/id&class-tag "img" {:class "myclass"})))
  (is (= :img#myid (#'hicv/id&class-tag "img" {:id "myid"})))
  (is (= :img#myid.myclass (#'hicv/id&class-tag "img" {:id "myid" :class "myclass"})))
  (is (= :img#myid.myclass1.myclass2
         (#'hicv/id&class-tag "img" {:id "myid" :class "myclass1 myclass2"})))
  (is (= :img.foo.bar-foo
         (#'hicv/id&class-tag "img" {:class "foo bar-foo"}))))

(deftest ensure-under-hicv-dir-test
  (is (= "hicv/aaa.html" (#'hicv/ensure-under-hicv-dir "hicv/aaa.html")))
  (is (= "hicv/bbb.html" (#'hicv/ensure-under-hicv-dir "/aaa/bbb.html")))
  (is (= "hicv/http:__google.com_aaa"
         (#'hicv/ensure-under-hicv-dir "http://google.com/aaa"))))

