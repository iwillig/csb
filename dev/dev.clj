(ns dev
  (:require
   [io.aviso.repl :as repl]
   [typed.clojure :as t]
   [csb.db :as db]
   [kaocha.repl :as k]
   [clj-kondo.core :as clj-kondo]
   [clj-reload.core :as reload]))

(comment
  (k/run-all))

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

(def conn (db/file-sqlite-database "test.db"))

(defn migration
  "Migrates the test.db database"
  []
  (let [db-config (db/migration-config conn)]
    (db/migrate db-config)))


(defn type-check
  "Checks the types using Clojure typed Clojure"
  []
  (t/check-dir-clj "src"))
