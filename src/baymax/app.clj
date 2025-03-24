(ns baymax.app
  (:require [clojure.tools.logging :as log]
            [nrepl.server :as nrepl]
            [yang.exception :as ye]
            [mount.core :as mount :refer [defstate]]
            [mount-up.core :as mu]
            [baymax.config :as env]
            [baymax.scheduler :as sch]
            [baymax.chip :as cp]
            [baymax.server :as web])
  (:gen-class))

(defstate config :start (env/load-config)
                 :stop  :stopped)

(defn- start-nrepl [{:keys [host port]
                     :or {host "0.0.0.0"
                          port 2142}}]
  (nrepl/start-server :bind host
                      :port port))

(defstate nrepl :start (start-nrepl (config :nrepl))
                :stop (nrepl/stop-server nrepl))

(defstate chip :start (cp/flash config)
               :stop  :unplugged)

(defstate server :start (web/start-server config)
                 :stop  (web/stop-server server))

(defstate scheduler :start (sch/start chip)
                    :stop  (sch/stop scheduler))

(defn -main [& args]
  (ye/set-default-exception-handler)
  (mu/on-upndown :info mu/log :before)

  (log/info "baymax: initializing..")
  (.addShutdownHook (Runtime/getRuntime)
                    (Thread. (fn []
                               (mount/stop))))
  ;; starting nrepl
  ;; to make sure app is accessible remotely in case some states won't be able to start
  (mount/start #'baymax.app/config
               #'baymax.app/nrepl)
  (log/info "baymax: flashing the chip.. [loading]")
  (mount/start #'baymax.app/chip)
  (log/info "baymax: flashing the chip.. [done]")

  (mount/start)
  (log/info "baymax: your visibility and insights loyal companion is ready to rock!"))
