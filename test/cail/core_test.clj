
(ns cail.core-test
  (:require [clojure.test :refer [deftest]]
            [cail.core :refer :all]
            [cail.fixtures :refer [make-message]]))

(let [msg (message->map (make-message))]
  (deftest a-message-can-be-parsed-to-a-map
    (is (= "subject" (:subject msg)))
    (is (< 0 (count (:attachments msg))))))

(let [msg (message->map (make-message))
      attachment (first (:attachments msg))]
  (deftest attachments-have-properties-available
    (is (= "application/pdf" (:content-type attachment)))
    (is (= 123 (:size attachment)))
    (is (= "foo.txt" (:file-name attachment)))))

(run-tests)

