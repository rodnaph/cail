
(defproject rodnaph/cail "0.8.7"
  :description "Sane javax.mail parser"
  :dependencies [[cljfmt "0.5.6"]
                 [org.clojure/clojure "1.8.0"]
                 [javax.mail/mail "1.4.7"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]
                   :plugins [[lein-cljfmt "0.5.6"]
                             [lein-midje "3.2.1"]]}})
