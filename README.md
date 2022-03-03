# scratch

A Decentralized Platform for Supply chain and Resource Management, which allows creation and sharing of production Recipes, Inventory management, scheduling and production costing with a Supplier and Customer Market place  built on the [Secure Scuttlebutt](https://securescuttlebutt.nz) protocol to provide securely signed cryptographic ledgers that are distributed via a decentralized gossip network.

![recipe.gif](/docs/recipe.gif)

## Development Mode

### Run application:

This application can be run in a Clojurescript developement mode by using [Shadow-cljs]( https://shadow-cljs.github.io/docs/UsersGuide.html) build tool. At the command line run the following, to install.
```
npm install -g shadow-cljs
```

Then compile and hot reload changes that you make to the source file:

```
shadow-cljs watch server
```
Launch the node server:
```
node out/main.js
```
and compile the client similarly
```
shadow-cljs watch client
```

Wait a bit, then browse to [http://localhost:3449](http://localhost:3449).

### Emacs
In Emacs connect with Cider to Clojurescript REPL use 'M-x cider-connect', 
then run the following at the repl:
```
(shadow.cljs.devtools.api/nrepl-select :server) 
```

Then switch to the server.main namespace from REPL
```
(in-ns 'server.core)
```

Or connect to REPL for the build from the command line:
```
shadow-cljs cljs-repl app
```

# Compile Release build
```
shadow-cljs release app
```

# Tutorials
TODO

# How to Guides
TODO

# Reference Guide
TODO

# Explanations
- see the Glossary and Datastructure section of Notes.org
