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
 *
 */

package com.solidfire.jsvcgen.serialization;

import com.solidfire.jsvcgen.javautil.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static com.solidfire.jsvcgen.reflection.ReflectionUtils.*;

public class OptionalAdaptorUtils {
    private static final Logger log = LoggerFactory.getLogger(OptionalAdaptorUtils.class);

    /**
     * Searches a given Object hierarchy and sets all null Optional fields to Optional.empty()
     * @param obj an Object, Array, Iterable or Map
     * @param <T> the given type of the given Object hierarchy
     * @return the given object with all null Optional fields are initialized to Optional.empty()
     */
    public static <T> T initializeAllNullOptionalFieldsAsEmpty(final T obj) {
        if (!hasOptionalFields(obj))
            return obj;

        for (final Map.Entry<Field, Object> fieldEntry : getOptionalFields(obj).entrySet()) {
            final Field field = fieldEntry.getKey();
            final Object parentObject = fieldEntry.getValue();

            if (safeGet(field, parentObject) == null) {
                safeSet(field, parentObject, Optional.empty());
            }
        }
        return obj;
    }

    /**
     * Determines if any Optional fields exist in a given object hierarchy.
     * @param obj an Object, Array, Iterable or Map
     * @return true if any Optional fields are found, otherwise false.
     */
    public static boolean hasOptionalFields(final Object obj) {
        if (obj == null || obj instanceof String) return false;

        if(obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive()) return false;

        if (obj.getClass().isArray() && !obj.getClass().isPrimitive()) {
            for (final Object anObj : (Object[]) obj) {
                if (hasOptionalFields(anObj)) {
                    return true;
                }
            }
            return false;
        }

        if (obj instanceof Iterable) {
            for (final Object anObj : (Iterable<?>) obj) {
                if (hasOptionalFields(anObj)) {
                    return true;
                }
            }
            return false;
        }

        if (obj instanceof Map) {
            for (final Map.Entry<?, ?> anEntry : ((Map<?, ?>) obj).entrySet()) {
                if (hasOptionalFields(anEntry.getKey()) || hasOptionalFields(anEntry.getValue())) {
                    return true;
                }
            }
            return false;
        }

        for (final Field field : obj.getClass().getDeclaredFields()) {

            // Don't bother searching into primitives or the java namespace
            if (field.getType().isPrimitive() || field.getType().getName().startsWith("java.")) {
                continue;
            }

            if (field.getType() == Optional.class) {
                return true;
            } else {
                if (hasOptionalFields(safeGet(field,obj))) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Retrieves any Optional fields in a given object hierarchy.
     * @param obj an Object, Array, Iterable or Map
     * @return a Map of optional fields and the object containing the field.
     */
    public static Map<Field, Object> getOptionalFields(final Object obj) {
        final Map<Field, Object> fieldMap = new HashMap<>();

        if (obj == null || obj instanceof String) return fieldMap;

        if (obj.getClass().isArray() && !obj.getClass().isPrimitive()) {
            for (final Object anObj : (Object[]) obj) {
                if (hasOptionalFields(anObj)) {
                    fieldMap.putAll(getOptionalFields(anObj));
                }
            }
            return fieldMap;
        }

        if (obj instanceof Iterable) {
            for (final Object anObj : (Iterable<?>) obj) {
                if (hasOptionalFields(anObj)) {
                    fieldMap.putAll(getOptionalFields(anObj));
                }
            }
            return fieldMap;
        }

        if (obj instanceof Map) {
            for (final Map.Entry<?, ?> anEntry : ((Map<?, ?>) obj).entrySet()) {
                if (hasOptionalFields(anEntry.getKey())) {
                    fieldMap.putAll(getOptionalFields(anEntry.getKey()));
                }
                if (hasOptionalFields(anEntry.getValue())) {
                    fieldMap.putAll(getOptionalFields(anEntry.getValue()));
                }
            }
            return fieldMap;
        }

        for (final Field field : obj.getClass().getDeclaredFields()) {
            // Don't bother searching into primitives or the java namespace
            if (field.getType().isPrimitive() || field.getType().getName().startsWith("java.")) {
                continue;
            }

            if (field.getType() == Optional.class) {
                fieldMap.put(field, obj);
            } else {

                final Object value = safeGet(field,obj);
                if (hasOptionalFields(value)) {
                    fieldMap.putAll(getOptionalFields(value));
                }

            }
        }
        return fieldMap;
    }
}
