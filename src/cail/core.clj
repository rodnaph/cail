
(ns cail.core
  (:import (javax.mail Message BodyPart Part Multipart)
           (javax.mail.internet InternetAddress)))

(defn address->map [^InternetAddress address]
  {:name (.getPersonal address)
   :email (.getAddress address)})

(defn multiparts [^Multipart multipart]
  (for [i (range 0 (.getCount multipart))]
    (.getBodyPart multipart i)))

(defn content-type [^Part part]
  (if-let [ct (.getContentType part)]
    (if (.contains ct ";")
      (.toLowerCase (.substring ct 0 (.indexOf ct ";")))
      ct)))

(defn parts [^Multipart multipart part-type]
  (filter #(.equalsIgnoreCase part-type (.getDisposition %))
          (multiparts multipart)))

;; Attachments
;; -----------

(defn content-fn [^BodyPart part]
  (fn []
    (slurp (.getContent part))))

(defn part->attachment [^BodyPart part]
  {:content-type (content-type part)
   :file-name (.getFileName part)
   :size (.getSize part)
   :content (content-fn part)})

(defmulti attachments class)

(defmethod attachments String
  [content]
  [{:content-type "text/plain"
    :size (count content)
    :content (fn [] content)}])

(defmethod attachments Multipart
  [multipart]
  (map part->attachment
       (parts multipart Part/ATTACHMENT)))

;; Message Body
;; ------------

(defmulti message-body class)

(defmethod message-body String
  [content]
  content)

(defmethod message-body Multipart
  [multipart]
  (if-let [part (first (parts multipart Part/INLINE))]
    (.getContent part)
    ""))

;; Public
;; ------

(defn ^{:doc "Parse a Message into a map"}
  message->map [^Message msg]
  {:subject (.getSubject msg)
   :body (message-body (.getContent msg))
   :from (address->map (first (.getFrom msg)))
   :reply-to (address->map (first (.getReplyTo msg)))
   :date-sent (.getSentDate msg)
   :content-type (content-type msg)
   :size (.getSize msg)
   :attachments (attachments (.getContent msg))})

(defn ^{:doc "Fetch stream for reading the content of the attachment at index"}
  message->attachment [^Message msg index]
  (throw "NOT IMPLEMENTED"))

