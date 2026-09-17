(ns baymax.publisher.stdout
  (:require [baymax.publisher.proto :refer [Publisher]]
            [baymax.config :as env]
            [clojure.string :as s]
            [clojure.pprint :as pp]
            [jsonista.core :as json]
            [clojure.tools.logging :as log]))

(def mapper (json/object-mapper))

(defn print-pretty
  "a banner and a pretty printed intel: made for human eyes"
  [collector-id intel at]
  (locking *out*
    (println "\n================= intel ==================== |" collector-id "|" at)
    (clojure.pprint/pprint intel)
    (println "============================================")))

(defn print-json
  "one json object per metric, logged as one event each: made for log shippers"
  [collector-id intel at]
  (doseq [metric intel]
    (log/info "publishing intel:"
              (json/write-value-as-string {:collector collector-id
                                           :at at
                                           :intel metric}
                                          mapper))))

(defn print-intel-atomically [format collector-id intel at]
  (case format
    :json (print-json collector-id intel at)
    (print-pretty collector-id intel at)))

(defrecord StdoutPublisher [config]
  Publisher
  (publish [this collector-id intel]
    (let [timestamp (java.time.LocalDateTime/now)
          formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
          formatted-time (.format timestamp formatter)]

      (print-intel-atomically (:format config)
                              collector-id
                              intel
                              formatted-time)

      {:published (count intel)
       :timestamp formatted-time}))

  (health-check [this]
    {:healthy true
     :status "available"
     :message "stdout is 6 feet above"})

  (disconnect [this]
    {:status "disconnected"}))

(defn make-publisher
  "a publisher level :format wins over the one from {:logging {:metrics {:format ...}}}
   which arrives here as :metrics-format. with neither around it is :pretty"
  [config]
  (let [format (or (env/parse-metrics-format (:format config))
                   (:metrics-format config)
                   :pretty)
        conf (merge {:pretty-print true
                     :color false}
                    config
                    {:format format})]
    (log/info "making stdout publisher, logging metrics in" format "format")
    (->StdoutPublisher conf)))
