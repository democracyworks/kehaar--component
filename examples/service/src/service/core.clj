(ns service.core
  (:require
    [clojure.core.async :as async]
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [com.stuartsierra.component :as component]
    [kehaar-component.system :as kc-system]
    [service.handlers])) ; needs to be included so handler vars are found

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
    ;; This would be where something like immutant/at-exit goes;
    ;; the core.async stuff here is just a simple way to start this
    ;; up and have a way to shut it down in order to play with it
    ;; as though it were a service running independently.
    (async/go
        (let [shut-it-down (async/<! shutdown-chan)]
          (println "Got the shutdown signal! Shutting down.")
          (component/stop @system)
          (reset! system nil)))))
