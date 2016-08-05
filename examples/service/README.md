# service

Example service illustrating how one may use kehaar-component.

Take a look at resources/config.edn to see what each type of service uses for configuration.


## Usage

```clojure
service.core> (-main)
;; Starting RabbitMQ
;; Starting IncomingService  incoming-service-queue
;; Starting EventsExchange
;; Starting OutgoingEventService  outgoing-event-queue
;; Starting IncomingEventService  incoming-event-queue-redux
;; Starting ExternalService  external-service-queue
;; Starting IncomingEventService  incoming-event-queue
#object[clojure.core.async.impl.channels.ManyToManyChannel 0x71c36196 "clojure.core.async.impl.channels.ManyToManyChannel@71c36196"]
service.core> (def ext-config (assoc-in (get-in @system [:incoming-service-queue :config]) [1 :type] :external))
#'service.core/ext-config
service.core> (require '[kehaar-component.external-service :as es])
nil
service.core> (def es1 (component/start (es/new-external-service {:config ext-config :rabbitmq (:rabbitmq @system)})))
;; Starting ExternalService  incoming-service-queue
#'service.core/es1
service.core> (async/<!! (es/call-external! es1 {:test "test!"}))
msg!  {:test test!}
{:status :ok}
service.core> (async/put! (get-in @system [:outgoing-event-queue :outgoing-events-chan]) {:test "sending this to everyone"})
true
service.core> incoming event:  {:test sending this to everyone}
incoming event (redux):  
{:test sending this to everyone}


service.core> 
service.core> (shut-it-down!)
trueGot the shutdown signal! Shutting down.
service.core> ;; Stopping IncomingEventService  incoming-event-queue
;; Stopping ExternalService  external-service-queue
;; Stopping IncomingEventService  incoming-event-queue-redux
;; Stopping OutgoingEventService  outgoing-event-queue
;; Stopping EventsExchange
;; Stopping IncomingService  incoming-service-queue
;; Stopping RabbitMQ

service.core> 
```

## License

Copyright © 2016 Democracy Works

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
