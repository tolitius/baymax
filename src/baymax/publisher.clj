(ns baymax.publisher
  (:require [baymax.publisher.prometheus :as prometheus]
            [baymax.publisher.stdout :as stdout]
            [baymax.publisher.elastic :as elastic]
            ; [baymax.publisher.influxdb :as influxdb]
            ))

(defn make-publisher [config]
  (case (:type config)
    :prometheus                   (prometheus/make-publisher config)
    (:elastic :elasticsearch)     (elastic/make-publisher config)
    :stdout                       (stdout/make-publisher config)
    ;; :influxdb                     (influxdb/make-publisher config)
    (throw (ex-info "unknown publisher type" {:config config
                                              :supported-types [:prometheus :elastic :stdout]}))))
