(ns csb.main-test
  (:require [clojure.test :as t]))

(t/deftest test-okay
  (t/testing "okay"
    (t/is (= {} #{}))))
