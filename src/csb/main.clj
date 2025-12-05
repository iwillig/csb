(ns csb.main
  (:require [typed.clojure :as t])
  (:gen-class))

(defn -main [& args]
  (println "csb" args)
  "")

(t/ann -main [(t/Seqable t/Any) :-> t/Str])
