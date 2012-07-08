(ns leiningen.hicv
  (:require [net.cgrand.enlive-html :as en]
            [hiccup.core :as hic]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [hozumi.det-enc :as enc]
            [org.satta.glob :as glob]))

(def ^{:private true} hicv-dir-name "hicv")

(defn- ensure-hicv-dir! []
  (let [f (io/file hicv-dir-name)]
    (if-not (.exists f)
      (.mkdir f))))

(defn- remove-extension [file-path]
  (if-let [[_ pure-file-path] (re-matches #"(.*)\..*" file-path)]
    pure-file-path
    file-path))

(defn- replace-extension [file-path extension]
  (-> file-path remove-extension (str ,,, extension)))

(defn- writeout-hiccup2html [file-path]
  (spit (replace-extension file-path ".html")
        (-> (slurp file-path :encoding (enc/detect file-path :default))
            read-string
            hic/html)))

(defn- hiccups2htmls [file-paths]
  (ensure-hicv-dir!)
  (let [file-paths (if (empty? file-paths)
                     (glob/glob (str hicv-dir-name "/**/*.clj") :s)
                     file-paths)]
    (dorun
     (map writeout-hiccup2html file-paths))))

(defn- mk-tag [tag {:keys [class id]}]
  (keyword
   (str (name tag)
        (when id
          (str "#" id))
        (when class
          (apply str
                 (interleave (repeat ".")
                             (re-seq #"\w+" class)))))))

(defn- enlive-node2hiccup [node]
  (if (map? node)
    ;; for comment node {:type :comment, :data "[if IE]> ..."}
    (if (= :comment (:type node))
      (str "<!--" (:data node) "-->")
      (let [{:keys [tag attrs content]} node
            tag (mk-tag tag attrs)
            attrs (dissoc attrs :class :id)
            hiccup-form (if (empty? attrs) [tag] [tag attrs])
            cnts (filter #(not (and (string? %)
                                    (re-matches #"\n\s*" %))) content)]
        (reduce conj hiccup-form (map enlive-node2hiccup cnts))))
    node))

(defn- url? [s]
  (re-matches #"https?://.*" s))

(defn- get-resource [resource-path]
  (if (url? resource-path)
    (java.net.URL. resource-path)
    (io/reader resource-path :encoding (enc/detect resource-path :default))))

(defn- html2hiccup [resource-path]
  (let [nodes (-> resource-path
                  get-resource
                  en/html-resource)]
    (->> (map enlive-node2hiccup nodes)
         (filter #(not (and (string? %)
                            (re-matches #"\n\s*" %))) ,,,))))

(defn- ensure-under-hicv-dir [^String resource-path]
  (if (url? resource-path)
    (apply str hicv-dir-name "/" (replace {\/ \_} resource-path))
    (if (.startsWith resource-path hicv-dir-name)
      resource-path
      (str hicv-dir-name "/"
           (or (re-find #"[^/]*$" resource-path) ;;"/ab/cd.html" => "cd.html"
               "out.html")))))

(defn- writeout-html2hiccup [resource-path]
  (spit (replace-extension (ensure-under-hicv-dir resource-path) ".clj")
        (-> resource-path
            html2hiccup
            pp/pprint
            with-out-str)))

(defn- htmls2hiccups [resource-paths]
  (ensure-hicv-dir!)
  (let [resource-paths (if (empty? resource-paths)
                     (glob/glob (str hicv-dir-name "/**/*.html") :s)
                     resource-paths)]
    (dorun
     (map writeout-html2hiccup resource-paths))))

(defn hicv
  [project & [first-arg & rest-args]]
  (condp = first-arg
    "2html" (hiccups2htmls rest-args)
    "2clj"  (htmls2hiccups rest-args)
    (println "Usage:
  lein hicv 2html
  lein hicv 2clj\n")))
