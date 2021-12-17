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

### Installing new npm dependencies:
```
npm install the-thing
```

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
# How to Guides
# Reference Guides
# Explanations
## Items
Physical items in your inventory, equipment you use, and products which you produce, or purchase from a supplier are all saved as Items. Items have names, descriptions and are uniquely addressable.
## Units
### SI Base Units
- Meter (m) length unit of measurement:
  Distance traveled by light in a vacuum in 1/299,792,458 seconds
- Second (s) time unit of measurement:
  9,192,631,770 cycles of radiation of an atom of caesium-133
- Kilogram (kg) mass unit of measurement:
  Planck’s constant divided by 6.626,070,15 × 10−34 m−2s
- Candela (cd) luminous intensity measurement unit:
  Light source with monochromatic radiation of frequency 540 × 1012 Hz and radiant intensity of 1/683 watt per steradian
- Kelvin (K) temperature unit of measurement:
  Boltzmann constant, defined as a change in thermal energy of 1.380 649 × 10−23 joules
- Ampere (A) electric current measurement unit:
  Flow equal to 1/1.602176634×10−19 elementary charges per second
- Mole (mol) amount of substance measurement unit:
  Avogadro constant, defined as 6.02214076 ×1023 elementary entities.

## Tasks
Tasks are a list of instructions for completing a specific task, the inventory items that will be consumed, the equipment you'll use and the final product (if any) from completing the task.  Tasks that don't result in a product are often called Services. A task is different than a recipe in that each individual step needs to be done in order and cannot be done concurrently by seperate individual, or at a later time.

## Recipes
A Recipe is a list of individual tasks that result in finished good or service. Each task is seperated out by the equipment used and whether or not the task can be done in concurrently, or in a specific order.
## Ingredients
The items consumed in a recipe or task.
## Equipment
Any item that isn't consummed in a task such as tools or furniture.
## Products
The items produced in a recipe or task.
## Inventory
Items stored in a specific location for later use.
## Suppliers
The manufacturer or seller of items that you use.
## Events
The date, time and location used when scheduling tasks.
# Messages
