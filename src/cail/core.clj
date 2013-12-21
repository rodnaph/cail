
(ns cail.core
  (:import (javax.mail Address BodyPart Message Multipart Part)))

(def ^{:dynamic true} *with-content-stream* false)

(def default-address {:name nil :email ""})

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
                     (.getContent part))})

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

;; Public
;; ------

(defmacro with-content-stream [& body]
  `(binding [*with-content-stream* true]
     (do ~@body)))

(defn ^{:doc "Parse a Message into a map"}
  message->map [^Message msg]
  (let [recipients (map address->map (.getAllRecipients msg))]
    {:id (.getMessageNumber msg)
     :subject (.getSubject msg)
     :body (message-body (.getContent msg))
     :from (address->map (first (.getFrom msg)))
     :recipients recipients
     :to (first recipients)
     :reply-to (address->map (first (.getReplyTo msg)))
     :sent-on (.getSentDate msg)
     :content-type (content-type msg)
     :size (.getSize msg)
     :attachments (attachments (.getContent msg))}))

(defn ^{:doc "Fetch stream for reading the content of the attachment at index"}
  message->attachment [^Message msg index]
  (if-let [part (nth (attachment-parts (.getContent msg)) index)]
    (part->attachment part)))

