(ns baymax.proto)

(defprotocol Source
  (collect [this collector] "collect intel for a given collector")
  (health-check [this] "check the health of this source")
  (disconnect [this] "clean up resources and disconnect from the source"))

(defprotocol Publisher
  (publish [this intel] "publish intel to the destination")
  (health-check [this] "check the health of the destination")
  (disconnect [this] "clean up resources and disconnect from the destination"))


