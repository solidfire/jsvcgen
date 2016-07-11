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

package com.solidfire.jsvcgen.reflection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;


public class ReflectionUtils {
    private static final Logger log = LoggerFactory.getLogger(ReflectionUtils.class);

    @SuppressWarnings("unchecked")
    public static <T> T safeGet(final Field field, final Object obj) {
        try {

            final boolean accessibility = field.isAccessible();
            field.setAccessible(true);

            final T value = (T) field.get(obj);

            field.setAccessible(accessibility);

            return value;

        } catch (IllegalAccessException e) {
            log.debug("Error getting field {} value from {}", field, obj.getClass().getSimpleName());
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static <T> void safeSet(final Field field, final Object obj, final T value) {
        try {

            final boolean accessibility = field.isAccessible();
            field.setAccessible(true);

            field.set(obj, value);

            field.setAccessible(accessibility);

        } catch (IllegalAccessException e) {
            log.debug("Error setting field {} in {} with value {}", new Object[] {field, obj.getClass().getSimpleName(), value});
        }
    }
}
