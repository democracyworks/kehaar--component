(ns kehaar-component.external-service
  (:require
    [clojure.core.async :as async]
    [com.stuartsierra.component :as component]
    [kehaar.wire-up :as wire-up]
    [langohr.core :as rmq]))

;; Pseudo spec/schema just so it's clear what we expect for config
;; here--could later be used to validate config with some adjustments.

(def config-spec
  {:type :external

   ;; Kehaar/RabbitMQ config
   :exclusive false
   :durable true
   :auto-delete false})

(defprotocol IExternalService
  (call-external! [this msg]))

(defrecord ExternalService [rabbitmq config msg-chan service call-external]
  component/Lifecycle

  (start [component]
    (println ";; Starting ExternalService " (first config))

    (let [[queue-name service-config] config
          msg-chan (async/chan (or (:timeout service-config) 1000))
          service (-> (:connection rabbitmq)
                      (wire-up/external-service
                       "" ; why is this not the default in kehaar.wire-up?
                       queue-name service-config 10000 msg-chan))
          call-external (wire-up/async->fn msg-chan)]

      (assoc component :service service :msg-chan msg-chan :call-external call-external)))

  (stop [component]
    (println ";; Stopping ExternalService " (first config))

    (when-not (rmq/closed? service) (rmq/close service))
    (async/close! msg-chan)

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :service nil :msg-chan nil :call-external call-external))

  IExternalService
  (call-external! [component msg]
    ((:call-external component) msg)))

(defn new-external-service [config]
  (map->ExternalService config))