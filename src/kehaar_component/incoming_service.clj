(ns kehaar-component.incoming-service
  (:require
    [clojure.core.async :as async]
    [com.stuartsierra.component :as component]
    [kehaar.wire-up :as wire-up]
    [kehaar-component.shared :as shared]
    [langohr.core :as rmq]))

;; Pseudo spec/schema just so it's clear what we expect for config
;; here--could later be used to validate config with some adjustments.

(def config-spec
  {:type :incoming

   ;; For the function handling incoming messages.
   :handler-fn #(fn? %) ; function arg.

   ;; Kehaar/RabbitMQ config
   :exclusive false
   :durable true
   :auto-delete false})

(defrecord IncomingService [rabbitmq config in-chan out-chan service]
  component/Lifecycle

  (start [component]
    (println ";; Starting IncomingService " (first config))

    (let [[queue-name service-config] config
          in-chan (async/chan), out-chan (async/chan)
          service (-> (:connection rabbitmq)
                      (wire-up/incoming-service queue-name
                                                service-config
                                                in-chan
                                                out-chan))
          handler-fn' (or (:handler-fn service-config) shared/handler-no-op)]

      (wire-up/start-responder! in-chan
                                out-chan
                                (shared/handle-errors handler-fn'))

      (assoc component :service service :in-chan in-chan :out-chan out-chan)))

  (stop [component]
    (println ";; Stopping IncomingService " (first config))

    (when-not (rmq/closed? service) (rmq/close service))
    (async/close! in-chan)
    (async/close! out-chan)

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :service nil :in-chan nil :out-chan nil)))

(defn new-incoming-service [config]
  (map->IncomingService config))
