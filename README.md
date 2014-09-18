jsvcgen
=======

A code generator for JSON-RPC web services.

[![Build Status](https://travis-ci.org/tgockel/jsvcgen.svg?branch=master)](https://travis-ci.org/tgockel/jsvcgen)

First, you describe your JSON-RPC web service with an extended version of [JSON-WSP][JSON-WSP] (in this example, see
 `test/jsonwsp/simple.json`).
You run the program with:

    $> ./src/jsvcgen/jsvc-generate --namespace=com.example test/jsonwsp/simple.json build/output/gen-src

This outputs a tree full of `.java` files:

    build/output/gen-src
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

Whoa! That is so cool!

 [JSON-WSP]: http://en.wikipedia.org/wiki/JSON-WSP
