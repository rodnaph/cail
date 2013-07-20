
(ns cail.fixtures
  (:import (java.util Properties)
           (javax.mail Session Part)
           (javax.mail.internet MimeMultipart MimeMessage InternetAddress MimeBodyPart)))

(def non-attachment (doto (MimeBodyPart.)))

(def attachment (doto (MimeBodyPart.)
                  (.setFileName "foo.txt")
                  (.setDisposition Part/ATTACHMENT)))

(def address (InternetAddress. "foo bar <foo@bar.com>"))
(def session (Session/getInstance (Properties.)))
(def multipart (doto (MimeMultipart.)
                 (.addBodyPart non-attachment)
                 (.addBodyPart attachment)))

;; Public
;; ------

(defn make-message []
  (doto (MimeMessage. session)
               (.setSubject "subject")
               (.setSender address)
               (.setContent multipart)))

