(ns hiccup-bridge.test.core
  (:use clojure.test
        hiccup-bridge.core))

(deftest replace-extension-test
  (is (= "hicv/aaa.html"
         (replace-extension "hicv/aaa.clj" ".html")))
  (is (= "hicv/aaa/bbb.clj"
         (replace-extension "hicv/aaa/bbb.html" ".clj"))))

(deftest add-id&classes->tag-test
  (is (= :img (add-id&classes->tag "img" {})))
  (is (= :img.myclass (add-id&classes->tag "img" {:class "myclass"})))
  (is (= :img#myid (add-id&classes->tag "img" {:id "myid"})))
  (is (= :img#myid.myclass
         (add-id&classes->tag "img" {:id "myid" :class "myclass"})))
  (is (= :img#myid.myclass1.myclass2
         (add-id&classes->tag "img" {:id "myid" :class "myclass1 myclass2"})))
  (is (= :img.foo.bar-foo
         (add-id&classes->tag "img" {:class "foo bar-foo"}))))

(deftest ensure-under-hicv-dir-test
  (is (= "hicv/aaa.html" (ensure-under-hicv-dir "hicv/aaa.html")))
  (is (= "hicv/bbb.html" (ensure-under-hicv-dir "/aaa/bbb.html")))
  (is (= "hicv/http:__google.com_aaa"
         (ensure-under-hicv-dir "http://google.com/aaa"))))

(deftest html->hiccup-test
  (is (= '([:foo [:bar "buzz"]]) (html->hiccup "<foo><bar>buzz</bar></foo>")))
  (is (= '([:foo#me.class1.class2 [:bar "buzz"]])
         (html->hiccup (str "<foo id=\"me\" class=\"class1 class2\">"
                            "<bar>buzz</bar>"
                            "</foo>")))))