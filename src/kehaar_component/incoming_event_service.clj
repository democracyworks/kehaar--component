(ns kehaar-component.incoming-event-service
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
  {:type :incoming-event

   ;; RMQ specific config here, unique name, basically topic within events channel
   :routing-key "String" 

   ;; For the function handling incoming messages.
   :handler-fn #(fn? %) ; function arg.

   ;; Kehaar/RabbitMQ config
   :exclusive false
   :durable true
   :auto-delete false})

(defrecord IncomingEventService [rabbitmq config incoming-events-chan service]
  component/Lifecycle

  (start [component]
    (println ";; Starting IncomingEventService " (first config))

    (let [[queue-name service-config] config
          incoming-events-chan (async/chan 1000) ; make buffer-size configurable?

          service (-> (:connection rabbitmq)
                      (wire-up/incoming-events-channel
                       queue-name
                       service-config
                       "events" ; topic exchange name
                       (:routing-key service-config)
                       incoming-events-chan
                       (or (:timeout service-config) 2000)))
          
          handler-fn' (or (:handler-fn service-config) shared/handler-no-op)]

      (wire-up/start-event-handler! incoming-events-chan
                                    (shared/handle-errors handler-fn'))

      (assoc component :service service :incoming-events-chan incoming-events-chan)))

  (stop [component]
    (println ";; Stopping IncomingEventService " (first config))

    (when-not (rmq/closed? service) (rmq/close service))
    (async/close! incoming-events-chan)

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :service nil :incoming-events-chan nil)))

(defn new-incoming-event-service [config]
  (map->IncomingEventService config))
