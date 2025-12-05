(ns csb.main
  (:gen-class)
  (:require
   [typed.clojure :as t]))

(defn -main [& args]
  (println "csb" args)
  "")

(t/ann -main [(t/Seqable t/Any) :-> t/Str])
