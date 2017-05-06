
.PHONY: cljfmt deps midje

all: deps cljfmt midje

deps:
	lein deps

cljfmt:
	lein cljfmt check

midje:
	lein midje
