
(ns cail.core-test
  (:require [clojure.test :refer [deftest is run-tests]]
            [cail.core :refer :all]
            [cail.fixtures :refer [make-message]]))

(let [msg (message->map (make-message))]
  (deftest a-message-can-be-parsed-to-a-map
    (is (= "foo bar" (-> msg :from :name)))
    (is (= "foo@bar.com" (-> msg :from :email)))
    (is (= "subject" (:subject msg)))
    (is (< 0 (count (:attachments msg))))
    (is (= "<b>HTML</b>" (:body msg)))))

(let [msg (with-content-stream (message->map (make-message)))
      attachment (first (:attachments msg))]
  ; @todo
  ;(is (not (nil? (:content-stream attachment))))
  )

(let [msg (message->map (make-message))
      attachment (first (:attachments msg))]
  (deftest attachments-have-properties-available
    (is (= "application/pdf" (:content-type attachment)))
    (is (= "foo.txt" (:file-name attachment)))))

(deftest parsing-different-email-formats
  (is (= {:name nil :email "foo@bar"}
         (:from (message->map (make-message {:from "foo@bar"}))))))

(let [msg (message->map (make-message))
      rcpts (:recipients msg)]
  (deftest parsing-recipients
    ; all recipients available
    (is (= 2 (count rcpts)))
    (is (= "a@b.com" (:email (first rcpts))))
    (is (= "b@c.com" (:email (second rcpts))))
    ; to address is first recipient
    (is (= (:to msg) (first rcpts)))))

(let [msg (message->map (make-message) [:id])]
  (deftest restricting-fields
    (is (not (nil? (:id msg))))
    (is (nil? (:from msg)))))

(run-tests)

