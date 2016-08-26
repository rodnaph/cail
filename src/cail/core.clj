
(ns cail.core
  (:import (javax.mail Address Message Message$RecipientType Part)
           (javax.mail.internet MimeMessage MimeMultipart))
  (:require [clojure.string :as string]))

(def ^{:dynamic true} *with-content-stream* false)

(def default-address {:name nil :email ""})

(def default-fields [:id :subject :body :from :to :cc :bcc :reply-to :sent-on :content-type :size :attachments])

(defn address->map [^Address address]
  (if address
    {:name (.getPersonal address)
     :email (.getAddress address)}
    default-address))

(defn is-disposition [part disposition]
  (.equalsIgnoreCase
    disposition
    (.getDisposition part)))

;; Attachments
;; -----------

(defn attachment?
  [multipart]
  (is-disposition multipart Part/ATTACHMENT))

(defn inline?
  [multipart]
  (is-disposition multipart Part/INLINE))

(defn any-attachment?
  [multipart]
  (or (attachment? multipart)
      (inline? multipart)))

(defn- multiparts*
  [^MimeMultipart mp f]
  (for [i (range 0 (.getCount mp))]
    (let [p (.getBodyPart mp i)
          c (.getContent p)]
      (if (instance? MimeMultipart c)
        (multiparts* c f)
        (when (f p) p)))))

(defn multiparts
  [^MimeMessage msg f]
  (filter
    (complement nil?)
    (flatten
      (let [content (.getContent msg)]
          (if (instance? MimeMultipart content)
            (multiparts* content f)
            content)))))

(defn ->attachment
  [part id]
  {:content-id (.getContentID part)
   :content-stream (when *with-content-stream*
                     (.getInputStream part))
   :content-type (.getContentType part)
   :file-name (.getFileName part)
   :id id
   :size (.getSize part)
   :type (if (inline? part) :inline :attachment)})

(defn attachments
  [^MimeMessage msg]
  (let [xs (multiparts msg any-attachment?)]
    (map ->attachment
         xs
         (range 1 (inc (count xs))))))

;; Message Body
;; ------------

(defn prefer-html
  [x y]
  (if (string/starts-with?
        (.getContentType x)
        "text/html")
    -1 1))

(defn body
  [^MimeMessage msg]
  (first
    (sort
      prefer-html
      (multiparts msg (complement any-attachment?)))))

;; Fields
;; ------

(defmulti field (comp first list))

(defmethod field :id
  [_ ^Message msg]
  (.getMessageNumber msg))

(defmethod field :uid
  [_ ^Message msg]
  (.getUID (.getFolder msg) msg))

(defmethod field :subject
  [_ ^Message msg]
  (.getSubject msg))

(defmethod field :body
  [_ ^Message msg]
  (.getContent (body msg)))

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
  (.getContentType (body msg)))

(defmethod field :size
  [_ ^Message msg]
  (.getSize msg))

(defmethod field :attachments
  [_ ^Message msg]
  (attachments msg))

(defmethod field :attachment-count
  [_ ^Message msg]
  (count (attachments msg)))

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
  (nth (attachments msg) (dec index)))

