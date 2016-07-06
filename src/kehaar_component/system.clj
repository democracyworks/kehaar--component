(ns kehaar-component.system
  (:require
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]
    [kehaar-component.events-exchange :as ee]
    [kehaar-component.external-service :as es]
    [kehaar-component.incoming-event-service :as ies]
    [kehaar-component.incoming-service :as is]
    [kehaar-component.kehaar-rabbitmq :as krmq]
    [kehaar-component.outgoing-event-service :as oes]
    [kehaar-component.shared :as shared]))

(defn- inject-handler!
  [config]
  (try
    (update config :handler-fn
            (fnil #(-> % symbol find-var)
                  "kehaar-component.incoming-service/handler-no-op"))
    (catch Exception e
      (log/warn "Got a bad handler in the config: " (:handler-fn config))
      (assoc config :handler-fn shared/handler-no-op))))

(defmulti gen-component-map :type)

(defmethod gen-component-map :incoming
  [config]
  (component/using
   (is/new-incoming-service {:config (inject-handler! config)})
   [:rabbitmq]))

(defmethod gen-component-map :external
  [config]
  (component/using
   (es/new-external-service {:config config})
   [:rabbitmq]))

(defmethod gen-component-map :incoming-event
  [config]
  (component/using
   (ies/new-incoming-event-service {:config (inject-handler! config)})
   [:rabbitmq :events-exchange]))

(defmethod gen-component-map :outgoing-event
  [config]
  (component/using
   (oes/new-outgoing-event-service {:config config})
   [:rabbitmq :events-exchange]))

(defn has-event-exchange?
  [system-map]
  (->> (take-nth 2 (rest system-map))
       (filter #(#{:incoming-event :outgoing-event} (get-in % [:config :type])))
       seq
       boolean))

(defn maybe-add-events-exchange
  [system-map]
  (if (has-event-exchange? system-map)
    (->> (component/using (ee/new-events-exchange {}) [:rabbitmq])
         (conj system-map :events-exchange))
    system-map))

(defn maybe-inject-kehaar-component-map
  [components [queue-name queue-config]]
  (if-let [component (->> (assoc queue-config :queue-name queue-name)
                          gen-component-map)]
    (conj components (keyword queue-name) component)
    components))

(defn- gen-system-map
  [{:keys [connection max-retries queues topics]}]
  (let [rmq-base [:rabbitmq (krmq/new-kehaar-rabbitmq connection max-retries)]]
    (->> queues
         (reduce maybe-inject-kehaar-component-map rmq-base)
         (maybe-add-events-exchange))))

(defn system [config-options]
  (let [system-map (gen-system-map config-options)]
    (log/debug "System-map: " (pr-str system-map))
    (apply component/system-map system-map)))
