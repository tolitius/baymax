(ns baymax.config
  (:require [cprop.core :as cp]
            [mount.core :refer [defstate]]
            [baymax.sandbox :refer [eval-fn]]
            [clojure.string :as str])
  (:import (java.util.concurrent TimeUnit)))

(def ^:private time-units
  {"ms" TimeUnit/MILLISECONDS
   "s"  TimeUnit/SECONDS
   "m"  TimeUnit/MINUTES
   "h"  TimeUnit/HOURS
   "d"  TimeUnit/DAYS})

(defn- parse-duration
  "convert duration strings like '30s', '5m', etc. to milliseconds"
  [duration]
  (if-not (string? duration)
    duration
    (if-let [[_ amount unit] (re-matches #"(\d+)([a-z]+)" duration)]
      (let [amount (Long/parseLong amount)
            unit (get time-units unit)]
        (if unit
          (.toMillis unit amount)
          (throw (ex-info (str "unknown time unit: " unit)
                          {:duration duration}))))
      (throw (ex-info (str "invalid duration format: " duration
                           " expected format: <number><unit> (e.g. 5m, 30s)")
                      {:duration duration})))))

;; source and collector lookup functions

(defn find-collector [config cid]
  (some (fn [collector]
          (when (= cid (:id collector))
            collector))
        (:collectors config)))

(defn find-source
  "lookup a source by id from the available sources"
  [config source-id]
  (get-in config [:sources source-id]))

(defn- resolve-transformer
  "resolve collector transform function string to a function"
  [collector]
  (if-let [transform (:transform collector)]
    (assoc collector :transform (eval-fn transform))
    collector))

(defn- add-schedule
  "convert schedule string to milliseconds"
  [collector config]
  (if-let [schedule (:schedule collector)]
    (let [default-interval (get-in config [:scheduler :default-interval] "1m")
          duration (or schedule default-interval)
          interval-ms (parse-duration duration)]
      (assoc collector :interval-ms interval-ms))
    collector))

(defn parse-collector
  "process a collector configuration, resolving transform function and schedule"
  [collector config]
  (-> collector
      resolve-transformer
      (add-schedule config)))

(defn- validate-sources
  "validate that all required sources exist and are properly configured"
  [config]
  (doseq [collector (:collectors config)]
    (when-let [source-id (:source collector)]
      (when-not (find-source config source-id)
        (throw (ex-info (str "collector references non-existent source: " source-id)
                        {:collector-id (:id collector)
                         :source-id source-id})))))
  config)

(defn- validate-publishers
  "validate that all collectors referenced by publishers exist"
  [config]
  (doseq [[id publisher] (:publishers config)]
    (let [publisher-collectors (get publisher :collectors [])]
      (doseq [collector-id publisher-collectors]
        (when-not (find-collector config collector-id)
          (throw (ex-info (str "publisher references non-existent collector: " collector-id)
                          {:publisher-id id
                           :publisher-type (:type publisher)
                           :collector-id collector-id}))))))
  config)

(defn make-all
  "given config, resolve all functions, sources, collectors, transformers and schedules"
  [config]
  (let [collectors (mapv #(parse-collector % config) (:collectors config))
        scheduler (update-in (:scheduler config)
                             [:default-interval]
                             parse-duration)]
    (-> config
        validate-sources
        validate-publishers
        (assoc :collectors collectors)
        (assoc :scheduler scheduler))))

(defn load-config
  "load and process the config"
  []
  (-> (cp/load-config) ;; classpath config.edn / -Dconf="../somepath/baymax.edn" / etc..
       make-all))

(defstate config :start (load-config)
                 :stop  :stopped)
