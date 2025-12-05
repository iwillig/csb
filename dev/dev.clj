(ns dev
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-reload.core :as reload]
   [io.aviso.repl :as repl]
   [typed.clojure :as t]))

(repl/install-pretty-exceptions)

;; Configures the reload system
(reload/init
 {:dirs ["src" "dev" "test"]})

(defn refresh
  "Reloads and compiles he Clojure namespaces."
  []
  (reload/reload))

(defn lint
  "Lint the entire project (src and test directories)."
  []
  (-> (clj-kondo/run! {:lint ["src" "test" "dev"]})
      (clj-kondo/print!)))

(defn type-check
  "Checks the types using Clojure typed Clojure"
  []
  (t/check-dir-clj "src"))
