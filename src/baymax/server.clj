(ns baymax.server
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.resource :as resource]
            [ring.middleware.content-type :as content-type]
            [clojure.java.io :as io]
            [clojure.string :as s]
            [reitit.ring :as ring]
            [reitit.core :as r]
            [reitit.coercion.spec]
            [reitit.ring.coercion :as rrc]
            [reitit.ring.middleware.parameters :as parameters]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [muuntaja.core :as m]
            [mount.core :refer [defstate]]
            [baymax.config :as env]
            [baymax.chip :as chip]
            [baymax.registry :as registry]
            [baymax.scheduler :as sch]
            [baymax.publisher.prometheus :as prometheus]
            [baymax.source.proto :as source]
            [baymax.publisher.proto :as publisher]
            [clojure.tools.logging :as log]))

(defn collector-intel-handler [request]
  (let [collector-id (get-in request [:path-params :collector-id])
        format (get-in request [:path-params :format] "json")
        intel (registry/find-latest-intel collector-id)]
    (if intel
      (case format
        "json" {:status 200
                :body (:latest intel)}
        "prometheus" {:status 200
                      :headers {"Content-Type" "text/plain; version=0.0.4"}
                      :body (prometheus/format-metrics collector-id
                                                       (:latest intel))}
        {:status 400
         :body {:error (str "unsupported format: " format)}})
      {:status 404
       :body {:error (str "no intel found for collector: " collector-id)}})))

(defn all-intel-handler [request]
  (let [format (get-in request [:path-params :format] "json")
        all-intel (registry/find-all-intel)]
    (if (seq all-intel)
      (case format
        "json" {:status 200
                :body all-intel}
        "prometheus" {:status 200
                      :headers {"Content-Type" "text/plain; version=0.0.4"}
                      :body (prometheus/format-all-metrics all-intel)}
        {:status 400
         :body {:error (str "unsupported format: " format)}})
      {:status 200
       :body {}})))

;; scheduler handlers
(defn schedule-health-handler [_]
  {:status 200
   :body (sch/health sch/scheduler)})

(defn schedule-status-handler [_]
  {:status 200
   :body (sch/status sch/scheduler)})

(defn collector-schedule-status-handler [request]
  (let [collector-id (get-in request [:path-params :collector-id])
        all-status (sch/status sch/scheduler)
        collector-status (->> (:schedules all-status)
                              (filter #(= collector-id (:id %)))
                              first)]
    (if collector-status
      {:status 200
       :body collector-status}
      {:status 404
       :body {:error (str "no schedule found for collector: " collector-id)
              :collector-id collector-id}})))

(defn health-handler [_]
  {:status 200
   :body {:status "6 feet above"
          :app "baymax"
          :timestamp (java.time.Instant/now)}})


(defn dashboard-handler [_]
  (let [collectors (chip/chip :collectors)
        sources    (chip/chip :sources)
        publishers (chip/chip :publishers)
        schedules (sch/find-schedules sch/scheduler)
        uptime    (-> sch/scheduler sch/status :uptime)]
    {:status 200
     :body {:uptime uptime
            :collectors {:count (count collectors)
                         :items (map (fn [{:keys [id schedule source]}]
                                       {:id id
                                        :running? (-> (get schedules id)
                                                      :running?)
                                        :source source
                                        :schedule schedule})
                                     collectors)}
            :sources {:count (count sources)
                      :items (map (fn [[id source]]
                                    {:id id
                                     :type (-> source :config :type)
                                     :health (source/health-check source)})
                                  sources)}
            :publishers {:count (count publishers)
                         :items (map (fn [[id pub]]
                                       {:id id
                                        :type (-> pub :config :type)
                                        :health (publisher/health-check pub)})
                                     publishers)}}}))

(defn home-handler [request]
  (let [root-uri (or (some-> (get-in request [:config :root-uri])
                             (s/replace #"/$" ""))
                     "")]
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body (-> (io/resource "public/index.html")
               slurp
               (s/replace #"ROOT_URI" root-uri))}))

(def app-routes
  [["/" {:get home-handler}]
   ["/health" {:get health-handler}]
   ["/dashboard" {:get dashboard-handler}]
   ["/intel-all/:format" {:get all-intel-handler}]
   ["/intel-all" {:get all-intel-handler}]
   ["/intel/:collector-id/:format" {:get collector-intel-handler}]
   ["/intel/:collector-id" {:get collector-intel-handler}]
   ["/schedule/health" {:get schedule-health-handler}]
   ["/schedule/status" {:get schedule-status-handler}]
   ["/schedule/status/:collector-id" {:get collector-schedule-status-handler}]])

(def default-handler
  (let [encode-json (fn [status data]
                      {:status status
                       :headers {"Content-Type" "application/json"}
                       :body (m/encode m/instance "application/json" data)})]
    (ring/create-default-handler
      {:not-found          (fn [_] (encode-json 404 {:error "route not found"}))
       :method-not-allowed (fn [_] (encode-json 405 {:error "method not allowed"}))
       :not-acceptable     (fn [_] (encode-json 406 {:error "not acceptable"}))})))

(defn wrap-config [handler config]
  (fn [request]
    (handler (assoc request :config config))))

(defn app [config]
  (-> (ring/ring-handler
        (ring/router
          app-routes
          {:data {:coercion reitit.coercion.spec/coercion
                  :muuntaja m/instance
                  :middleware [parameters/parameters-middleware
                               muuntaja/format-middleware
                               rrc/coerce-exceptions-middleware
                               rrc/coerce-request-middleware
                               rrc/coerce-response-middleware]}})
        (ring/routes
          (ring/redirect-trailing-slash-handler)
          default-handler))
      (wrap-config config)
      (resource/wrap-resource "public")
      (content-type/wrap-content-type)))

(defn start-server [config]
  (let [port (get-in config [:web :port] 4242)
        root-uri (get-in config [:web :root-uri] "/")]
    (log/infof "starting baymax server on port %s, routable on root uri \"%s\"" port root-uri)
    (jetty/run-jetty (app (:web config)) {:port port
                                          :join? false})))

(defn stop-server [server]
  (log/info "stopping baymax server")
  (.stop server)
  {:status "stopped"})

(defstate server :start (start-server env/config)
                 :stop  (stop-server server))
