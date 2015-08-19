jsvcgen
=======

A description format and code generator for [JSON-RPC][JSON-RPC]-like web services.
The input to the tool is a [`jsvcgen` description][JsvcgenDescription] JSON file and the output is an SDK for the
 language of your choosing.

[![Build Status](https://travis-ci.org/solidfire/jsvcgen.svg?branch=develop)](https://travis-ci.org/solidfire/jsvcgen)

Project Goals
-------------

The goal of `jsvcgen` is to define a standard, language-agnostic interface to [JSON-RPC][JSON-RPC]-like APIs which
 allows both humans and computers to discover and understand the capabilities of the service without access to source
 code, documentation or through network traffic inspection.
With a properly-defined interface through the `jsvcgen` description format, a user should be able to interact with a
 remote service with a minimal amount of implementation logic.
Similar to what interfaces have done for lower-level programming, `jsvcgen` removes the guesswork in calling the
 service.

The main use case for `jsvcgen` is to assist you in the generation of a language-specific SDK that does not feel foreign
 to the target language.
Automatic generation of documentation is also

### Comparison to Swagger

The `jsvcgen` might look very familiar to you if you have ever interacted with [Swagger][Swagger-spec].
You might even have noticed the opening paragraph of the "Project Goals" section was ~~stolen~~ *borrowed* directly from
 Swagger's project goals.
This is intentional -- `jsvcgen` aims to be similar to Swagger for JSON-RPC.
However, `jsvcgen` will always be a smaller project with smaller aims, as it will have a smaller user base, because
 JSON-REST is the king of web APIs.

The biggest difference between `jsvcgen` and Swagger is the focus of the project.
While Swagger focuses mostly on discoverability and communication, `jsvcgen` puts the emphasis on SDK development that
 feels native to whatever language output you are aiming for.
Concepts such as language-specific type mappings and member functions are part of the `jsvcgen` spec, but would feel
 quite foreign in Swagger.

### Why not JSON-REST?

Maybe you already have a service that has a [JSON-RPC][JSON-RPC]-like protocol.
Perhaps you do not like representational state transfer or you believe the only HTTP method is `POST`.
I don't know your life.
This tool was written because I did not feel like maintaining consistency across Java, Python and Scala SDKs by hand and
 there were no tools to do it for me.
It is useful for me and I hope it is for you as well.

Code Generation
===============

First, you describe your JSON-RPC web service with a [`jsvcgen` description][JsvcgenDescription] (in this example, see
 `jsvcgen-core/src/test/resources/descriptions/jsvcgen-description/simple.json`).
You run the program with:

    $> ./sbt "jsvcgen/run jsvcgen-core/src/test/resources/descriptions/jsvcgen-description/simple.json \
                          --output demo/gen-src                                                        \
                          --namespace com.example                                                      \
                          --generator java"

This outputs a tree full of `.java` files:

    demo/gen-src
    └── com
        └── example
            ├── CreateUserResponse.java
            ├── Group.java
            ├── User.java
            └── UserService.java

These files contain all the boring implementation bits that will eventually allow your clients to say something like:

    public static void main(String[] args) {
        UserService svc = new UserService();
        for (Group g : svc.listGroups()) {
            System.out.println(g.getName());
        }
    }

The same concept applies to other languages, but that's the general idea of code generation.

License
=======

Copyright (C) 2014 by [Travis Gockel](mailto:travis@gockelhut.com).

Licensed to the Apache Software Foundation (ASF) under one or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information regarding copyright ownership.  The ASF licenses this file to you
under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at

 - [http://www.apache.org/licenses/LICENSE-2.0](http://www.apache.org/licenses/LICENSE-2.0)

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the License for the
specific language governing permissions and limitations under the License.

 [JSON-RPC]: http://json-rpc.org/
 [JsvcgenDescription]: https://github.com/tgockel/jsvcgen/blob/master/doc/JsvcgenDescription.md
 [Swagger-spec]: https://github.com/wordnik/swagger-spec
