
(ns cail.core
  (:import (javax.mail Address BodyPart Message Message$RecipientType Multipart Part)
           (javax.mail.internet MimeMessage)
           (com.sun.mail.imap IMAPMessage)
           (com.sun.mail.util BASE64DecoderStream)))

(def ^{:dynamic true} *with-content-stream* false)

(def default-address {:name nil :email ""})

(def default-fields [:id :subject :body :from :recipients :to :reply-to :sent-on :content-type :size :attachments])

(defn address->map [^Address address]
  (if address
    {:name (.getPersonal address)
     :email (.getAddress address)}
    default-address))

(defn multiparts [^Multipart multipart]
  (for [i (range 0 (.getCount multipart))]
    (.getBodyPart multipart i)))

(defn content-type [^Part part]
  (if-let [ct (.getContentType part)]
    (if (.contains ct ";")
      (.toLowerCase (.substring ct 0 (.indexOf ct ";")))
      ct)))

;; Content Streams
;; ---------------

(defmulti content-stream class)

(defmethod content-stream BASE64DecoderStream
  [content]
  content)

(defmethod content-stream MimeMessage
  [content]
  (.getRawInputStream content))

(defmethod content-stream :default
  [content]
  content)

;; Attachments
;; -----------

(defn attachment? [multipart]
  (.equalsIgnoreCase
    Part/ATTACHMENT
    (.getDisposition multipart)))

(defn part->attachment [^BodyPart part]
  {:content-type (content-type part)
   :file-name (.getFileName part)
   :size (.getSize part)
   :content-stream (if *with-content-stream*
                     (content-stream (.getContent part)))})

(defn attachment-parts [^Multipart multipart]
  (->> (multiparts multipart)
       (filter attachment?)))

(defmulti attachments class)

(defmethod attachments String
  [content]
  [{:content-type "text/plain"
    :size (count content)
    :content content}])

(defmethod attachments Multipart
  [multipart]
  (map part->attachment
       (attachment-parts multipart)))

;; Message Body
;; ------------

(defn textual? [multipart]
  (let [ct (content-type multipart)]
    (or (= "text/plain" ct)
        (= "text/html" ct)
        (= "multipart/alternative" ct))))

(defmulti message-body class)

(defmethod message-body String
  [content]
  content)

(defmethod message-body Multipart
  [multipart]
  (if-let [part (->> (multiparts multipart)
                     (filter (complement attachment?))
                     (filter textual?)
                     (last))]
    (message-body (.getContent part))))

;; Attachment
;; ----------

(defmulti message-attachment (comp class first list))

(defmethod message-attachment String
  [content _]
  {:content-type "text/plain"
   :size (count content)
   :content-stream content})

(defmethod message-attachment Multipart
  [content index]
  (if-let [part (nth (attachment-parts content) index)]
    (part->attachment part)))

;; Fields
;; ------

(defmulti field (comp first list))

(defmethod field :id
  [_ ^Message msg]
  (.getMessageNumber msg))

(defmethod field :uid
  [_ ^IMAPMessage msg]
  (.getUID (.getFolder msg) msg))

(defmethod field :subject
  [_ ^Message msg]
  (.getSubject msg))

(defmethod field :body
  [_ ^Message msg]
  (message-body (.getContent msg)))

(defmethod field :from
  [_ ^Message msg]
  (address->map (first (.getFrom msg))))

(defmethod field :recipients
  [_ ^Message msg]
  (map address->map (.getAllRecipients msg)))

(defmethod field :to
  [_ ^Message msg]
  (map address->map (.getRecipients msg Message$RecipientType/TO)))

(defmethod field :cc
  [_ ^Message msg]
  (map address->map (.getRecipients msg Message$RecipientType/CC)))

(defmethod field :bcc
  [_ ^Message msg]
  (map address->map (.getRecipients msg Message$RecipientType/BCC)))

(defmethod field :reply-to
  [_ ^Message msg]
  (address->map (first (.getReplyTo msg))))

(defmethod field :sent-on
  [_ ^Message msg]
  (.getSentDate msg))

(defmethod field :content-type
  [_ ^Message msg]
  (content-type msg))

(defmethod field :size
  [_ ^Message msg]
  (.getSize msg))

(defmethod field :attachments
  [_ ^Message msg]
  (attachments (.getContent msg)))

(defmethod field :attachment-count
  [_ ^Message msg]
  (let [content (.getContent msg)]
    (if (instance? Multipart content)
      (- (.getCount content) 1)
      -1)))

;; Public
;; ------

(defmacro with-content-stream [& body]
  `(binding [*with-content-stream* true]
     (do ~@body)))

(defn ^{:doc "Parse a Message into a map, optionally specifying which fields to return"}
  message->map
  ([^Message msg] (message->map msg default-fields))
  ([^Message msg fields] (reduce #(merge %1 {%2 (field %2 msg)}) {} fields)))

(defn ^{:doc "Fetch stream for reading the content of the attachment at index"}
  message->attachment [^Message msg index]
  (message-attachment (.getContent msg) index))

