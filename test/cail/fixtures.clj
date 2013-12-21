
(ns cail.fixtures
  (:import (java.util Properties)
           (javax.mail Session Part Message$RecipientType)
           (javax.mail.internet MimeMultipart MimeMessage InternetAddress MimeBodyPart)))

(def defaults {:from "foo bar <foo@bar.com>"
               :subject "subject"
               :to ["bim@bom.com"]})

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

(defn ->address [email]
  (InternetAddress. email))

;; Public
;; ------

(defn make-message
  ([] (make-message {}))
  ([params]
    (let [{:keys [from subject]} (merge defaults params)]
      (doto (MimeMessage. session)
        (.setFrom (->address from))
        (.addRecipient Message$RecipientType/TO
                       (->address "a@b.com"))
        (.addRecipient Message$RecipientType/TO
                       (->address "b@c.com"))
        (.setSubject subject)
        (.setContent multipart)
        (.saveChanges)))))

