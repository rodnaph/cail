
(defproject rodnaph/cail "0.6.7"
  :description "Sane javax.mail parser"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [javax.mail/mail "1.4.7"]]
  :profiles {:dev {:dependencies [[midje "1.7.0"]]
                   :plugins [[lein-midje "3.1.1"]]}})

