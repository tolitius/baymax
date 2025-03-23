(ns baymax.source.proto)

(defprotocol Source
  (collect [this collector] "collect intel for a given collector")
  (health-check [this] "check the health of this source")
  (disconnect [this] "clean up resources and disconnect from the source"))

