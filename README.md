# Hiccup-bridge

Hiccup-bridge is a conversion tool between html and the functions which include hiccup vector.

## Usage

### Hiccup function to html

    % lein hicv 2html

The functions which include hiccup vector are gathered from source files and converted to html files under "hicv" directory.

    % ls -l hicv
    -rw-r--r--  1 fatrow  staff   434 10 30 00:37 your.app.namespace.view1.html
    -rw-r--r--  1 fatrow  staff   895 10 30 00:37 your.app.namespace.view2.html

Files are created at each namespace file.
If you want to separate file, use **2htmls**

    % lein hicv 2htmls
    % ls -l hicv
    -rw-r--r--  1 fatrow  staff   434 10 30 00:37 your.app.namespace.view1.render-sidebar.html
    -rw-r--r--  1 fatrow  staff   895 10 30 00:37 your.app.namespace.view1.render-page.html
    -rw-r--r--  1 fatrow  staff   895 10 30 00:37 your.app.namespace.view2.render-my-page.html

***Example***

```clojure
(defn page [x]
  (if x
    [:p#mypage "Hello"]
    [:p#otherpage "Bye"]))
```

Above code will be converted like bellow.

```html
<hicv />
    
<c-- clj="(defn page [x] $1)">
  <c-- clj="(if x $1 $2)">
    <p id="mypage">Hello</p>
    <p id="otherpage">Bye</p>
  </c-->
</c-->
```

Generated html are not indented in current version. Indent it manually.
Hicv tag in the head is a little hack for internal parser.
Clojure code are wrapped by c-- tag.<br>

### Html to hiccup function

    % lein hicv 2hic

Htmls under "hicv" directory are converted to hiccup node and pprinted into terminal.

    % lein hicv 2hic 
    (defn page [x] (if x [:p#mypage "Hello"] [:p#otherpage "Bye"]))

You can specify files you want to print.

    % lein hicv 2hic hicv/your.app.namespace.view.html 
    (defn page [x] (if x [:p#mypage "Hello"] [:p#otherpage "Bye"]))

## Installation

Leiningen

    % lein plugin install org.clojars.hozumi/hiccup-bridge 1.0.0-SNAPSHOT

