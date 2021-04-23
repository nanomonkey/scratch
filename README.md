# scratch

A Decentralized Platform for Supply chain and Resource Management, which allows creation and sharing of production Recipes, Inventory management, scheduling and production costing with a Supplier and Customer Market place  built on [Secure Scuttlebutt](https://securescuttlebutt.nz) to provide securely signed cryptographic ledgers that are distributed via a gossip network.

![recipe.gif](/docs/recipe.gif)

## Development Mode

### Run application:

This application can be run in a Clojure development mode by using the [Leinegen](https://leiningen.org) build tool.

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

# Tutorials
# How to Guides
# Reference Guides
# Explanations
