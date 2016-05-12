package com.solidfire.jsvcgen.serialization;

import com.solidfire.jsvcgen.javautil.Optional;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.*;

/**
 * Created by Jason Ryan Womack on 5/11/16.
 */
public class OptionalAdaptorUtils {
    public static <T> T initializeAllNullOptionalFieldsAsEmpty(final T obj) {
        if(!hasOptionalFields(obj))
            return obj;

        for(final Map.Entry<Field, Object> field : getOptionalFields(obj).entrySet()) {
            try {
                final boolean accessibility = field.getKey().isAccessible();
                field.getKey().setAccessible(true);
                final Object fieldValue = field.getKey().get(field.getValue());
                if(fieldValue == null) {
                    field.getKey().set(field.getValue(), Optional.empty());
                }
                field.getKey().setAccessible(accessibility);
            } catch (IllegalAccessException e) {
            }
        }
        return obj;
    }

    public static boolean implementsSerializable(final Object obj) {
        if(obj == null) return false;
        for(final Class<?> clazz : obj.getClass().getInterfaces() ) {
            if(Serializable.class.equals(clazz)) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasOptionalFields(final Object obj) {
        if(obj == null) return false;

        for(final Field field : obj.getClass().getDeclaredFields()) {
            if(field.getType() == Optional.class) {
                return true;
            } else {
                try {
                    if (hasOptionalFields(field.get(obj))) {
                        return true;
                    }
                } catch (IllegalAccessException e) {}
            }
        }
        return false;
    }

    public static Map<Field, Object> getOptionalFields(final Object obj) {
        final Map<Field, Object> fieldMap = new HashMap<>();

        if(obj == null) return fieldMap;

        for(final Field field : obj.getClass().getDeclaredFields()) {
            if(field.getType() == Optional.class) {
                fieldMap.put(field, obj);
            } else {
                try {
                    if (hasOptionalFields(field.get(obj))) {
                        fieldMap.putAll(getOptionalFields(field.get(obj)));
                    }
                } catch (IllegalAccessException e) {}
            }
        }
        return fieldMap;
    }
}
