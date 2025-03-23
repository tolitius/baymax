(ns baymax.registry
  (:require [clojure.tools.logging :as log])
  (:import [java.util.concurrent ConcurrentHashMap]
           [java.time Instant]))

(defonce store (ConcurrentHashMap.))

(defn record-intel! [collector-id intel]
  (let [timestamp (Instant/now)
        timestamped (map #(assoc % :timestamp timestamp)
                         intel)]
    (.put store collector-id
          {:latest timestamped
           :updated-at timestamp})
    timestamped))

(defn find-latest-intel [collector-id]
  (get store collector-id))

(defn find-all-intel []
  (into {} (for [[k v] store]
             [k v])))
