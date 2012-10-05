(ns leiningen.hicv
  (:require [hiccup-bridge.core :as hic]))

(defn ^:no-project-needed hicv
  "Convert html into hiccup and vice verse"
  [project & [first-arg & rest-args]]
  (condp = first-arg
    "2html" (hic/hiccup-files->html-files rest-args)
    "2clj"  (hic/html-files->hiccup-files rest-args)
    (println "Usage:
  lein hicv 2html
  lein hicv 2clj\n")))