(ns baymax.source.postgres
  (:require [hikari-cp.core :as hikari]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [camel-snake-kebab.core :as csk]
            [clojure.tools.logging :as log]
            [clojure.string :as s]
            [baymax.proto :refer [Source]]))

(def default-pool-config
  {:minimum-idle 2
   :maximum-pool-size 10
   :auto-commit true
   :connection-timeout 30000
   :idle-timeout 600000
   :max-lifetime 1800000
   :validation-timeout 5000
   :pool-name "baymax-pool"})

(defn execute-query
  [{:keys [datasource]} query]
  (try
    (->> (jdbc/execute! datasource [query]
                        {:builder-fn rs/as-unqualified-modified-maps
                         :label-fn csk/->kebab-case-keyword}))
    (catch Exception e
      (log/error e "failed to execute postgres query:" query)
      (throw (ex-info "query execution failed"
                      {:query query
                       :error (.getMessage e)})))))

(defn collect
  "collect metrics from a postgres source based on query"
  [source {:keys [query transform]}]
  (let [collected (execute-query source query)]
    (transform collected)))

(defn check-health
  "check if the postgres connection is healthy"
  [{:keys [datasource]}]
  (try
    (when (jdbc/execute-one! datasource ["SELECT 1 as health"])
      {:healthy true
       :status "connected"
       :message "database connection is 6 feet above"})
    (catch Exception e
      {:healthy false
       :status "error"
       :message (.getMessage e)})))

(defn close [{:keys [datasource]}]
  (when datasource
    (hikari/close-datasource datasource)))

(defn make-datasource [{:keys [connection pool]}]
  (let [{:keys [host port database user password]} connection
        hikari-config (merge default-pool-config
                             (select-keys pool [:minimum-idle
                                                :maximum-pool-size
                                                :idle-timeout])
                             {:adapter "postgresql"
                              :server-name host
                              :port-number port
                              :database-name database
                              :username user
                              :password password})]
    (hikari/make-datasource hikari-config)))


(defrecord PostgresSource [config datasource]
  Source
  (collect [this {:keys [query transform]}]
    (let [collected (execute-query this query)]
      (transform collected)))

  (health-check [this]
    (check-health this))

  (disconnect [this]
    (when datasource
      (hikari/close-datasource datasource)
      (log/info "disconnected from postgres:"
                (get-in config [:connection :host])))))

(defn make-source [config]
  (let [datasource (make-datasource config)
        config (update-in config [:connection]
                          dissoc :password)]
    (->PostgresSource config datasource)))
