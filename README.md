# Hiccup-bridge

Hiccup-bridge is a conversion tool between plain html and plain Hiccup form.

***Example***

`hicv/hello.html`

```html
<html>
  <head>
    <meta content="text/html;charset=utf-8" http-equiv="content-type" />
    <title>Hello</title>
  </head>
  <body>
    <h1>Hello World</h1>
  </body>
</html>
```

Above HTML will be converted to following and vice versa.

`hicv/hello.clj`

```clojure
([:html
  [:head
   [:meta
    {:http-equiv "content-type", :content "text/html;charset=utf-8"}]
   [:title "Hello"]]
  [:body [:h1 "Hello World"]]])
```

## Usage
### As a library
Add hiccup-bridge to your `project.clj`, then:

```clojure
(require '[hiccup-bridge.core :as hicv])

(hicv/html->hiccup "<foo><bar>buzz</bar></foo>")
;; -> ([:foo [:bar "buzz"]])
```
### Html to Hiccup

    % lein hicv 2clj

All HTML files under "hicv" directory are converted to Hiccup form.<br>

***Example***

```
% mkdir hicv
% curl https://github.com/ > hicv/github.html
% ls -l hicv
total 48
-rw-r--r--  1 fatrow  staff  22239  7  7 09:14 github.html
%
% lein hicv 2clj
%
% ls -l hicv
total 96
-rw-r--r--  1 fatrow  staff  21833  7  7 09:16 github.clj     <- generated
-rw-r--r--  1 fatrow  staff  22239  7  7 09:14 github.html
```

You can specify files you want to convert.

    % lein hicv 2clj hicv/hello.html

### Hiccup to Html

    % lein hicv 2html

***Example***

```
% ls -l hicv
total 48
-rw-r--r--  1 fatrow  staff  21833  7  7 09:16 github.clj
%
% lein hicv 2html
% ls -l hicv
total 88
-rw-r--r--  1 fatrow  staff  21833  7  7 09:16 github.clj
-rw-r--r--  1 fatrow  staff  18622  7  7 09:19 github.html    <- generated
```

## Installation to Leiningen
Add hiccup-bridge to `project.clj`.

```clojure
    :plugins [
      [hiccup-bridge "1.0.1"]
    ]
```

You can also use plugins for any project without modifying profect.clj by specifing them in your :user profile of `~/.lein/profiles.clj`.

```clojure
{:user {:plugins [[hiccup-bridge "1.0.1"]]}}
```

## License

The source code for hiccup-bridge is available under the Eclipse
Public License v 1.0.

For more information, see epl-v10.html.

(c) Copyright: Takahiro Hozumi and Hoang Minh Thang.
