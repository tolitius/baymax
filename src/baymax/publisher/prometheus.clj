(ns baymax.publisher.prometheus
  (:require [baymax.publisher.proto :refer [Publisher]]
            [clojure.string :as s]
            [clojure.tools.logging :as log])
  (:import [io.prometheus.client Gauge GaugeMetricFamily CollectorRegistry SimpleCollector]
           [io.prometheus.client.exporter.common TextFormat]))

(defn make-publisher [config]
  :to-implement)

(defn name->prometheus [name]
  (-> (str name)
      (s/replace #"[^a-zA-Z0-9_:]" "_")))

(defn metrics->prometheus
  "Format metrics for Prometheus using the client library (on-demand)"
  [metrics]
  (let [registry (CollectorRegistry.)
        metric-families (atom {})]

    ;; group metrics by name
    (doseq [{:keys [name value labels]} metrics]
      (let [metric-name (name->prometheus name)
            help-text (str "Metric: " metric-name)
            label-keys (keys labels)

            ;; make metric family
            family (if-let [existing (get @metric-families metric-name)]
                     existing
                     (let [new-family (GaugeMetricFamily.
                                      metric-name
                                      help-text
                                      (java.util.ArrayList. (map name label-keys)))]
                       (swap! metric-families assoc metric-name new-family)
                       new-family))]

        ;; add sample to the metric family
        (.addMetricValue family
                        (java.util.ArrayList. (map str (vals labels)))
                        (double value))))

    ;; register it all
    (doseq [[_ family] @metric-families]
      (.register family registry))

    ;; export registry to string
    (let [writer (java.io.StringWriter.)]
      (TextFormat/write004 writer registry)
      (.toString writer))))

(defn format-metrics [collector-id metrics]
  (metrics->prometheus metrics))

(defn format-all-metrics [metrics]
  (let [metrics (mapcat
                  (fn [[_ intel]] (:latest intel))
                  metrics)]
    (metrics->prometheus metrics)))
