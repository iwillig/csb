(ns csb.main-test
  (:require [clojure.test :as t]
            [csb.main]))

(t/deftest test-main-no-args
  (t/testing "Calling -main with no arguments"
    (t/testing "Given no arguments are passed"
      (let [output (with-out-str
                     (csb.main/-main))]
        (t/testing "When the main function is called"
          (t/testing "Then it should output 'csb nil\\n'"
            (t/is (= "csb nil\n" output))))))))

(t/deftest test-main-with-args
  (t/testing "Calling -main with arguments"
    (t/testing "Given arguments are passed"
      (let [output (with-out-str
                     (csb.main/-main "arg1" "arg2" "arg3"))]
        (t/testing "When the main function is called"
          (t/testing "Then it should output 'csb (arg1 arg2 arg3)\\n'"
            (t/is (= "csb (arg1 arg2 arg3)\n" output))))))))

(t/deftest test-main-returns-empty-string
  (t/testing "-main returns empty string"
    (t/testing "Given no arguments are passed"
      (t/testing "When the main function is called"
        (t/testing "Then it should return an empty string"
          (t/is (= "" (csb.main/-main))))))
    (t/testing "Given arguments are passed"
      (t/testing "When the main function is called"
        (t/testing "Then it should return an empty string"
          (t/is (= "" (csb.main/-main "arg1" "arg2"))))))))
