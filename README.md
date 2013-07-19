
# cail ("sail")

Cail is a small library for parsing the insane _javax.mail_ library
classes into Clojure data structures.

## Usage

```clojure
(require [cail.core :refer [message->map]])

(def original (.getJavaMailMessageFromSomewhere java))

(def message (message->map original))

(:subject message) ; => "The Subject"
(count (:attachments message)) ; => 3

; etc...
```

