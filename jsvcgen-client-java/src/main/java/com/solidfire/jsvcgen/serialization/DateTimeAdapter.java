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

import com.solidfire.gson.TypeAdapter;
import com.solidfire.gson.stream.JsonReader;
import com.solidfire.gson.stream.JsonWriter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.IOException;

/**
 * Handles converting a DateTime object to and from JSON.
 */
public class DateTimeAdapter extends TypeAdapter<DateTime> {

    /**
     * Gets the Class that this adapter serializes.
     *
     * @return The serializable Class.
     */
    public static Class<DateTime> serializingClass() {
        return DateTime.class;
    }

    /**
     * Reads a DateTime object.
     *
     * @param reader the JSON reader to read from.
     * @return The DateTime object that was read.
     * @throws IOException if the DateTime object cannot be parsed
     */
    @Override
    public DateTime read(JsonReader reader) throws IOException {
        return DateTime.parse(reader.nextString());
    }

    /**
     * Writes a DateTime object.
     *
     * @param writer the JSON writer to write to.
     * @param value  the DateTime object to write.
     * @throws IOException
     */
    @Override
    public void write(JsonWriter writer, DateTime value) throws IOException {
        if (value == null) {
            writer.nullValue();
        } else {
            DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            writer.value(fmt.print(value.withZone(DateTimeZone.UTC)));
        }
    }
}
