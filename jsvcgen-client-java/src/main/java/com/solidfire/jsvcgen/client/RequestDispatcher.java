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
package com.solidfire.jsvcgen.client;

import java.io.IOException;

/**
 * Represents a mechanism for dispatching requests to some server.
 */
public interface RequestDispatcher extends Versionable {
    /**
     * Dispatch an encoded request to the system and await some response.
     *
     * @param input The input string to send to the remote server.
     * @return The server's response.
     * @throws IOException if anything went wrong on the connection side of things.
     */
    String dispatchRequest(String input) throws IOException;


    /**
     * Sets the time to attempt to connect to a cluster.
     *
     * Default: 15 seconds
     *
     * @param timeInMilliseconds an <code>int</code> that specifies the timeout
     * value to be used in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     */
    void setConnectionTimeout(int timeInMilliseconds);

    /**
     * Sets the time to attempt to read data from a request to a cluster.
     *
     * Default: 60 seconds
     *
     * @param timeInMilliseconds an <code>int</code> that specifies the timeout
     * value to be used in milliseconds
     * @throws IllegalArgumentException if the timeout parameter is negative
     */
    void setReadTimeout(int timeInMilliseconds);

    /**
     * Sets the time to attempt to connect to a cluster and the time
     * to attempt to read data from a request to a cluster to the default settings.
     *
     * Connection Default: 15 seconds
     * Read Default: 60 seconds
     *
     */
    void setTimeoutToDefault();
}
