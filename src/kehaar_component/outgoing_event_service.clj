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
   :queue-name "outgoing-event-service-queue-name"

   ;; RMQ specific config here, unique name, basically topic within events channel
   :routing-key "String"

   ;; optional
   :outgoing-events-chan-buffer 1000 ; or something?
   })

(defrecord OutgoingEventService [rabbitmq config outgoing-events-chan service]
  component/Lifecycle

  (start [component]
    (log/info ";; Starting OutgoingEventService " (:queue-name config))

    (let [{:keys [queue-name outgoing-events-chan-buffer routing-key]} config
          outgoing-events-chan (async/chan (or outgoing-events-chan-buffer 1000))

          service (-> (:connection rabbitmq)
                      (wire-up/outgoing-events-channel
                       "events"
                       routing-key
                       outgoing-events-chan))]

      (assoc component :service service :outgoing-events-chan outgoing-events-chan)))

  (stop [component]
    (log/info ";; Stopping OutgoingEventService " (:queue-name config))

    (when-not (rmq/closed? service) (rmq/close service))
    (async/close! outgoing-events-chan)

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :service nil :outgoing-events-chan nil)))

(defn new-outgoing-event-service [config]
  (map->OutgoingEventService config))
