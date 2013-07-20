
(ns cail.core
  (:import (javax.mail Address Message BodyPart Part Multipart)))

(defn address->map [^Address address]
  {:address (.toString address)})

(defn multiparts [^Multipart multipart]
  (for [i (range 0 (.getCount multipart))]
    (.getBodyPart multipart i)))

(defn content-type [^Part part]
  (if-let [ct (.getContentType part)]
    (if (.contains ct ";")
      (.toLowerCase (.substring ct 0 (.indexOf ct ";")))
      ct)))

(defn content-fn [^BodyPart part]
  (fn []
    (slurp (.getContent part))))

(defn part->attachment [^BodyPart part]
  {:content-type (content-type part)
   :file-name (.getFileName part)
   :size (.getSize part)
   :content (content-fn part)})

(defn attachment? [^Part part]
  (.equalsIgnoreCase Part/ATTACHMENT
                     (.getDisposition part)))

(defmulti attachments class)

(defmethod attachments String
  [content]
  [{:content-type "text/plain"
    :content (fn [] content)}])

(defmethod attachments Multipart
  [multipart]
  (->> (multiparts multipart)
       (filter attachment?)
       (map part->attachment)))

;; Public
;; ------

(defn ^{:doc "Parse a Message into a map"}
  message->map [^Message msg]
  {:subject (.getSubject msg)
   :content-type (content-type msg)
   :encoding (.getEncoding msg)
   :from (address->map (first (.getFrom msg)))
   :message-id (.getMessageID msg)
   :date-received (.getReceivedDate msg)
   :reply-to (address->map (first (.getReplyTo msg)))
   :sender (address->map (.getSender msg))
   :date-sent (.getSentDate msg)
   :size (.getSize msg)
   :attachments (attachments (.getContent msg))})

(defn ^{:doc "Fetch stream for reading the content of the attachment at index"}
  message->attachment [^Message msg index]
  (throw "NOT IMPLEMENTED"))

