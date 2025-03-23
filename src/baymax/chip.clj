(ns baymax.chip
  (:require [clojure.tools.logging :as log]
            [baymax.source :as source]
            [baymax.publisher :as publisher]))

(defn flash [config]
  (let [sources (reduce (fn [acc [id sconfig]]
                          (assoc acc id (source/make-source sconfig)))
                        {}
                        (:sources config))

        collectors (:collectors config)

        publishers (mapv publisher/make-publisher
                         (:publishers config))]
    {:sources sources
     :collectors collectors
     :publishers publishers
     :config config}))

(defn find-collector [chip cid]
  (some (fn [collector]
          (when (= cid (:id collector))
            collector))
        (:collectors chip)))

(defn find-publishers
  "find all publishers that this collector in their list"
  [chip cid]
  (filter #(some->> %
                    :config
                    :collectors
                    (some #{cid}))
          (:publishers chip)))

