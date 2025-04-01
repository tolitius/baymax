(ns baymax.scheduler
  (:require [yang.scheduler :as ys]
            [clojure.tools.logging :as log]
            [mount.core :refer [defstate]]
            [baymax.source.proto :as source]
            [baymax.publisher.proto :as publisher]
            [baymax.chip :as cp]
            [baymax.registry :as registry]
            [baymax.config :as config])
  (:import [java.util.concurrent TimeUnit]
           [java.time Instant ZonedDateTime ZoneId]
           [java.time.format DateTimeFormatter]))

(defn format-instant [instant]
  (let [formatter (DateTimeFormatter/ofPattern "yyyy-MM-dd HH:mm:ss")
        zdt (ZonedDateTime/ofInstant instant (ZoneId/systemDefault))]
    (.format formatter zdt)))

(defn create-collector-task [chip collector-id]
  (let [collector (cp/find-collector chip
                                     collector-id)
        source-id (:source collector)
        source (get-in chip [:sources source-id])
        publishers (cp/find-publishers chip
                                       collector-id)]

    (fn collect-task []
      (try
        (log/info "collecting intel for" collector-id "from source" source-id)
        (let [start-time (System/currentTimeMillis)
              intel (source/collect source collector)
              duration (- (System/currentTimeMillis) start-time)]

          (log/info "collected intel for" collector-id
                    "in" duration "ms")

          ;; record intel to registry
          (registry/record-intel! collector-id intel)

          ;; publish intel to all associated publishers
          (doseq [pub publishers]
            (try
              (log/debug "publishing intel from" collector-id "to" (-> pub :config :type))
              (publisher/publish pub intel)
              (catch Exception e
                (log/error "could not publish intel for" collector-id "to" (-> pub :config :type) "due to" (.getMessage e))))))

        (catch Exception e
          (log/error e "could not collect intel for" collector-id "from source" source-id))))))

(defn schedule-collector [chip collector-id]
  (let [collector (cp/find-collector chip
                                     collector-id)
        interval-ms (:interval-ms collector)
        collector-task (create-collector-task chip
                                              collector-id)]

    (log/info "scheduling collector" collector-id "with interval" interval-ms "ms")

    (ys/every interval-ms collector-task
              {:task-name (str "collector-" collector-id)
               :time-unit TimeUnit/MILLISECONDS})))

(defn start
  "start all scheduled collectors based on intel burned into a chip
   return a map of collector ids to their scheduled tasks"
  [chip]
  (let [collector-ids (map :id (:collectors chip))
        schedules (reduce (fn [acc collector-id]
                            (assoc acc collector-id
                                   (schedule-collector chip
                                                       collector-id)))
                          {}
                          collector-ids)]

    (log/info "scheduled " (count schedules) "collectors")

    {:schedules schedules         ;; collector id => scheduler
     :chip chip                   ;; the chip that was used to schedule
     :started-at (Instant/now)}))

(defn stop
  "stop all scheduled collector tasks"
  [{:keys [schedules]}]
  (doseq [[collector-id schedule] schedules]
    (log/info "cancel schedule for a collector" collector-id)
    ((:cancel schedule)))
  (log/info "all collector schedules are cancelled"))

(defn find-schedules [scheduler]
  (->> (for [{:keys [id] :as schedule} (-> scheduler status :schedules)]
         [id (dissoc schedule :id)])
       (into {})))

(defn status
  "return the current status of all schedules"
  [{:keys [schedules started-at]}]
  (let [now (Instant/now)
        uptime (- (.toEpochMilli now) (.toEpochMilli started-at))
        status (mapv (fn [[collector-id schedule]]                ;; later if collector ids are unique, this should be a map
                       (let [intel (:intel schedule)]
                         {:id collector-id
                          :running? ((:running? intel))
                          :cancelled? ((:cancelled? intel))
                          :done? ((:done? intel))
                          :next-run (some-> ((:next-run intel))
                                            format-instant)
                          :interval (:interval intel)}))
                     schedules)]

    {:status "running"
     :started-at (format-instant started-at)
     :uptime-ms uptime
     :uptime (format "%d days %d hours %d minutes %d seconds"
                     (quot uptime (* 24 60 60 1000))
                     (quot (mod uptime (* 24 60 60 1000)) (* 60 60 1000))
                     (quot (mod uptime (* 60 60 1000)) (* 60 1000))
                     (quot (mod uptime (* 60 1000)) 1000))
     :schedules status}))

(defn health
  "check health of all scheduled collectors"
  [{:keys [schedules chip]}]
  (let [sources (:sources chip)
        source-health (reduce-kv (fn [acc id source]
                                   (assoc acc id (source/health-check source)))
                                 {}
                                 sources)
        collector-health (reduce (fn [acc [collector-id schedule]]
                                   (let [intel (:intel schedule)
                                         collector (get-in chip [:collectors collector-id])
                                         source-id (:source collector)
                                         source-status (get-in source-health [source-id :healthy])]
                                     (assoc acc collector-id
                                            {:id collector-id
                                             :scheduled? true
                                             :running? ((:running? intel))
                                             :source-id source-id
                                             :source-healthy? source-status})))
                                 {}
                                 schedules)]

    {:sources source-health
     :collectors collector-health
     :healthy? (and (every? (fn [[_ status]] (:healthy status)) source-health)
                   (every? (fn [[_ status]] (:running? status)) collector-health))}))

(defstate scheduler :start (start cp/chip)
                    :stop  (stop scheduler))

;; ----------
(defn collect
  "manually trigger collection for a specific collector"
  [chip collector-id]
  (let [task (create-collector-task chip
                                    collector-id)]
    (log/info "manually triggering collection for" collector-id)
    (task)
    {:status "completed"
     :collector-id collector-id
     :timestamp (format-instant (Instant/now))}))
