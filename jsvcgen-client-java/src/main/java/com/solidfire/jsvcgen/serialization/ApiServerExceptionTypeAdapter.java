package com.solidfire.jsvcgen.serialization;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.solidfire.jsvcgen.client.ApiServerException;
import com.solidfire.jsvcgen.javautil.Optional;

import java.io.IOException;

/**
 * The Type Adapter responsible for transforming JSON error objects into an ApiServierException
 */
public class ApiServerExceptionTypeAdapter extends TypeAdapter<ApiServerException> {

    /**
     * Gets the Class that this adapter serializes.
     *
     * @return The serializable Class.
     */
    @SuppressWarnings("rawtypes")
    static public Class serializingClass() {
        return ApiServerException.class;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(JsonWriter out, ApiServerException value) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiServerException read(JsonReader in) throws IOException {

        String name = null;
        String code = null;
        String message = null;

        in.beginObject();
        while (in.hasNext()) {
            if (in.peek() != JsonToken.NAME) {
                in.skipValue();
                continue;
            }

            switch (in.nextName()) {
                case "name":
                    name = in.nextString();
                    break;
                case "code":
                    code = in.nextString();
                    break;
                case "message":
                    message = in.nextString();
                    break;
            }
        }
        in.endObject();

        return new ApiServerException(name, code, message);
    }
}
