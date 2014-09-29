(ns hiccup-bridge.core
  "Convert html into hiccup and vice verse"
  (:require [net.cgrand.enlive-html :as en]
            [hiccup.core :as hic]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.edn :as edn]
            [det-enc.core :as det]
            [org.satta.glob :as glob]))

(def ^{:private true} hicv-dir-name "hicv")

(defn ensure-hicv-dir!
  "Make sure that hicv/ directory exist by creating it when necessary."
  []
  (let [dir (io/file hicv-dir-name)]
    (if-not (.exists dir)
      (.mkdir dir))))

(defn remove-extension
  "Remove extension from a file path."
  [file-path]
  (if-let [[_ pure-file-path] (re-matches #"(.*)\..*" file-path)]
    pure-file-path
    file-path))

(defn replace-extension
  "Replace extension of a file path with a new one."
  [file-path extension]
  (-> file-path remove-extension (str extension)))

(defn hiccup->html
  "Do convert a Clojure/hiccup data to an HTML string."
  [[x & res :as data]]
  (if (and (string? x) (re-find #"<.*>" x)) ;; x is doctype?
    (hic/html x res)
    (hic/html data)))

(defn hiccup-file->html-file
  "Do convert a Clojure/hiccup file to an HTML one."
  [file-path]
  (spit (replace-extension file-path ".html")
        (-> (slurp file-path
                   :encoding (det/detect file-path :default))
            edn/read-string
            hiccup->html)))

(defn hiccup-files->html-files
  "Batch convert many Clojure/hiccup files to HTML ones."
  [file-paths]
  (ensure-hicv-dir!)
  (let [file-paths (if (empty? file-paths)
                     (glob/glob (str hicv-dir-name "/**/*.clj") :s)
                     file-paths)]
    (dorun
     (map hiccup-file->html-file file-paths))))

(defn string->nodes [s]
  (let [nodes (-> s
                  java.io.StringReader.
                  en/html-resource)
        dtd (when (= :dtd (-> nodes first :type))
              (re-find #"<.*?>" s))]
    (if dtd
      (cons {:type :dtd :data dtd} (rest nodes))
      nodes)))

(defn add-id&classes->tag
  "Add id and classes to an HTML tag (the hiccup way)."
  [tag {:keys [class id]}]
  (keyword
   (str (name tag)
        (when id
          (str "#" id))
        (when class
          (apply str
                 (interleave (repeat ".")
                             (re-seq #"[^ ]+" class)))))))

(defn enlive-node->hiccup
  "A parser that makes hiccup data from an enlive node."
  [node]
  (if (map? node)
    (condp = (:type node)
      ;; for doctype node {:type :dtd, :data "html"}
      :dtd (:data node)
      ;; for comment node {:type :comment, :data "[if IE]> ..."}
      :comment (str "<!--" (:data node) "-->")
      (let [{:keys [tag attrs content]} node
            tag (add-id&classes->tag tag attrs)
            attrs (dissoc attrs :class :id)
            hiccup-form (if (empty? attrs) [tag] [tag attrs])
            cnts (filter #(not (and (string? %)
                                    (re-matches #"\n\s*" %))) content)]
        (reduce conj hiccup-form (map enlive-node->hiccup cnts))))
    node))

(defn html->hiccup
  "Do convert an HTML string to Clojure/hiccup data."
  [s]
  (let [nodes (string->nodes s)]
    (->> (map enlive-node->hiccup nodes)
         (filter #(not (and (string? %)
                            (re-matches #"\n\s*" %)))))))

(defn url? [s]
  (re-matches #"https?://.*" s))

(defn get-resource
  [resource-path]
  (if (url? resource-path)
    (java.net.URL. resource-path)
    (io/reader resource-path :encoding (det/detect resource-path :default))))

(defn html-file->hiccup
  "Make Clojure/hiccup data from a HTML file."
  [resource-path]
  (-> resource-path
      get-resource
      html->hiccup))

(defn ensure-under-hicv-dir
  "Check if files passed as command line arguements are under hicv/ directory."
  [^String resource-path]
  (if (url? resource-path)
    (apply str hicv-dir-name "/" (replace {\/ \_} resource-path))
    (if (.startsWith resource-path hicv-dir-name)
      resource-path
      (str hicv-dir-name "/"
           (or (re-find #"[^/]*$" resource-path) ;;"/ab/cd.html" => "cd.html"
               "out.html")))))

(defn html-file->hiccup-file
  "Do convert a html file to a Clojure/hiccup one (using io stream
  so if you want to convert strings, use html->hiccup instead)."
  [resource-path]
  (spit (replace-extension (ensure-under-hicv-dir resource-path) ".clj")
        (-> resource-path
            html-file->hiccup
            pp/pprint
            with-out-str)))

(defn html-files->hiccup-files
  "Batch convert many HTML files to Clojure/hiccup ones."
  [resource-paths]
  (ensure-hicv-dir!)
  (let [resource-paths (if (empty? resource-paths)
                         (glob/glob (str hicv-dir-name "/**/*.html") :s)
                         resource-paths)]
    (dorun
     (map html-file->hiccup-file resource-paths))))
