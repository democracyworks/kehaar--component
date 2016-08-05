(defproject democracyworks/kehaar-component "0.1.0-SNAPSHOT"
  :description "Component (https://github.com/stuartsierra/component) -based wrapper for kehaar to simplify configuration and operation"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ch.qos.logback/logback-classic "1.1.7"]
                 [com.stuartsierra/component "0.3.1"]
                 [democracyworks/kehaar "0.5.0"]
                 [org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.385"]
                 [org.clojure/tools.logging "0.3.1"]
                 [prismatic/schema "1.0.3"]]
  :test-selectors {:default (complement :rabbitmq)
                   :rabbitmq :rabbitmq
                   :all (constantly true)})
