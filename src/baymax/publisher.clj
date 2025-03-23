(ns baymax.publisher
  (:require [baymax.publisher.prometheus :as prometheus]
            [baymax.publisher.stdout :as stdout]
            ; [baymax.publisher.influxdb :as influxdb]
            ; [baymax.publisher.elastic :as elastic]
            ))

(defn make-publisher [config]
  (case (:type config)
    :prometheus (prometheus/make-publisher config)
    :stdout     (stdout/make-publisher config)
    ; :influxdb (influxdb/make-publisher config)
    ; :elastic  (elastic/make-publisher config)
    (throw (ex-info "unknown publisher type" {:config config
                                              :supported-types [:prometheus :stdout]}))))
