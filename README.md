# scratch

A Decentralized Recipe tool built on [Secure Scuttlebutt](https://securescuttlebutt.nz), written in Clojure, the [re-frame](https://github.com/Day8/re-frame) framework.

## Development Mode

### Run application:

This application can be run in development mode by using the [Leinegen](https://leiningen.org) build tool.

```
lein clean
lein figwheel dev
```

Figwheel will automatically push cljs changes to the browser.

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

## Production Build


To compile clojurescript to javascript:

```
lein clean
lein cljsbuild once min
```
