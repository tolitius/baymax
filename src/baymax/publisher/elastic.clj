(ns baymax.publisher.elastic
  (:require [baymax.publisher.proto :refer [Publisher]]
            [clojure.tools.logging :as log]
            [clj-http.client :as http]
            [jsonista.core :as json]))

(def mapper (json/object-mapper))

(defn serialize [data]
  (json/write-value-as-string data mapper))

(defn add-timestamp [intel]
  (let [now (java.time.Instant/now)]
    (map #(assoc % :collected_at (.toString now)) intel)))

(defn format-bulk-action [index doc]
  (let [doc-id (or (:id doc)
                   (str (java.util.UUID/randomUUID)))]
    [(serialize {:index {:_index index :_id doc-id}})
     (serialize doc)]))

(defn make-bulk [index intel]
  (let [timestamped-intel (add-timestamp intel)
        bulk-actions (mapcat #(format-bulk-action index %)
                             timestamped-intel)]
    (str (clojure.string/join "\n" bulk-actions) "\n")))

(defn bulk-index [url auth-headers index intel]
  (let [bulk (make-bulk index intel)
        endpoint (str url "/_bulk")
        response (http/post endpoint
                           {:body bulk
                            :headers (merge {"Content-Type" "application/json"}
                                            auth-headers)
                            ; :debug true
                            ; :debug-body true
                            })]
    (when-not (<= 200 (:status response) 299)
      (throw (ex-info "bulk indexing failed"
                      {:status (:status response)
                       :body (:body response)})))
    (json/read-value (:body response) mapper)))

(defrecord ElasticsearchPublisher [config]
  Publisher
  (publish [this intel]
    (let [{:keys [url auth-type username password api-key token index]} config
          auth-headers (case auth-type
                         :basic {"Authorization" (str "Basic "
                                                      (javax.xml.bind.DatatypeConverter/printBase64Binary
                                                        (.getBytes (str username ":" password))))}
                         :api-key {"Authorization" (str "ApiKey " api-key)}
                         :token {"Authorization" (str "Bearer " token)}
                         {})]
      (try
        (let [result (bulk-index url auth-headers index intel)]
          {:published (count intel)
           :errors (get result "errors")
           :took (get result "took")
           :timestamp (java.time.LocalDateTime/now)})
        (catch Exception e
          (log/error e "failed to publish to elasticsearch:" (-> e ex-data :body))
          {:published 0
           :error (.getMessage e)}))))

  (health-check [this]
    (let [{:keys [url auth-type username password api-key token]} config
          auth-headers (case auth-type
                         :basic {"Authorization" (str "Basic "
                                                     (javax.xml.bind.DatatypeConverter/printBase64Binary
                                                       (.getBytes (str username ":" password))))}
                         :api-key {"Authorization" (str "ApiKey " api-key)}
                         :token {"Authorization" (str "Bearer " token)}
                         {})]
      (try
        (let [response (http/get (str url "/_cluster/health")
                                 {:headers auth-headers})]
          (if (<= 200 (:status response) 299)
            (let [body (json/read-value (:body response) mapper)]
              {:healthy true
               :status (get body "status")
               :cluster_name (get body "cluster_name")
               :message "elasticsearch cluster is 6 feet above"})
            {:healthy false
             :status "error"
             :message (str "elasticsearch returned status: " (:status response))}))
        (catch Exception e
          {:healthy false
           :status "error"
           :message (.getMessage e)}))))

  (disconnect [this]
    {:status "disconnected"}))

(defn make-publisher [config]
  (log/info "making elasticsearch publisher")
  (->ElasticsearchPublisher config))
