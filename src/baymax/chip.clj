(ns baymax.chip
  (:require [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]
            [baymax.config :as env]
            [baymax.source :as source]
            [baymax.publisher :as publisher]))

(defn flash [config]
  (let [sources (reduce (fn [acc [id sconfig]]
                          (assoc acc id (source/make-source sconfig)))
                        {}
                        (:sources config))

        collectors (:collectors config)

        ;; the global metrics log format travels with every publisher config
        ;; a publisher level :format, if any, wins over it
        metrics-format (env/metrics-format config)

        publishers (reduce-kv (fn [acc id pconfig]
                                (assoc acc id (publisher/make-publisher
                                                (merge {:metrics-format metrics-format}
                                                       pconfig))))
                              {}
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

(defn find-prometheus-collectors
  "find all collector ids that are associated with prometheus publishers"
  [chip]
  (->> (:publishers chip)
       (filter (fn [[_ publisher]]
                 (= :prometheus (get-in publisher [:config :type]))))
       (mapcat (fn [[_ publisher]]
                 (get-in publisher [:config :collectors])))
       (into #{})))

(defn find-publishers
  "find all publishers that this collector in their list"
  [chip cid]
  (->> (:publishers chip)
       vals
       (filter #(some->> %
                         :config
                         :collectors
                         (some #{cid})))))

(defstate chip :start (flash env/config)
               :stop  :unplugged)
