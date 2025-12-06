(ns csb.main-test
  (:require [clojure.test :as t]
            [csb.test-helpers :as test-helper]))

(t/use-fixtures :each test-helper/use-sqlite-database)

(t/deftest test-okay
  (t/testing "okay"
    (t/is (nil? test-helper/*connection*))
    (t/is (= {} #{}))))
