(ns kehaar-component.kehaar-rabbitmq
  (:require
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]
    [kehaar.rabbitmq]
    [langohr.core :as rmq]))

;; Pseudo spec/schema just so it's clear what we expect for config
;; here--could later be used to validate config with some adjustments.

(def config-spec
  ;; RMQ-specific config
  {:connection {:host "localhost"
                :port 5672}

   :max-retries 5})

(defrecord KehaarRabbitMQ [config max-retries connection]
  component/Lifecycle

  (start [component]
    (log/info ";; Starting RabbitMQ")

    (assoc component
           :connection
           (kehaar.rabbitmq/connect-with-retries config max-retries)))

  (stop [component]
    (log/info ";; Stopping RabbitMQ")

    (when-not (rmq/closed? connection) (rmq/close connection))

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :connection nil)))

(defn new-kehaar-rabbitmq [config max-retries]
  (map->KehaarRabbitMQ {:config config :max-retries max-retries}))
