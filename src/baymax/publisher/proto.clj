(ns baymax.publisher.proto)

(defprotocol Publisher
  (publish [this intel] "publish intel to the destination")
  (health-check [this] "check the health of the destination")
  (disconnect [this] "clean up resources and disconnect from the destination"))


