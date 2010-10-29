(ns leiningen.hicv
  (:use [hiccup.page-helpers])
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

(defn- html2hic* [node]
  (if (map? node)
    (let [{:keys [tag attrs content]} node
	  tag (mk-tag tag attrs)
	  attrs (dissoc attrs :class :id)
	  v (if (not (empty? attrs))
	      [tag attrs] [tag])
	  cnts (filter #(not (and (string? %)
				  (re-matches #"\n\s*" %))) content)]
      (if (and (= tag :c--)
	       (:clj attrs))
	(with-open [pbr (PushbackReader. (StringReader. (:clj attrs)))]
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

(defn- html2hic "doc" [resource]
  (let [[node] (en/html-resource resource)]
    (html2hic* node)))

(declare logged-in? current-user-name uri)
(defn render-session-info [req]
  (let [ui {}]
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
    (for [ns *ns*]
      [:li (link-to (uri ns) ns)])]
   [:h3 "Meta"]
   [:ul#meta
    [:li (link-to (uri "changes") "Recent Changes")]
    [:li (link-to (uri "guidelines") "Guidelines")]
    [:li (link-to (uri "formatting") "Formatting")]
    [:li (link-to (uri "todo") "To Do")]
    [:li (link-to "http://github.com/jkk/clj-wiki" "Source Code")]]])

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

(defn- hic2vec* [node]
;;  (println node)
  (condp #(%1 %2) node
    seq?
    (reduce conj
	    [(keyword "c--") {:clj (clj-attr node)}]
	    (map hic2vec* (filter should-be-child? node)))
    html-node? (vec (map hic2vec* node))
    node))

(defn- hic2vec [fn-sym-or-s]
  (cond
   (seq? fn-sym-or-s) (hic2vec* fn-sym-or-s)
   (symbol? fn-sym-or-s)
   (let [s (source2s fn-sym-or-s)]
     (hic2vec* s))))

(defn- mk-sym-str [targets]
;;  (do (apply use (map first targets))
  (for [[nspace & res] targets
	anfn res]
    (str nspace "/" anfn)))

(cdef/defvar- hicv-dir-name "hicv/")

(defn- prepare-hicv-dir! []
  (let [f (io/file hicv-dir-name)]
    (if-not (.exists f)
      (.mkdir f))))

(defn- list-s [path]
  (let [encoding (enc/detect path :default)]
    (with-open [pbr (-> path
			(FileInputStream.)
			(InputStreamReader. encoding)
			(PushbackReader.))]
      (doall (take-while #(not (nil? %))
			 (repeatedly
			  #(try (read pbr)
				(catch java.lang.Exception _
				  nil))))))))

(defn- ns2filename [ns-str & [fnname]]
  (let [ns-str (if fnname (str ns-str "." fnname) ns-str)
	replaced (-> ns-str
		     (.replaceAll "\\*" "(star)")
		     (.replaceAll "/" "."))]
    (str hicv-dir-name
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
    (.replaceAll n "/" ".")))

(defn- search-hic [src-path]
  (let [cljs (glob/glob (str src-path "/**/*.clj") :s)
	s-exps-map (reduce (fn [m path] (assoc m path (list-s path))) {} cljs)
	hic-fns    (for [path (keys s-exps-map)
			 exp  (s-exps-map path)]
		     (if-let [fn-name (and (should-be-child? exp)
					   (get-name exp))]
		       [(ns2filename (path2ns path src-path) fn-name) exp]))]
    (reduce conj {}
	    (filter #(not (nil? %))
		    hic-fns))))

(defn- hic2html [src-path targets]
  (prepare-hicv-dir!)
  (if targets
    (let [sym-strs (mk-sym-str targets)]
      (map #(do (spit (ns2filename %) (hic/html (hic2vec (symbol %))))
		(ns2filename %)) sym-strs))
    (let [targets (search-hic src-path)]
      (doall (map (fn [[filename s-exp]]
		    (do (spit filename (hic/html (hic2vec s-exp)))
			filename)) targets)))))

(defn html2hic-front []
  (doall (map (comp pp/pprint html2hic) (.listFiles (io/file hicv-dir-name)))))
	  
(defn hicv
  [project & [first-arg &rest-args]]
  (pr project)
  (condp = first-arg
      "2html" (hic2html (:source-path project) (:target-hiccup project))
      "2hic"  (html2hic-front)))
