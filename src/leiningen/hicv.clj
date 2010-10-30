(ns leiningen.hicv
  (:use [hiccup.page-helpers])
  (:use [clojure.contrib.classpath])
  (:refer-clojure :exclude [pop!])
  (:require [net.cgrand.enlive-html :as en]
	    [hiccup.core :as hic]
	    [clojure.contrib.def :as cdef :only [defvar-]]
	    [clojure.java.io :as io]
	    [clojure.pprint :as pp]
	    [hozumi.det-enc :as enc]
	    [pattern-match :as pat]
	    ;[hiccup.page-helpers :as hich])
	    [org.satta.glob :as glob]
	    )
  (:import [java.util.regex Pattern]
	   [java.io StringReader PushbackReader
	    FileInputStream InputStreamReader LineNumberReader]))

(cdef/defvar- *clj-tag* :c--)
(cdef/defvar- *clj-attr-key* :clj)
(cdef/defvar- *attr-code-prefix* "c--")

(defn- mk-tag [tag {:keys [class id]}]
  (keyword
   (str (name tag)
	(when id
	  (str "#" id))
	(when class
	  (apply str
		 (interleave (repeat ".")
			     (re-seq #"\w+" class)))))))

(defprotocol Stream (pop! [this]))

(defn- stream [lst]
  (let [alst (atom lst)]
    (reify Stream
	   (pop! [this] (let [[fs] @alst]
			 (swap! alst rest)
			 fs)))))

(defn read-from-str [s-str]
  (with-open [pbr (-> s-str (StringReader.) (PushbackReader.))]
    (read pbr)))

(defn- attr-solve [attrs]
  (reduce conj {}
	  (map (fn [[k v]]
		 [(if-let [[_ c] (re-matches
				  (Pattern/compile (str *attr-code-prefix* "(\\.*)"))
				  (name k))]
			      (read-from-str c) k)
		  (if-let [[_ c] (re-matches
				  (Pattern/compile (str *attr-code-prefix* "(\\.*)"))
				  v)]
		    (read-from-str c) v)]) attrs)))

(defn- html2hic* [node]
  (if (map? node)
    (let [{:keys [tag attrs content]} node
	  tag (mk-tag tag attrs)
	  attrs (dissoc attrs :class :id)
	  attrs (attr-solve attrs)
	  v (if (not (empty? attrs))
	      [tag attrs] [tag])
	  cnts (filter #(not (and (string? %)
				  (re-matches #"\n\s*" %))) content)]
      (if (and (= tag *clj-tag*)
	       (*clj-attr-key* attrs))
	(with-open [pbr (PushbackReader. (StringReader. (*clj-attr-key* attrs)))]
	  (let [s (read pbr)]
	    (cond
	     (seq? s)  (let [cntsstream (stream (map html2hic* cnts))]
			 (map #(if (and (symbol? %)
					(= \$ (first (str %))))
				 (pop! cntsstream) %)
			      s));;(concat s (map html2hic* cnts))
	     (coll? s) (reduce conj s (map html2hic* cnts))
	     :else     s)))
	(reduce conj v (map html2hic* cnts))))
    node))

(defn- source2s
  [x]
  (when-let [v (resolve x)]
    (when-let [filepath (:file (meta v))]
      (with-open [rdr (-> filepath
			  (FileInputStream. ,,,)
			  (InputStreamReader. ,,,)
			  (LineNumberReader. ,,,))]
	(dotimes [_ (dec (:line (meta v)))] (.readLine rdr))
	(with-open [pbr (PushbackReader. rdr)]
	  (read (PushbackReader. pbr)))))))

(defn- html-node? [s]
  (and (vector? s)
       (keyword? (first s))))

(defn- tree-search [pred node]
  (letfn [(inner [s q]
		 ;;(println s :q q)
		 (if-let [v (pred s)]
		   v
		   (cond
		    (map? s)
		    (let [[fs & res] (reduce conj q
					     (concat (keys s)
						     (vals s)))]
		      (recur fs (vec res)))
		    
		    (coll? s)
		    (let [[fs & res] (reduce conj q s)]
		      (recur fs (vec res)))

		    :else
		    (if (empty? q)
		      nil
		      (recur (first q) (vec (rest q)))))))]
    (inner node [])))

(defn- should-be-child? [node]
  (tree-search html-node? node))
;;(or (seq? node) (html-node? node)))

(defn- clj-attr [node]
  (with-out-str
    (pr (let [idxstream (stream (iterate inc 1))]
	  (map #(if (should-be-child? %)
		  (symbol (str "$" (pop! idxstream))) %)
	       node)))))
	;;(let [idx (atom 0)]
	;;  (map #(if (should-be-child? %)
		;;  (symbol (str "$" (swap! idx inc))) %)
	;;node)))))

(defn- attr-code [code]
  (with-out-str
    (print *attr-code-prefix*)
    (pr code)))

(defn- hic2vec* [node]
;;  (println node)
  (condp #(%1 %2) node
    seq?
    (reduce conj
	    [*clj-tag* {*clj-attr-key* (clj-attr node)}]
	    (map hic2vec* (filter should-be-child? node)))
    html-node? (vec (map hic2vec* node))
    map?  (reduce conj {}
		  (map (fn [[k v]]
			 [(if (keyword? k) k (keyword (attr-code k)))
			  (if (string? v) v (attr-code v))]) node))
    node))

(defn- hic2vec [fn-sym-or-s]
  (cond
   (seq? fn-sym-or-s) (hic2vec* fn-sym-or-s)
   (symbol? fn-sym-or-s)
   (let [s (source2s fn-sym-or-s)]
     (hic2vec* s))))

(cdef/defvar- *hicv-dir-name* "hicv/")

(defn- prepare-hicv-dir! []
  (let [f (io/file *hicv-dir-name*)]
    (if-not (.exists f)
      (.mkdir f))))

(defn- list-s [path]
  (let [encoding (enc/detect path :default)]
    (with-open [pbr (-> path
			(FileInputStream.)
			(InputStreamReader. encoding)
			(PushbackReader.))]
      (doall (take-while identity
			 (repeatedly
			  #(try (read pbr)
				(catch java.lang.Exception _
				  nil))))))))

(defn- ns2filename [ns-str]
  (let [replaced (.replaceAll ns-str "/" ".")]
    (str *hicv-dir-name*
	 replaced
	 ".html")))

(defn- get-name [exp]
  (let [expanded (macroexpand exp)]
    (pat/match expanded
	       v :when (not (coll? v)) nil
	       [fs x & _] :when (= fs 'def) x
	       _ nil)))

(defn- path2ns [path src-path]
  (let [src-path (if (= \/ (last src-path))
		   src-path
		   (str src-path \/))
	p (Pattern/compile (str src-path "(.*)\\.clj"))
	[_ n] (re-matches p path)]
    (-> n
	(.replaceAll ,,, "_" "-")
	(.replaceAll ,,, "/" "."))))

(defn- search-hic [src-path]
  (filter (fn [[_ hics]] (not (empty? hics)))
	  (for [file-path (glob/glob (str src-path "/**/*.clj") :s)]
	    [(path2ns file-path src-path)
	     (filter identity
		     (for [exp (list-s file-path)]
		       (if (and (should-be-child? exp) (get-name exp))
			 exp)))])))

(defn- mk-syms [nspace hic-names]
  (map #(symbol (str nspace "/" %)) hic-names))

(defn- hic2html [src-path targets]
  (prepare-hicv-dir!)
  (doseq [[nspace exps] (search-hic src-path)]
    (do (with-open [f (io/writer (ns2filename nspace))]
	  (doto f (.write "<hicv />") (.newLine) (.newLine)));; (io/spit (ns2filename nspace) "<hicv />\n\n")
	(with-open [f (io/writer (ns2filename nspace) :append true)]
	  (doseq [exp exps]
	    (doto f (.write (hic/html (hic2vec exp))) (.newLine) (.newLine)))))))
;;(println (io/append-spit (ns2filename nspace)
;;(str (hic/html (hic2vec (symbol hic))) "\n\n\n")))))))

