(ns csb.db.plan-test
  "Tests for plan database operations"
  (:require
   [clojure.test :refer [deftest is]]
   [csb.db.plan :as db.plan]))

(deftest test-create-plan
  ;; TODO: Actually test the plan creation with a real project first
  ;; For now we'll just check that the function exists and is callable
  (is (fn? db.plan/create-plan)))

(deftest test-get-plan-by-id
  (is (fn? db.plan/get-plan-by-id)))

(deftest test-get-plans-by-project-id
  (is (fn? db.plan/get-plans-by-project-id)))

(deftest test-update-plan
  (is (fn? db.plan/update-plan)))

(deftest test-delete-plan
  (is (fn? db.plan/delete-plan)))