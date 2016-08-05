(ns kehaar-component.shared
  (:require
    [clojure.tools.logging :as log]))

(defn handle-errors
  [handler]
  (fn [message]
    (try
      (handler message)
      (catch IllegalArgumentException semantic-error
        (log/error semantic-error)
        {:status :error
         :error {:type :semantic
                 :message (.getMessage semantic-error)}})
      (catch clojure.lang.ExceptionInfo validation-error
        (log/error validation-error)
        {:status :error
         :error {:type :validation
                 :message (.getMessage validation-error)}})
      (catch RuntimeException runtime-exception
        (log/error runtime-exception)
        {:status :error
         :error {:type :server
                 :server (str "Unknown server error: "
                              (.getMessage runtime-exception))}}))))

(defn handler-no-op
  [msg]
  (log/info "No-op here because we didn't get a handler-fn passed in. Message: " msg)
  {:status :ok})
