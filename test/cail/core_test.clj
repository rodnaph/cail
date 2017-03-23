
(ns cail.core-test
  (:import [java.io File FileInputStream]
           [javax.mail Session]
           [javax.mail.internet MimeMessage]
           [java.util Properties])
  (:require [cail.core :refer :all]
            [midje.sweet :refer :all]))

(defn- create-message
  [id]
  (let [session (Session/getInstance (Properties.))
        file (File. (format "test/data/email_%s.txt" id))
        stream (FileInputStream. file)]
    (MimeMessage. session stream)))

(defn- parse-message
  ([id]
   (message->map (create-message id)))
  ([id fields]
   (message->map (create-message id) fields)))

(facts "about parsing basic message properties"
       (fact "default fields returned when none specified"
             (parse-message "simple") => (just {:attachments (list)
                                                :bcc (list)
                                                :body "<div dir=\"ltr\">Test Message</div>\r\n"
                                                :cc (list)
                                                :content-type "text/html"
                                                :from {:email "foo@bar.com"
                                                       :name "admin admin"}
                                                :id 0
                                                :reply-to {:email "foo@bar.com"
                                                           :name "admin admin"}
                                                :sent-on #inst "2015-12-10T16:39:31.000-00:00"
                                                :size 230
                                                :subject "Test Subject"
                                                :to (list {:email "test@test.com"
                                                           :name nil})}))

       (fact "fields to be returned can be specified"
             (parse-message "simple" [:size :subject :charset])
             =>
             (just {:size 230
                    :subject "Test Subject"
                    :charset "UTF-8"}))

       (fact "case of content type is lowercased"
             (parse-message "uppercase") => (contains {:content-type "text/html"}))

       (future-fact "peek mode is used for IMAP messages"))

(facts "about parsing attachments"
       (fact "attachments can be parsed"
             (let [{:keys [attachments]} (parse-message "attachment_1")]
               (count attachments) => 1
               (first attachments) => (contains {:content-type "text/plain"
                                                 :charset "US-ASCII"
                                                 :file-name "attachment_cd.txt"
                                                 :type :attachment
                                                 :size 32
                                                 :content-stream nil})))

       (fact "filename of attachment falls back to content type name"
             (let [{:keys [attachments]} (parse-message "attachment_2")]
               (first attachments) => (contains {:content-type "image/png"
                                                 :charset "US-ASCII"
                                                 :file-name "image.png"
                                                 :type :inline})))

       (fact "filename of attachment is nill when nothing specified"
             (let [{:keys [attachments]} (parse-message "attachment_3")]
               (first attachments) => (contains {:file-name nil})))

       (fact "non textual multipaarts without disposition are attachments"
             (let [{:keys [body attachments]} (parse-message "attachment_5")]
               (count attachments) => 1
               body => "The Body"))

       (future-fact "peek mode is used for IMAP messages")

       (fact "inline attachments with no content id treated as regular attachments"
             (let [{:keys [attachments]} (parse-message "inline_attachments")]
               (count attachments) => 3
               (count (filter #(= :attachment (:type %)) attachments)) => 1))

       (fact "inline attachments with no disposition only a content ID supported"
             (let [{:keys [attachments]} (parse-message "inline_attachments")]
               (count attachments) => 3
               (count (filter #(= :inline (:type %)) attachments)) => 2)))

(facts "about parsing recipients"
       (parse-message "recipients") => (contains {:to (just [anything anything])
                                                  :cc (just [anything anything])
                                                  :bcc (just [anything anything])}))

(facts "about parsing inline attachments"
       (let [{:keys [attachments]} (parse-message "inline")]
         (first attachments) => (contains {:content-type "image/png"
                                           :type :inline
                                           :file-name "image.png"
                                           :content-id "<ii_151cefbe952bc940>"})))

(facts "about parsing embedded multipart messages"
       (parse-message "attachment_4") => (contains {:attachments (just [anything anything])
                                                    :subject "WS233870 - Torres Family Trust - Currently Valued Loss Runs / loss runs to the agent"}))

(facts "about parsing message RFC822"
       (let [{:keys [attachments]} (parse-message "message_rfc822")]
         (count attachments) => 4
         (count (filter #(= :attachment (:type %)) attachments)) => 1
         (count (filter #(= :inline (:type %)) attachments)) => 3))

(facts "about fetching attachments"
       (fact "they are returned numbered with their ID to fetch by"
             (let [message (parse-message "message_rfc822")]
               (:attachments message) => (just [(contains {:id 1})
                                                (contains {:id 2})
                                                (contains {:id 3})
                                                (contains {:id 4})])))

       (fact "they can be fetched individually"
             (message->attachment
               (create-message "message_rfc822")
               1) => (contains {:file-name "image001.jpg"
                                :id 1
                                :content-stream nil}))

       (fact "their content can be requested and read"
             (let [attachment (with-content-stream
                                (message->attachment
                                  (create-message "attachment_1") 1))]
               (slurp (:content-stream attachment)) => "test attachment content\n")))