(defn- html2hic [resource]
  (let [nodes (-> resource en/html-resource first :content)]
    (map html2hic* nodes)))

(defn html2hic-front []
  (doall (map pp/pprint
	      (filter #(not (and (string? %)
				 (re-matches #"\n\s*" %)))
		      (mapcat html2hic (.listFiles (io/file *hicv-dir-name*)))))))

(defn hicv
  [project & [first-arg &rest-args]]
  (condp = first-arg
      "2html" (hic2html (:source-path project) (:target-hiccup project))
      "2hic"  (html2hic-front)
      (println "Usage:
  lein hicv 2html
  lein hicv 2hic\n")))

(declare user-info logged-in? uri current-user-name ns-list)
(defn render-session-info [req]
  (let [ui (user-info)]
    [:div#session-info
     (if (logged-in?)
       [:div#login-info "Logged in as "
        [:span#username (link-to (uri "preferences")
                                 (current-user-name req))] " "
        [:span#logout-link.button
         (link-to (.createLogoutURL (:user-service ui) (uri)) "Log out")]]
       [:div#login-info
        [:span#login-link.button
         (link-to (.createLoginURL (:user-service ui) (uri "preferences"))
                  "Log in")]])]))

(defn render-sidebar [req]
  [:div#sidebar
   [:h3 "Namespaces"]
   [:ul#ns-list
    (for [ns ns-list]
      [:li (link-to (uri ns) ns)])]
   [:h3 "Meta"]
   [:ul#meta
    [:li (link-to (uri "changes") "Recent Changes")]
    [:li (link-to (uri "guidelines") "Guidelines")]
    [:li (link-to (uri "formatting") "Formatting")]
    [:li (link-to (uri "todo") "To Do")]
    [:li (link-to "http://github.com/jkk/clj-wiki" "Source Code")]]])