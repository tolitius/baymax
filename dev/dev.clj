(ns dev
  (:require [clj-http.client :as http]
            [jsonista.core :as json]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [clojure.repl :refer :all]
            [clojure.pprint :refer [pprint]]
            [yang.lang :as y]
            [mount.core :as mount]
            [baymax.config :as c]
            [baymax.chip :as chip]
            [baymax.app :as app]
            [baymax.registry :as registry]
            [baymax.scheduler :as scheduler]
            [baymax.source.postgres :as pg]))

(defn restart []
  (mount/stop)
  (mount/start))
