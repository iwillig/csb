(ns csb.db.types
  "Shared type definitions for database operations.
   
   Provides the Result type alias for Railway-Oriented Programming
   and common type utilities used across all db modules."
  (:require
   [typed.clojure :as t])
  (:import
   (failjure.core
    Failure)))

;; ============================================================================
;; Result Type for Railway-Oriented Programming
;; ============================================================================

(t/defalias Result
  "A Result type that is either a success value T or a Failure.
   
   This is the core type for Railway-Oriented Programming.
   All fallible database operations return Result<T> instead of throwing exceptions.
   
   Usage:
     (t/ann my-function [Input :-> (Result Output)])
   
   The caller must check for failure using (f/failed? result) before
   using the value, or use f/attempt-all to chain operations.
   
   Example:
     (f/attempt-all [project (get-project-by-id conn 1)
                     plans (get-plans-by-project-id conn (:id project))]
       {:project project :plans plans})
     ;; Returns either the map or the first Failure encountered"
  (t/TFn [[a :variance :covariant]]
         (t/U a Failure)))
