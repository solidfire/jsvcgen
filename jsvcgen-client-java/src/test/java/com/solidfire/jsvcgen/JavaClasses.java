package com.solidfire.jsvcgen;

import com.solidfire.jsvcgen.javautil.Optional;

public class JavaClasses {

    public static class A {
    }

    public static class B {
        private final Optional<String> optional;

        public B(Optional<String> optional) {
            this.optional = optional;
        }

        public Optional<String> getOptional() {
            return optional;
        }
    }

    public static class C {
        private final B b;

        public C(B b) {
            this.b = b;
        }

        public B getB() {
            return b;
        }
    }

    public static class D {
        private final B[] b;

        public D(B[] b) {
            this.b = b;
        }

        public B[] getB() {
            return b;
        }
    }

    public static class Foo {

        private final String bar;
        private final Optional<String> baz;

        public Foo(String bar, Optional<String> baz) {
            this.bar = bar;
            this.baz = baz;
        }

        public String getBar() {
            return bar;
        }

        public Optional<String> getBaz() {
            return baz;
        }
    }

    public static class FooFoo {

        private final Foo bar;
        private final Optional<Foo> baz;

        public FooFoo(Foo bar, Optional<Foo> baz) {
            this.bar = bar;
            this.baz = baz;
        }

        public Foo getBar() {
            return bar;
        }

        public Optional<Foo> getBaz() {
            return baz;
        }
    }

    public static class FooArray {

        private final Foo[] bar;
        private final Optional<Foo[]> baz;

        public FooArray(Foo[] bar, Optional<Foo[]> baz) {
            this.bar = bar;
            this.baz = baz;
        }

        public Foo[] getBar() {
            return bar;
        }

        public Optional<Foo[]> getBaz() {
            return baz;
        }
    }
}