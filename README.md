# Cubanostack

  [![Build Status](https://travis-ci.org/s-ted/cubanostack.svg)](https://travis-ci.org/s-ted/cubanostack)
  [![Dependencies Status](http://jarkeeper.com/cubane/stack/status.svg)](http://jarkeeper.com/cubane/stack)
  [![Download Status](https://jarkeeper.com/cubane/stack/downloads.svg)](https://jarkeeper.com/cubane/stack)

  Full spectrum website stack in [Clojure](https://github.com/clojure/clojure) and [ClojureScript](https://github.com/clojure/clojurescript) 

## Description

As I figured out, it is not simple to find a full stack of cutting edge technology libraries for building a website in one example.

*Cubanostack* is aimed at this, building and illustrating the use of the following libraries:
  - [cublono-quiescent](https://github.com/s-ted/cublono-quiescent) for easy writing of ReactJS component in,
  - [bidi](https://github.com/juxt/bidi) for routing both in Clojure and ClojureScript,
  - [postal](https://github.com/drewr/postal) for sending mail,
  - [liberator](https://github.com/clojure-liberator/liberator) for RESTfull backend coding,
  - [buddy](https://github.com/funcool/buddy) for user authentication (using JWT),
  - [hiccup](https://github.com/weavejester/hiccup) for HTML generation,
  - [slingshot](https://github.com/scgilardi/slingshot) for advanced exception handling,
  - [timbre](https://github.com/ptaoussanis/timbre) for logging,
  - [tower](https://github.com/ptaoussanis/tower) for i18n,
  - (optional) [sente](https://github.com/ptaoussanis/sente) for WebSocket communication between frontend and backend,
  - [component](https://github.com/stuartsierra/component) for module definition,
  - [schema](https://github.com/plumatic/schema) for data integrity and coercion,
  - [ring](https://github.com/ring-clojure/ring) for the web application abstraction,
  - [http-kit](https://github.com/http-kit/http-kit) for serving the web application,
  - (optional) [orientdb](https://github.com/orientechnologies/orientdb) offering an embedded Java document noSql database,
  - [clj-http](https://github.com/dakrone/clj-http) for inter-server communication,
  - [cljs-http](https://github.com/r0man/cljs-http) to communicate with the RESTFull backend services.

In particular, the use of Stuart Sierra's component library allows an external observer to easily find and understand the inter-module dependencies.


## Modularization as a goal

*Cubanostack* is developed around the idea of being fully modularized and expandable without needing to change the source code of the core library or a third-party library.
In particular, any modules features is made to be easily overidable.

At first sight, this make the code much verbose as it forces to declare all the "public" features a module is using / providing. After some usage, you will certainly see that this
practice as a (mutch appreciated) kind of self-documenting process, which is very usefull when you are using other developer's module or when other developers will use your module.

Technically, the core of the modularization is the _Wrapper_ and the _Bus_ notion.

### Bus

The _Bus_ is a shared component that allow all others component to communicate between them.
Basically, it is managing named _Wrappers_, receiving messages from component, and passing them to the corresponding _Wrapper_ queue.

### Wrappers

_Wrappers_ are a stack of functions that will be applied to a payload in order to generate a response.

_Wrappers_ can be registred in the _WrapperManager_ component in order to be receiving messages from the _Bus_.


At the base, a _Wrapper_ is something (technicaly a Protocol instanciation) that:
  - do something (a fn) _before_ the main process (like changing the _payload_),
  - then apply the main process to the _payload_ building a _response_,
  - then do something (an other fn) _after_ the main process (like changing the _response_).

As an example:

```clojure
(deftype TimeCommandWrapper
 Wrapper
 (before [this payload]
  (merge payload
         {::start-time (System/nanoTime)}))

 (after [this response payload]
  (assoc response
         :elapsed (- (System/nanoTime)
                     (::start-time payload)))))
```

When TimeCommandWrapper is called on a _Wrapper_ stack, it will decorate the payload with a ::start-time info, then let the "normal" / other _Wrapper_ in the stack apply their
before and after processes, and then decorate the response with an :elapsed info giving the time the global process took for computation.

### Overrides

What is important is that any Wrapper (called A) can be encapsulated in another Wrapper (called B), allowing any of the following scenarii:
    - decorate the payload with info from Wrapper B but letting the normal Wrapper A process to occur on a modified payload,
    - letting the normal Wrapper A process to occure but decorating it's response with info from Wrapper B,
    - shortcuting Wrapper A process, replacing it with the Wrapper B process 

All of this without changing Wrapper A source code.

Here are some applicable scenarii:
  - a Wrapper that fixes another Wrapper bug(s) without changing it's source code (for legal/licence reasons for example),
  - a Wrapper that adds a new feature to another Wrapper without changing it's source code:
    - adding some log to a existing process,
    - modifying a e-commerce checkout process (keeping the core checkout process behind the scene [one step checkout]),
  - a Wrapper that decorates the output of another Wrapper, like adding some info in a HTML footer / menu,
  - a Wrapper that replaces another Wrapper process, like using OrientDb instead of MongoDb.


### Modules

By using Stuart's component, and registring _Wrappers_ in the _WrapperManager_, we are able to fully control the Module installation / start / stop / deinstallation processes.


## Installation

   [![Clojar link](https://clojars.org/cubane/stack/latest-version.svg)](https://clojars.org/cubane/stack)


## Usage

As a basic illustration, you should use the *Cubane* lein template that will mainly use *Cubanostack* as its main library, creating the needed project-base file you will need.
```sh
lein new cubane <your project name> --snapshot -- --http-kit --orient-db
```

## TODO

This library is still in heavy development phase, in particular the following features are planned to be released (no release dates / development order is not fixed):
  - user registration / password handling (using mail via postal),
  - backend settings dashboard,
  - backend modules de/activation dashboard,
  - persitent frontend modules de/activation,
  - wrapper order / dependency (using [Stuart's dependency](https://github.com/stuartsierra/dependency) ?)
  - cassendra module,
  - AMQP module,
    - bridging with the _Bus_,
  - redis module,
  - embedded SQL module (h2 ?),
  - module installation from JAR,
    - automatic ClojureScript rebuilding when new module get de/activated (as figwheel is able to),
  - externaly modularize non-core modules:
    - orientdb
    - sente
    - cublono-quiescent


## License

Copyright © 2016 [Sylvain Tedoldi](https://github.com/s-ted)

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
