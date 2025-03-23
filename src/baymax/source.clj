(ns baymax.source
  (:require [clojure.tools.logging :as log]
            [baymax.source.postgres :as pg]
            ; [baymax.source.http :as http]
            ; [baymax.source.jmx :as jmx]
            ))

(defn make-source [config]
  (case (:type config)
    :postgres (pg/make-source config)
    ; :http (http/make-source config)
    ; :jmx (jmx/make-source config)
    (throw (ex-info "unknown source type" {:config config
                                           :supported-types [:postgres]}))))
