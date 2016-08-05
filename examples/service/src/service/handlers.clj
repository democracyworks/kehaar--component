(ns service.handlers)

(defn handle-incoming
  [msg]
  (println "msg! " msg)
  {:status :ok})

(defn handle-incoming-events
  [msg]
  (println "incoming event: " msg)
  (println))

(defn handle-incoming-events-redux
  [msg]
  (println "incoming event (redux): " msg)
  (println))
