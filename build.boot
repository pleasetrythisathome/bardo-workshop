(set-env!
 :dependencies (vec
                (concat
                 '[[org.clojure/clojure "1.7.0-beta2"]
                   [org.clojure/clojurescript "0.0-3123"]
                   [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                   [bardo "0.1.1-SNAPSHOT"]]
                 (mapv #(conj % :scope "test")
                       '[[adzerk/boot-cljs "0.0-2814-4"]
                         [adzerk/boot-cljs-repl "0.1.10-SNAPSHOT"]
                         [deraen/boot-cljx "0.2.0"]
                         [pandeiro/boot-http "0.6.3-SNAPSHOT"]
                         [com.cemerick/piggieback "0.1.5"]])))
 :source-paths #{"src"}
 :resource-paths #(conj % "src" "resources"))

(require
 '[adzerk.boot-cljs           :refer :all]
 '[adzerk.boot-cljs-repl      :refer :all]
 '[deraen.boot-cljx           :refer [cljx]]
 '[pandeiro.boot-http         :refer [serve]])

(deftask dev
  "watch and compile cljx, css, cljs, init cljs-repl and push changes to browser"
  []
  (comp
   (serve :dir "target/")
   (watch)
   (cljx)
   (cljs-repl :port 3458)
   (cljs :source-map true
         :pretty-print true)))
