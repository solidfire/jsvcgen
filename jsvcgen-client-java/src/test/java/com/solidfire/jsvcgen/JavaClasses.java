package com.solidfire.jsvcgen;

import com.solidfire.jsvcgen.javautil.Optional;

import java.util.HashMap;
import java.util.Map;

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

    public static class E {
        private final FooMap[] map;

        public E(FooMap[] map) {
            this.map = map;
        }

        public FooMap[] getFooMap() {
            return map;
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

    @SuppressWarnings("serial")
    public static class FooMap extends HashMap<String, Object> { }

    public static class ComplexFooMap {
        private final Map<String, String> stringMap;
        private final Map<String, String[]> stringArrayMap;
        private final Map<String, Object> fooMap;

        public ComplexFooMap(Map<String, String> stringMap, Map<String, String[]> stringArrayMap, Map<String, Object> fooMap) {
            this.stringMap = stringMap;
            this.stringArrayMap = stringArrayMap;
            this.fooMap = fooMap;
        }

        public Map<String, String> getStringMap() {
            return stringMap;
        }

        public Map<String, String[]> getStringArrayMap() {
            return stringArrayMap;
        }

        public Map<String, Object> getFooMap() {
            return fooMap;
        }
    }
}