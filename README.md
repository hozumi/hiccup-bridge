# Hiccup-bridge

Hiccup-bridge is a conversion tool between html and the functions which include hiccup vector.

## Usage

### Hiccup function to html
    % lein hicv 2html
The functions which include hiccup vector are gathered and converted to html files under "hicv" directory.
    % ls -l hicv         
    -rw-r--r--  1 fatrow  staff   434 10 30 00:37 your.app.namespace.render-sidebar.html
    -rw-r--r--  1 fatrow  staff   895 10 30 00:37 your.app.namespace.render-page.html
Generated html are not indented in current version. Indent it manually.
    <c-- clj="(defn render-session-info [req] $1)">
    <c-- clj="(let [ui (users/user-info)] $1)">
    <div id="session-info">
      <c-- clj="(if (logged-in?) $1 $2)">
      <div id="login-info">Logged in as <span id="username">
        <c-- clj="(link-to (uri &quot;preferences&quot;)
	                   (current-user-name req))" />
        </span> <span class="button" id="logout-link">
        <c-- clj="(link-to (.createLogoutURL (:user-service ui) (uri))
	                 &quot;        Log out&quot;)" />
        </span></div>
      <div id="login-info"><span class="button" id="login-link">
        <c-- clj="(link-to (.createLoginURL (:user-service ui)
	           (uri &quot;preferences&quot;)) &quot;Log in&quot;)" />
        </span></div>
      </c-->
    </div>
    </c-->
    </c-->
    
### Html to hiccup function
    % lein hicv 2hic
Htmls under "hicv" directory are conveted to hiccup node and pprinted into terminal.
    
## Installation

Leiningen
    :dev-dependencies [[org.clojars.hozumi/hiccup-bridge "1.0.0-SNAPSHOT"]]

