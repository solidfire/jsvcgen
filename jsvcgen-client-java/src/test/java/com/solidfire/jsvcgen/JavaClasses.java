package com.solidfire.jsvcgen;

import com.solidfire.jsvcgen.javautil.Optional;

import java.io.Serializable;

public class JavaClasses {

    public static class A {
    }
    @SuppressWarnings("serial")
    public static class AA implements Serializable {
    }
    @SuppressWarnings("serial")
    public static class B implements Serializable {
        public Optional<String> optional;

        public B(Optional<String> optional) {
            this.optional = optional;
        }
    }
    @SuppressWarnings("serial")
    public static class C implements Serializable {
        public B b;

        public C(B b) {
            this.b = b;
        }
    }
    @SuppressWarnings("serial")
    public static class Foo implements Serializable {

        public String bar;
        public Optional<String> baz;

        public Foo(String bar, Optional<String> baz) {
            this.bar = bar;
            this.baz = baz;
        }
    }
    @SuppressWarnings("serial")
    public static class FooFoo implements Serializable {

        public Foo bar;
        public Optional<Foo> baz;

        public FooFoo(Foo bar, Optional<Foo> baz) {
            this.bar = bar;
            this.baz = baz;
        }
    }
}