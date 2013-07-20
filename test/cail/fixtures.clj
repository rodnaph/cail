
(ns cail.fixtures
  (:import (java.util Properties)
           (javax.mail Session Part)
           (javax.mail.internet MimeMultipart MimeMessage InternetAddress MimeBodyPart)))

(def session (Session/getInstance (Properties.)))

(def inline-content (doto (MimeBodyPart.)
                      (.setHeader "Content-Type" "text/html")
                      (.setText "<b>HTML</b>")
                      (.setDisposition Part/INLINE)))

(def attachment (doto (MimeBodyPart.)
                  (.setHeader "Content-Type" "application/pdf")
                  (.setFileName "foo.txt")
                  (.setDisposition Part/ATTACHMENT)))

(def address (InternetAddress. "foo bar <foo@bar.com>"))

(def multipart (doto (MimeMultipart.)
                 (.addBodyPart inline-content)
                 (.addBodyPart attachment)))

;; Public
;; ------

(defn make-message []
  (doto (MimeMessage. session)
    (.setSubject "subject")
    (.setFrom address)
    (.setContent multipart)
    (.saveChanges)))

