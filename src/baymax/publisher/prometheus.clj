(ns baymax.publisher.prometheus
  (:require [baymax.publisher.proto :refer [Publisher]]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:import [io.prometheus.client Gauge GaugeMetricFamily CollectorRegistry Collector SimpleCollector]
           [io.prometheus.client.exporter.common TextFormat]))

(defrecord PrometheusPublisher [config]
  Publisher
  (publish [this collector-id intel]
    :to-implement)

  (health-check [this]
    {:healthy true
     :status "to be implemented"
     :message "stdout is 6 feet above"})

  (disconnect [this]
    {:status "disconnected"}))

(defn make-publisher [config]
  (log/info "making prometheus publisher")
  (->PrometheusPublisher config))

(defn name->prometheus [metric-name]
  (-> (if (keyword? metric-name)
        (name metric-name)
        (str metric-name))
      (clojure.string/replace #"[^a-zA-Z0-9_]" "_")))

(defn metrics->prometheus
  "clojure maps to prometheus metrics"
  [metrics]
  (let [registry (CollectorRegistry.)]

    ;; group metrics by name
    (let [by-name (group-by :name metrics)]
      (doseq [[metric-name metrics-group] by-name]
        (let [safe-name (name->prometheus (str metric-name))
              first-metric (first metrics-group)
              label-keys (keys (:labels first-metric))
              sanitized-label-keys (map (fn [k]
                                          [(name->prometheus k) k])
                                        label-keys)

              ;; create a simple counter
              gauge (-> (Gauge/build)
                        (.name safe-name)
                        (.help (str "Metric: " safe-name))
                        (.labelNames (into-array String (map first sanitized-label-keys)))
                        (.register registry))]

          ;; add each sample with its labels
          (doseq [{:keys [value labels]} metrics-group]
            (let [label-values (map (fn [[sanitized original]]
                                      (str (get labels original)))
                                    sanitized-label-keys)]
              (-> gauge
                  (.labels (into-array String label-values))
                  (.set (double value))))))))

    ;; export registry to string
    (let [writer (java.io.StringWriter.)]
      (TextFormat/write004 writer (.metricFamilySamples registry))
      (.toString writer))))

(defn format-metrics [collector-id metrics]
  (metrics->prometheus metrics))

(defn format-all-metrics [metrics]
  (let [metrics (mapcat (comp :latest second)
                        metrics)]
    (metrics->prometheus metrics)))
