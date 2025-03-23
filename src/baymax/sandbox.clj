(ns baymax.sandbox
  (:require [sci.core :as sci]))

(def allowed-namespaces
  {'clojure.core {'map map, 'mapv mapv, 'filter filter, 'reduce reduce,
                  'comp comp, 'partial partial,
                  'mapcat mapcat,
                  'get get, 'get-in get-in, 'assoc assoc, 'assoc-in assoc-in,
                  'update update, 'merge merge,
                  'str str, 'vector vector, 'hash-map hash-map,
                  'count count, 'first first, 'rest rest, 'conj conj,
                  '= =, '< <, '> >, '<= <=, '>= >=}
   'clojure.string {'join clojure.string/join, 'split clojure.string/split,
                    'replace clojure.string/replace, 'upper-case clojure.string/upper-case,
                    'lower-case clojure.string/lower-case, 'trim clojure.string/trim,
                    'starts-with? clojure.string/starts-with?, 'ends-with? clojure.string/ends-with?}})

(def default-options
  {:namespaces allowed-namespaces
   :allow-def #{}                                         ;; no persistent defs
   :deny ['clojure.core/def 'clojure.core/alter-var-root] ;; juuuust in case
   :classes {}                                            ;; no java interop
   :timeout 2000                                          ;; 2 second timeout
   :max-heap-size (* 5 1024 1024)})                       ;; 5MB heap

(def secure-context
  (sci/init default-options))

(defn eval-fn [sfun]
  (try
    (let [result (sci/eval-string sfun secure-context)]
      (if (fn? result)
        result
        (fn [_] result)))
    (catch Exception e
      (println "could not run function [" sfun "] in sandbox due to:" (.getMessage e))
      (fn [_] {:error (.getMessage e)}))))
