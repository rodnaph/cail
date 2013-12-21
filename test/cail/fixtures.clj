
(ns cail.fixtures
  (:import (java.util Properties)
           (javax.mail Session Part)
           (javax.mail.internet MimeMultipart MimeMessage InternetAddress MimeBodyPart)))

(def defaults {:from "foo bar <foo@bar.com>"
               :subject "subject"})

(def session (Session/getInstance (Properties.)))

(def inline-content (doto (MimeBodyPart.)
                      (.setText "<b>HTML</b>" "text/html")
                      (.setDisposition Part/INLINE)))

(def attachment (doto (MimeBodyPart.)
                  (.setFileName "foo.txt")
                  (.setContent "content of file" "application/pdf")
                  (.setDisposition Part/ATTACHMENT)))

(def multipart (doto (MimeMultipart.)
                 (.addBodyPart inline-content)
                 (.addBodyPart attachment)))

;; Public
;; ------

(defn make-message
  ([] (make-message {}))
  ([params]
    (let [{:keys [from subject]} (merge defaults params)]
      (doto (MimeMessage. session)
        (.setSubject subject)
        (.setFrom (InternetAddress. from))
        (.setContent multipart)
        (.saveChanges)))))

