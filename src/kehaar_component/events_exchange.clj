(ns kehaar-component.events-exchange
  (:require
    [com.stuartsierra.component :as component]
    [kehaar.wire-up :as wire-up]
    [langohr.core :as rmq]))

;; I have skipped config for this at this time, as everywhere we are
;; using this it uses the same config in the resources/config.edn:

;; :topics {"events" {:durable true :auto-delete false}}}

;; Easy to add more configuration at some later point if need be.

(defrecord EventsExchange [rabbitmq exchange]
  component/Lifecycle

  (start [component]
    (println ";; Starting EventsExchange")

    (assoc component
           :exchange
           (-> (:connection rabbitmq)
               (wire-up/declare-events-exchange
                "events" "topic" {:durable true :auto-delete false}))))

  (stop [component]
    (println ";; Stopping EventsExchange")
    (when-not (rmq/closed? exchange) (rmq/close exchange))

    ;; Remember that if you dissoc one of a record's base fields, you
    ;; get a plain map.
    (assoc component :exchange nil)))

(defn new-events-exchange [config]
  (map->EventsExchange config))
