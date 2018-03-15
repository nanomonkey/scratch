# scratch

A Decentralized Recipe tool built on 
[Secure Scuttlebutt](https://securescuttlebutt.nz), 

written in Clojure using the following libraries 
[reagent](https://holmsand.github.io/reagent/) interface for React, which utilizes [Hiccup](https://github.com/weavejester/hiccup) for HTML templating.
[re-frame](https://github.com/Day8/re-frame) for front end state management.
Secretary and Accountant for routing.


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
