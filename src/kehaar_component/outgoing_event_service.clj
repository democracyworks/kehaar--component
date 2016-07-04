(ns kehaar-component.outgoing-event-service
  (:require
    [clojure.core.async :as async]
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]
    [kehaar.wire-up :as wire-up]
    [kehaar-component.shared :as shared]
    [langohr.core :as rmq]))

;; Pseudo spec/schema just so it's clear what we expect for config
;; here--could later be used to validate config with some adjustments.

(def config-spec
  {
   :type :outgoing-event

   ;; RMQ specific config here, unique name, basically topic within events channel
   :routing-key "String"

   ;; optional
   :outgoing-events-chan-buffer 1000 ; or something?
   })

(defrecord OutgoingEventService [rabbitmq config outgoing-events-chan service]
  component/Lifecycle

  (start [component]
    (println ";; Starting OutgoingEventService " (first config))

    (let [[queue-name service-config] config
          outgoing-events-chan (async/chan
                                (or (:outgoing-events-chan-buffer service-config)
                                    1000))

          service (-> (:connection rabbitmq)
                      (wire-up/outgoing-events-channel
                       "events"
                       (:routing-key service-config)
                       outgoing-events-chan))]

      (assoc component :service service :outgoing-events-chan outgoing-events-chan)))

  (stop [component]
    (println ";; Stopping OutgoingEventService " (first config))

    (when-not (rmq/closed? service) (rmq/close service))
    (async/close! outgoing-events-chan)

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :service nil :outgoing-events-chan nil)))

(defn new-outgoing-event-service [config]
  (map->OutgoingEventService config))
