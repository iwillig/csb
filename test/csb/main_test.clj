(ns csb.main-test
  (:require [clojure.test :as t]
            [csb.main]))

(t/deftest test-main-no-args
  (t/testing "Calling -main with no arguments"
    (let [output (with-out-str
                   (csb.main/-main))]
      (t/is (= "csb nil\n" output)))))

(t/deftest test-main-with-args
  (t/testing "Calling -main with arguments"
    (let [output (with-out-str
                   (csb.main/-main "arg1" "arg2" "arg3"))]
      (t/is (= "csb (arg1 arg2 arg3)\n" output)))))

(t/deftest test-main-returns-empty-string
  (t/testing "-main returns empty string"
    (t/is (= "" (csb.main/-main)))
    (t/is (= "" (csb.main/-main "arg1" "arg2")))))
