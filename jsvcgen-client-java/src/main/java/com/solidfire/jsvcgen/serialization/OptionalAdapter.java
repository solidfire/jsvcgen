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
package com.solidfire.jsvcgen.serialization;

import com.google.gson.*;
import com.solidfire.jsvcgen.javautil.Optional;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Handles serializing and deserializing Optional objects.
 */
public class OptionalAdapter implements JsonSerializer<Optional<?>>, JsonDeserializer<Optional<?>> {

    /**
     * Gets the Class that this adapter serializes.
     *
     * @return The serializable Class.
     */
    @SuppressWarnings("rawtypes")
    static public Class serializingClass() {
        return Optional.class;
    }

    /**
     * Serializes an Optional object.
     *
     * @param optional the value to serialize.
     * @param type     the type of the source object.
     * @param context  Context used for serialization.
     * @return A tree of JsonElements corresponding to the serialized form of optional.
     */
    public JsonElement serialize(Optional<?> optional, Type type, JsonSerializationContext context) {
        if (!optional.isPresent()) {
            // Nothing to serialize
            return null;
        }

        // Defer serialization to handler for type contained in Optional
        return context.serialize(optional.get());
    }

    /**
     * Deserializes an Optional object.
     *
     * @param json    the element to deserialize.
     * @param typeOfT type of the expected return value.
     * @param context Context used for deserialization.
     * @return An Optional object containing an object of type typeOfT.
     */
    public Optional<?> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
        if (json.isJsonNull()) {
            return Optional.empty();
        }

        ParameterizedType pType = (ParameterizedType) typeOfT;
        Type genericType = pType.getActualTypeArguments()[0];

        // Special handling for string, "" will return Optional.empty()
        if (genericType.equals(String.class)) {
            String str = json.getAsString();
            if (str.length() == 0) {
                return Optional.empty();
            }
            return Optional.of(str);
        }

        // Defer deserialization to handler for type contained in Optional
        return Optional.of(context.deserialize(json, genericType));
    }
}
