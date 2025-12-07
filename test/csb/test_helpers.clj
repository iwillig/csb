(ns csb.test-helpers
  (:require [csb.db :as db]
            [csb.db.project :as db.project])
  (:import (org.sqlite SQLiteConnection)
           (org.sqlite.core DB)))

(def ^:dynamic *db* nil)
(def ^:dynamic *connection* nil)

(defn use-sqlite-database
  "A clojure.test fixture that sets up a in memory database
   After the test is run, this will rollback all of the migrations"
  [test-func]
  (let [conn     (db/memory-sqlite-database)
        database (.getDatabase ^SQLiteConnection conn)
        _        (.enable_load_extension ^DB database true)
        migration-config (db/migration-config conn)]
    (try
      (binding [*connection* conn
                *db*         database]
        (db/migrate migration-config)
        (test-func))
      (finally
        (db/rollback-all migration-config)
        (.close conn)))))

;; ============================================================================
;; Common Test Data Helpers
;; ============================================================================

(defn create-test-project
  "Creates a test project using db.project/create-project.
   Accepts optional name and path arguments.
   If no path is provided, generates a unique path using a random UUID."
  ([]
   (create-test-project "Test Project" (str "/path/to/project/" (random-uuid))))
  ([name path]
   (db.project/create-project
    *connection*
    {:name name
     :description "Test project description"
     :path path})))
