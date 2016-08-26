# cail ("sail") [![Build Status](https://api.travis-ci.org/rodnaph/cail.png)](http://travis-ci.org/rodnaph/cail) [![Dependencies Status](http://clj-deps.herokuapp.com/github/rodnaph/cail/status.png)](http://clj-deps.herokuapp.com/github/rodnaph/cail)

Cail is a small library for parsing the insane _javax.mail_ library
classes into Clojure data structures.  It doesn't do everything, but
it should help with the 99% use case of "give me the emails and their
attachments".

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

When given a message to parse it will still require the connection
to its associated folder to fetch its data from.  The message body
will prefer HTML content types.

### Message IDs

Be aware that the _:id_ attached to each message is dependent on its
folder, and can change depending on if messages get deleted or moved.

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

You can also ask for individual attachments by index...

```clojure
(def attachment 
  (message->attachment message 0))
```

By default this will *not* return the content stream for the 
attachment, just wrap it in _with-content-stream_ to fetch that.

```clojure
(def attachment
  (with-content-stream
    (message->attachment message 0)))
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
of attachments (which could be many MB's so are not cached).

## Testing

Tests use Midje.

```
lein midje
```

## Installation

Cail is available from [Clojars](https://clojars.org/rodnaph/cail)
