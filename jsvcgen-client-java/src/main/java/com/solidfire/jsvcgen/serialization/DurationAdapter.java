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

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.joda.time.Duration;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles converting a Duration object to and from JSON.
 */
public class DurationAdapter extends TypeAdapter<Duration> {

    /**
     * Gets the Class that this adapter serializes.
     *
     * @return The serializable Class.
     */
    public static Class<Duration> serializingClass() {
        return Duration.class;
    }

    private static final Pattern inputRegex = Pattern.compile("(-?)(?:(?:([0-9]+):)?([0-9]+):)?([0-9]+)(?:\\.([0-9]+))?");

    /**
     * Reads a Duration object.
     *
     * @param reader the JSON reader to read from.
     * @return The Duration object that was read.
     * @throws IOException if the Duration can not be parsed.
     */
    @Override
    public Duration read(JsonReader reader) throws IOException {
        String input = reader.nextString();
        Matcher m = inputRegex.matcher(input);
        while (m.matches()) {
            long hours = m.group(2) != null ? Long.parseLong(m.group(2)) : 0;
            long minutes = m.group(3) != null ? Long.parseLong(m.group(3)) : 0;
            long seconds = m.group(4) != null ? Long.parseLong(m.group(4)) : 0;

            // Milliseconds are trickier because if the input string is not properly formatted (e.g. ".1") we need to
            // right pad the input string with zeros -- ".1" is really 100 milliseconds. Likewise, if the string is
            // larger than 3 characters, we need to cut off extra characters.
            String millisecondsSource = m.group(5) != null ? m.group(5) : "000";
            while (millisecondsSource.length() < 3)
                millisecondsSource += '0';
            millisecondsSource = millisecondsSource.substring(0, 3);
            long millis = Long.parseLong(millisecondsSource);

            long millisTotal = ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis;

            // Negate final output if '-' was on original input
            if (m.group(1).equals("-"))
                millisTotal = -millisTotal;

            return Duration.millis(millisTotal);
        }
        throw new RuntimeException("Could not extract duration from \"" + input + "\"");
    }

    /**
     * Writes a Duration object.
     *
     * @param writer the JSON writer to write to.
     * @param value  the Duration object to write.
     * @throws IOException
     */
    @Override
    public void write(JsonWriter writer, Duration value) throws IOException {
        long total = value.getMillis();
        String neg = total < 0 ? "-" : "";
        if (total < 0)
            total = -total;
        long millis = total % 1000;
        total /= 1000;
        long seconds = total % 60;
        total /= 60;
        long minutes = total % 60;
        total /= 60;
        long hours = total;
        writer.value(String.format("%s%02d:%02d:%02d.%03d000", neg, hours, minutes, seconds, millis));
    }

}
