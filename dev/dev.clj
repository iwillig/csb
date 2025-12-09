(ns dev
  (:require
   [clj-kondo.core :as clj-kondo]
   [clj-reload.core :as reload]
   [clojure.repl :refer [doc]]
   [csb.db :as db]
   [io.aviso.repl :as repl]
   [kaocha.repl :as k]
   [typed.clojure :as t]))


(comment
  (doc k/run)
  (doc k/run-all)
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

(def conn (db/file-sqlite-database {:db_path "test.db"}))

(defn migration
  "Migrates the test.db database"
  []
  (let [db-config (db/migration-config conn)]
    (db/migrate db-config)))


(defn type-check
  "Checks the types using Clojure typed Clojure"
  []
  (t/check-dir-clj "src"))

(println (doc k/run))
(println (doc refresh))
(println (doc lint))
(println (doc type-check))
