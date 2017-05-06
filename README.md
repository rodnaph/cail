# cail ("sail") [![Build Status](https://api.travis-ci.org/rodnaph/cail.png)](http://travis-ci.org/rodnaph/cail) [![Dependencies Status](http://clj-deps.herokuapp.com/github/rodnaph/cail/status.png)](http://clj-deps.herokuapp.com/github/rodnaph/cail)

Cail is a small library for parsing the insane _javax.mail_ library
classes into Clojure data structures.  It doesn't do everything, but
it should help with the 99% use case of "give me the emails and their
attachments".

[![Clojars Project](https://img.shields.io/clojars/v/rodnaph/cail.svg)](https://clojars.org/rodnaph/cail)

## Usage

```clojure
(require [cail.core :refer [message->map]])

(def original 
  (.getJavaMailMessageFromSomewhereIn javaLand))

(def message 
  (message->map original))

(:subject message) ; => "The Subject"

(count 
  (:attachments message)) ; => 3

; etc...
```

The message body will prefer HTML content types.

## IMAP

When given a message to parse it will still require the connection
to its associated folder to fetch its data from.  Also be aware that
the _:id_ attached to each message is dependent on its folder, and
can change depending on if messages get deleted or moved - so use `uid`
where available.

## Attachments

Due to the possible (and probable) size of attachment content this
is not extracted by default.  All the metadata for attachments will
be available after parsing a message, but to fetch the content
you need to ask for it.

```clojure
(def msg 
  (with-content-stream
    (message->map message)))

(def attachments 
   (:attachments msg))

(println 
  (count attachments )) ; => 2

(def attachment 
  (first attachments))

(:content-type attachment) ; => "application/pdf"

(spit 
  "~/file.pdf" 
  (:content-stream attachment))
```

The _:content-stream_ is an input stream for reading the attachment.

### Individual Attachments

Attachments come with an `:id` property you can use to ask for them individually...

```clojure
; (message->map msg)
; {:attachments [{:id 1} {:id 2}]}
  
(def attachment 
  (message->attachment msg 1))
```

By default this will *not* return the content stream for the 
attachment, just wrap it in _with-content-stream_ to fetch that.

```clojure
(def attachment
  (with-content-stream
    (message->attachment message 1)))
```

## Selecting Fields

The *message->map* function returns a bunch of fields by default. If you want
to only return a subset of these (eg. for working with FetchProfiles) then you
can specify these as the second argument.

```clojure
(message->map msg [:id :subject :from])
```

## Cachability

One goal of Cail is to allow messages to be cacheable. So after
parsing they don't rely on any internal state (connections to
mail stores) that the Java versions do.

This means that after parsing and caching a message the only time
you need to go back to the mail store is to fetch the content
of attachments.

## Testing

Run all tests with `make`

```
$> make
```

## Installation

Cail is available from [Clojars](https://clojars.org/rodnaph/cail)
