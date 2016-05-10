/*
 * Copyright &copy 2014-2016 NetApp, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.solidfire.jsvcgen.javautil;

import java.util.Map;
import java.util.NoSuchElementException;

/**
 * An implementation of Optional that should be completely equivalent to the Java 8 Optional<T> type.
 *
 * @param <T>
 */
public final class Optional<T> {

    public static final Optional<String> EMPTY_STRING = Optional.empty();
    public static final Optional<String[]> EMPTY_STRING_ARRAY = Optional.empty();
    public static final Optional<Long> EMPTY_LONG = Optional.empty();
    public static final Optional<Long[]> EMPTY_LONG_ARRAY = Optional.empty();
    public static final Optional<Double> EMPTY_DOUBLE = Optional.empty();
    public static final Optional<Double[]> EMPTY_DOUBLE_ARRAY = Optional.empty();
    public static final Optional<Map<String, Object>> EMPTY_MAP = Optional.empty();


    private final T value;

    private Optional() {
        this.value = null;
    }

    private Optional(T value) {
        if (value == null)
            throw new NullPointerException("Cannot create Optional with a null value");

        this.value = value;
    }

    private static final Optional<?> EMPTY = new Optional<>();

    /**
     * Create an empty Optional value.
     * <p/>
     * If you are stuck in Java 1.7, you might have to specify the type manually with Optional.<T>empty() or Optional.empty(Class<T>);
     *
     * @return an empty Optional
     */
    public static <T> Optional<T> empty() {
        @SuppressWarnings("unchecked")
        Optional<T> x = (Optional<T>) EMPTY;
        return x;
    }

    /**
     * Returns an Optional with the specified present non-null value
     *
     * @param value the value to be present, which must be non-null
     * @param <T>   the class of the value
     * @return an Optional with the value present
     */
    public static <T> Optional<T> of(T value) {
        return new Optional<T>(value);
    }

    /**
     * Returns an Optional describing the specified value, if non-null, otherwise returns an empty Optional.
     *
     * @param value the optional value to create
     * @param <T>   the class of the value
     * @return an Optional with a present value if the specified value is non-null, otherwise an empty Optional
     */
    public static <T> Optional<T> ofNullable(T value) {
        if (value == null)
            return empty();
        else
            return of(value);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj instanceof Optional) {
            Optional<?> other = (Optional<?>) (obj);
            if (value == null)
                return other.value == null;
            else if (other.value == null)
                return false;
            else
                return value.equals(other.value);
        } else {
            return false;
        }
    }

    /**
     * If a value is present, and the value matches the given predicate, return an Optional describing the value, otherwise return an empty Optional.
     *
     * @param predicate a predicate to apply to the value, if present
     * @return an Optional describing the value of this Optional if a value is present and the value matches the given predicate, otherwise an empty Optional
     */
    public Optional<T> filter(Predicate<? super T> predicate) {
        if (predicate == null)
            throw new NullPointerException("predicate cannot be null");

        if (isPresent() && predicate.test(get()))
            return this;
        else
            return empty();
    }

    /**
     * Returns true if there is a value present, otherwise false.
     *
     * @return true if there is a value present, otherwise false
     */
    public boolean isPresent() {
        return value != null;
    }

    /**
     * If a value is present, apply the provided Optional-bearing mapping function to it, return that result, otherwise return an empty Optional. This method is similar to map(Function), but the provided mapper is one whose result is already an Optional, and if invoked, flatMap does not wrap it with an additional Optional.
     *
     * @param mapper a mapping function to apply to the value, if present the mapping function
     * @param <U>    The type parameter to the Optional returned by
     * @return the result of applying an Optional-bearing mapping function to the value of this Optional, if a value is present, otherwise an empty Optional
     */
    public <U> Optional<U> flatMap(Function<? super T, Optional<U>> mapper) {
        if (mapper == null)
            throw new NullPointerException("mapper cannot be null");

        if (isPresent())
            return mapper.apply(get());
        else
            return empty();
    }

    /**
     * If a value is present in this Optional, returns the value, otherwise throws NoSuchElementException.
     *
     * @return the non-null value held by this Optional
     */
    public T get() {
        if (value == null)
            throw new NoSuchElementException("Cannot get an Optional without a value");
        else
            return value;
    }

    @Override
    public int hashCode() {
        return isPresent() ? value.hashCode() : 0;
    }

    /**
     * If a value is present, invoke the specified consumer with the value, otherwise do nothing.
     *
     * @param consumer block to be executed if a value is present
     */
    public void ifPresent(Consumer<? super T> consumer) {
        if (consumer == null)
            throw new NullPointerException("consumer cannot be null");

        if (isPresent())
            consumer.accept(value);
    }

    /**
     * If a value is present, apply the provided mapping function to it, and if the result is non-null, return an Optional describing the result. Otherwise return an empty Optional.
     *
     * @param mapper a mapping function to apply to the value, if present
     * @param <U>    The type of the result of the mapping function
     * @return an Optional describing the result of applying a mapping function to the value of this Optional, if a value is present, otherwise an empty Optional
     */
    public <U> Optional<U> map(Function<? super T, ? extends U> mapper) {
        if (mapper == null)
            throw new NullPointerException("mapper cannot be null");

        if (isPresent())
            return new Optional<U>(mapper.apply(get()));
        else
            return empty();
    }

    /**
     * Return the value if present, otherwise return other.
     *
     * @param other the value to be returned if there is no value present, may be null
     * @return the value, if present, otherwise other
     */
    public T orElse(T other) {
        if (isPresent())
            return value;
        else
            return other;
    }

    /**
     * Return the value if present, otherwise invoke other and return the result of that invocation.
     *
     * @param other a Supplier whose result is returned if no value is present
     * @return the value if present otherwise the result of other.get()
     */
    public T orElseGet(Supplier<? extends T> other) {
        if (other == null)
            throw new NullPointerException("other cannot be null");

        if (isPresent())
            return value;
        else
            return other.get();
    }

    /**
     * Return the contained value, if present, otherwise throw an exception to be created by the provided supplier.
     *
     * @param exceptionSupplier The supplier which will return the exception to be thrown
     * @param <X>               Type of the exception to be thrown
     * @return the present value
     * @throws X if there is no value present
     */
    public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (exceptionSupplier == null)
            throw new NullPointerException("exceptionSupplier cannot be null");

        if (isPresent())
            return value;
        else
            throw exceptionSupplier.get();
    }

    @Override
    public String toString() {
        // I wish Java 7 had lambda expressions...
        return map(new Function<T, String>() {
            @Override
            public String apply(T x) {
                return "Some(" + x.toString() + ")";
            }
        }).orElse("None");
    }
}
