(ns kehaar-component.core-test
  (:require
    [clojure.core.async :as async]
    [clojure.test :refer [is deftest testing use-fixtures]]
    [com.stuartsierra.component :as component]
    [kehaar-component.events-exchange :as ee]
    [kehaar-component.external-service :as es]
    [kehaar-component.incoming-event-service :as ies]
    [kehaar-component.incoming-service :as is]
    [kehaar-component.kehaar-rabbitmq :as krmq]
    [kehaar-component.outgoing-event-service :as oes]
    [kehaar-component.system :as system]
    [langohr.core :as rmq]))

;;
;; These tests require RabbitMQ. An easy way to get it up and running
;; and available at the config set in the rmq-config below if you have
;; docker installed is
;;
;;
;; $ docker pull rabbitmq
;; $ docker run -d -p 127.0.0.1:5672:5672 -p 127.0.0.1:15672:15672 \
;;     --hostname my-rabbit --name some-rabbit rabbitmq:3-management
;;

(def rmq-config
  {:connection {:host "localhost"
                :port 5672}
   :max-retries 5})

(def incoming-service-config
  {:config {:type :incoming
            :queue-name "direct-exchange-queue-name"
            :handler-fn #(-> % (assoc :status :ok))
            :exclusive false
            :durable true
            :auto-delete false}})

(def external-service-config
  (-> incoming-service-config
      (assoc-in [:config :type] :external)
      (update :config dissoc :handler-fn)))

(def incoming-events-stub-chan1 (async/chan))
(def incoming-events-stub-chan2 (async/chan))

(def incoming-event-service1-config
  {:config {:type :incoming-event
            :queue-name "topic-exchange-consume-queue1"
            :handler-fn #(async/put! incoming-events-stub-chan1 %)
            :routing-key "test.events"
            :exclusive false
            :durable true
            :auto-delete false}})

(def incoming-event-service2-config
  {:config {:type :incoming-event
            :queue-name "topic-exchange-consume-queue2"
            :handler-fn #(async/put! incoming-events-stub-chan2 %)
            :routing-key "test.events"
            :exclusive false
            :durable true
            :auto-delete false}})

(def outgoing-event-service-config
  (-> incoming-event-service1-config
      (assoc :queue-name "topic-exchange-produce-queue")
      (assoc-in [:config :type] :outgoing-event)
      (update :config dissoc :handler-fn)))


(deftest ^:rabbitmq kehaar-component-services
  (let [{:keys [connection max-retries]} rmq-config
        rmq-component (krmq/new-kehaar-rabbitmq connection max-retries)
        rmq-running (component/start rmq-component)]

    (testing "Starts up RabbitMQ service"
      (is (not (rmq/closed? (:connection rmq-running)))))

    (testing "Shuts down RabbitMQ service"
      (is (nil? (:connection (component/stop rmq-running))))
      (is (rmq/closed? (:connection rmq-running))))

    (testing "Starts up and shuts down incoming and external services"
      (let [rmq-component (component/start rmq-component)
            is-component (-> incoming-service-config
                             (assoc :rabbitmq rmq-component)
                             is/new-incoming-service
                             component/start)
            es-component (-> external-service-config
                             (assoc :rabbitmq rmq-component)
                             es/new-external-service
                             component/start)]

        (is (= (async/<!! (es/call-external! es-component {:foo "bar"}))
               {:status :ok :foo "bar"}))

        (is (nil? (:service (component/stop is-component))))
        (is (rmq/closed? (:service is-component)))
        (is (nil? (:service (component/stop es-component))))
        (is (rmq/closed? (:service es-component)))

        ;; Close RMQ connection
        (component/stop rmq-component)))

    (testing "Starts up incoming event and outgoing event services"
      (let [rmq-component (component/start rmq-component)
            events-exchange (->> {:rabbitmq rmq-component}
                                 ee/new-events-exchange
                                 component/start)
            ies-component1 (-> incoming-event-service1-config
                               (assoc :rabbitmq rmq-component)
                               ies/new-incoming-event-service
                               component/start)
            ies-component2 (-> incoming-event-service2-config
                               (assoc :rabbitmq rmq-component)
                               ies/new-incoming-event-service
                               component/start)
            oes-component (-> outgoing-event-service-config
                              (assoc :rabbitmq rmq-component)
                              oes/new-outgoing-event-service
                              component/start)]

        (async/put! (:outgoing-events-chan oes-component) {:foo "bar"})
        (is (= {:foo "bar"} (async/<!! incoming-events-stub-chan1)))
        (is (= {:foo "bar"} (async/<!! incoming-events-stub-chan2)))

        (is (nil? (:service (component/stop ies-component1))))
        (is (rmq/closed? (:service ies-component1)))
        (is (nil? (:service (component/stop ies-component2))))
        (is (rmq/closed? (:service ies-component2)))
        (is (nil? (:service (component/stop oes-component))))
        (is (rmq/closed? (:service oes-component)))

        ;; Close RMQ connection
        (component/stop rmq-component)))

    ;; TODO add tests for system
    ))
