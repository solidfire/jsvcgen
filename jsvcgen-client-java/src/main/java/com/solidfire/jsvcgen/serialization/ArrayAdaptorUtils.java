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
import java.util.*;

import static com.solidfire.jsvcgen.reflection.ReflectionUtils.*;

public class ArrayAdaptorUtils {
    private static final Logger log = LoggerFactory.getLogger(OptionalAdaptorUtils.class);

    public static <T> T convertAllArrayListsToArrays(final T obj) {
        if (!hasMapFields(obj))
            return obj;

        convert(getConvertibleMaps(obj));

        return obj;
    }

    private static void convert(Map<Map<String, Object>, Map<String, List>> conversionMap) {
        for(final Map.Entry<Map<String, Object>, Map<String, List>> entry : conversionMap.entrySet()) {
            for(final Map.Entry<String, List> convertibleEntry : entry.getValue().entrySet()) {
                entry.getKey().put(convertibleEntry.getKey(), convertibleEntry.getValue().toArray());
            }
        }
    }

    private static <T> Map<Map<String, Object>, Map<String, List>> getConvertibleMaps(T obj) {
        final Map<Map<String, Object>, Map<String, List>> conversionMap = new HashMap<>();

        for (final Map<String, Object> map : getAllMaps(obj)) {

            conversionMap.put(map, new HashMap<String, List>());

            for (final Map.Entry<String, Object> mapEntry : map.entrySet()) {

                if(mapEntry.getValue() instanceof List)
                   conversionMap.get(map).put(mapEntry.getKey(), (List) mapEntry.getValue());
                }
        }
        return conversionMap;
    }

    /**
     * Determines if any Map fields exist in a given object hierarchy.
     * @param obj an Object, Array, Iterable or Map
     * @return true if any Optional fields are found, otherwise false.
     */
    public static boolean hasMapFields(final Object obj) {
        if (obj == null || obj instanceof String) return false;

        if(obj.getClass().isArray() && obj.getClass().getComponentType().isPrimitive()) return false;

        if(obj instanceof Optional) {
            return ((Optional) obj).isPresent() && hasMapFields(((Optional) obj).get());
        }

        if (obj.getClass().isArray() && !obj.getClass().isPrimitive()) {
            for (final Object anObj : (Object[]) obj) {
                if (hasMapFields(anObj)) {
                    return true;
                }
            }
            return false;
        }

        if (obj instanceof Iterable) {
            for (final Object anObj : (Iterable<?>) obj) {
                if (hasMapFields(anObj)) {
                    return true;
                }
            }
            return false;
        }

        if (obj instanceof Map) {
            return true;
        }

        for (final Field field : obj.getClass().getDeclaredFields()) {

            // Don't bother searching into primitives or the java namespace except java.util
            if (field.getType().isPrimitive() || (field.getType().getName().startsWith("java") && !field.getType().getName().startsWith("java.util")) ) {
                continue;
            }

            if(hasMapFields(safeGet(field, obj))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves any Map fields in a given object hierarchy.
     * @param obj an Object, Array, Iterable or Map
     * @return a Map of optional fields and the object containing the field.
     */
    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> getAllMaps(final Object obj) {
        final List<Map<String, Object>> maps = new ArrayList<>();

        if (obj == null || obj instanceof String) return maps;

        if (obj.getClass().isArray() && !obj.getClass().isPrimitive()) {
            for (final Object anObj : (Object[]) obj) {
                if (hasMapFields(anObj)) {
                    maps.addAll(getAllMaps(anObj));
                }
            }
            return maps;
        }

        if (obj instanceof Map) {
            maps.add((Map<String, Object>) obj);
            for (final Map.Entry<?, ?> anEntry : ((Map<?, ?>) obj).entrySet()) {
                if (hasMapFields(anEntry.getKey())) {
                    maps.addAll(getAllMaps(anEntry.getKey()));
                }

                maps.addAll(getAllMaps(anEntry.getValue()));
            }
            return maps;
        }

        if( obj instanceof Optional && ((Optional) obj).isPresent() ) {
            maps.addAll(getAllMaps(((Optional) obj).get()));
            return maps;
        }

        if (obj instanceof Iterable) {
            for (final Object anObj : (Iterable<?>) obj) {
                if (hasMapFields(anObj)) {
                    maps.addAll(getAllMaps(anObj));
                }
            }
            return maps;
        }

        for (final Field field : obj.getClass().getDeclaredFields()) {
            // Don't bother searching into primitives or the java namespace
            if (field.getType().isPrimitive()) {
                continue;
            }

            if (field.getType().getName().startsWith("java") && !field.getType().getName().startsWith("java.util")) {
                continue;
            }

            if ( safeGet(field,obj) instanceof Map) {
                final Map<String, Object> map = safeGet(field,obj);
                maps.add(map);
            }
        }
        return maps;
    }

}
