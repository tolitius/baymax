(ns baymax.publisher.stdout
  (:require [baymax.publisher.proto :refer [Publisher]]
            [clojure.string :as s]
            [clojure.pprint :as pp]
            [clojure.tools.logging :as log]))

(defrecord StdoutPublisher [config]
  Publisher
  (publish [this intel]
    (let [timestamp (java.time.LocalDateTime/now)
          formatter (java.time.format.DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
          formatted-time (.format timestamp formatter)]

      (println "\n================= intel ==================== |" formatted-time)

      (pp/pprint intel)

      (println "============================================")

      {:published (count intel)
       :timestamp formatted-time}))

  (health-check [this]
    {:healthy true
     :status "available"
     :message "stdout is 6 feet above"})

  (disconnect [this]
    {:status "disconnected"}))

(defn make-publisher [config]
  (let [conf (merge {:pretty-print true
                     :color false}
                    config)]
    (log/info "making stdout publisher")
    (->StdoutPublisher conf)))
