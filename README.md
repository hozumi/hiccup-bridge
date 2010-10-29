# Hiccup-bridge

Hiccup-bridge is a conversion tool between html and the functions which include hiccup vector.

## Usage

### Hiccup function to html
    % lein hicv 2html
The functions which include hiccup vector are gathered and converted to html files under "hicv" directory.
    % ls -l hicv         
    -rw-r--r--  1 fatrow  staff   434 10 30 00:37 your.app.namespace.render-sidebar.html
    -rw-r--r--  1 fatrow  staff   895 10 30 00:37 your.app.namespace.render-page.html
    %
    %
    % cat hicv/your.app.namespace.render-page.html 
    <c-- clj="(defn render-page [req title &amp; body] &quot;Render a page using
    the given title and body. Title will be escaped,\n  body will not.&quot; $1)">
    <c-- clj="(html (doctype :html5) $1)"><html><head><title><c-- clj="(str (h title)
    &quot; - &quot; (h site-title))" /></title><c-- clj="(include-css &quot;/css/style.css&quot;
    &quot;/css/shCore.css&quot; &quot;/css/shThemeDefault.css&quot;)" /><c-- clj="(include-js
    &quot;/js/jquery.js&quot; &quot;/js/shCore.js&quot; &quot;/js/shBrushClojure.js&quot;
    &quot;/js/main.js&quot;)" /></head><body><div id="page-shell"><div id="masthead"><h1>
    <c-- clj="(link-to &quot;/&quot; (h site-title))" /></h1></div><div id="content-shell">
    <c-- clj="(render-session-info req)" /><c-- clj="(render-sidebar req)" /><h2 id="page-title">
    <c-- clj="(h title)" /></h2><div id="main-content">body</div><div class="clear"></div>
    </div></div></body></html></c--></c-->
Generated html are not indented in current version. Indent manually.
    <c-- clj="(defn render-page [req title &amp; body] &quot;Render a page using the given title and body.
    Title will be escaped,\n  body will not.&quot; $1)">
    <c-- clj="(html (doctype :html5) $1)">
    <html>
    <head>
    <title>
    <c-- clj="(str (h title) &quot; - &quot; (h site-title))" />
    </title>
    <c-- clj="(include-css &quot;/css/style.css&quot; &quot;/css/shCore.css&quot;
    &quot;/css/shThemeDefault.css&quot;)" />
    <c-- clj="(include-js &quot;/js/jquery.js&quot; &quot;/js/shCore.js&quot; &quot;
    /js/shBrushClojure.js&quot; &quot;/js/main.js&quot;)" />
    </head>
    <body>
    <div id="page-shell">
      <div id="masthead">
        <h1>
          <c-- clj="(link-to &quot;/&quot; (h site-title))" />
        </h1>
      </div>
      <div id="content-shell">
        <c-- clj="(render-session-info req)" />
        <c-- clj="(render-sidebar req)" />
        <h2 id="page-title">
          <c-- clj="(h title)" />
        </h2>
        <div id="main-content">body</div>
        <div class="clear"></div>
      </div>
    </div>
    </body>
    </html>
    </c-->
    </c-->

### Html to hiccup function
    % lein hicv 2hic
Htmls under "hicv" directory are conveted to hiccup node and pprinted into terminal.
    
## Installation

Leiningen
    :dev-dependencies [[org.clojars.hozumi/hiccup-bridge "1.0.0-SNAPSHOT"]]

