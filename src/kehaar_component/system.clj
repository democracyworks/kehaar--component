(ns kehaar-component.system
  (:require
    [clojure.tools.logging :as log]
    [com.stuartsierra.component :as component]
    [kehaar-component.external-service :as es]
    [kehaar-component.incoming-service :as is]
    [kehaar-component.shared :as shared]
    [kehaar-component.kehaar-rabbitmq :as krmq]))

(defn- inject-handler!
  [config]
  (try
    (update-in
     config
     [1 :handler-fn]
     (fnil #(-> % symbol find-var)
           "kehaar-component.incoming-service/handler-no-op"))
    (catch Exception e
      (log/warn "Got a bad handler in the config: "
                (get-in config [1 :handler-fn]))
      (assoc-in config [1 :handler-fn] shared/handler-no-op))))

(defmulti gen-component-map
  (fn [[queue-name config]]
    (:type config)))

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

(defmethod gen-component-map :outgoing-event
  [config]
  nil
  )

(defmethod gen-component-map :incoming-event
  [config]
  nil
  )

(defn- gen-system-map
  [{:keys [connection max-retries queues topics]}]
  (let [rmq-base [:rabbitmq (krmq/new-kehaar-rabbitmq connection max-retries)]]
    (reduce
     (fn [components queue-config]
       (if-let [config (gen-component-map queue-config)]
         (conj components (keyword (first queue-config)) config)
         components))
     rmq-base
     queues)))

(defn system [config-options]
  (let [system-map (gen-system-map config-options)]
    (log/debug "System-map: " (pr-str system-map))
    (apply component/system-map system-map)))
