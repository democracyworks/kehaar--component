(ns service.core
  (:require
    [clojure.core.async :as async]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.stuartsierra.component :as component]
    [kehaar-component.system :as kc-system]))

(defn load-config
  []
  (edn/read-string (slurp (io/resource "config.edn"))))

(defonce system (atom nil))

(defonce shutdown-chan (async/chan))

(defn shut-it-down!
  []
  (async/put! shutdown-chan true))

(defn -main
  [& _]
  (let [config (load-config)]
    (reset! system (component/start (kc-system/system (:rabbitmq config))))
    ;; This would be where immutant/at-exit goes
    (async/go
        (let [shut-it-down (async/<! shutdown-chan)]
          (println "Got the shutdown signal! Shutting down.")
          (component/stop @system)
          (reset! system nil)))))
