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

import com.solidfire.jsvcgen.javautil.Consumer;
import com.solidfire.jsvcgen.javautil.Optional;
import net.iharder.Base64;

import javax.net.ssl.HttpsURLConnection;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

/**
 * A request dispatcher for dispatching JSON-RPC encoded requests to an Element OS cluster.
 */
public class HttpsRequestDispatcher implements RequestDispatcher {

    public static final int DEFAULT_CONNECTION_TIMEOUT = 15000;
    public static final int DEFAULT_READ_TIMEOUT = 60000;

    private final URL endpoint;
    private final Optional<String> authenticationToken;
    private final String endpointVersion;
    private int connectionTimeout;
    private int readTimeout;

    private HttpsRequestDispatcher(URL endpoint, Optional<String> authenticationToken) {
        if (!endpoint.getProtocol().equals("https"))
            throw new IllegalArgumentException("Unsupported endpoint protocol \"" + endpoint.getProtocol() + "\"." + "Only \"https\" is supported.");

        this.endpointVersion = VersioningUtils.getVersionFromEndpoint(endpoint);
        this.endpoint = endpoint;
        this.authenticationToken = authenticationToken;
        this.setTimeoutToDefault();
    }

    /**
     * Create a dispatcher using no authentication.
     */
    public HttpsRequestDispatcher(URL endpoint) {
        this(endpoint, Optional.<String>empty());
    }

    /**
     * Create a dispatcher using HTTP basic authentication using the supplied username and password.
     *
     * @param endpoint the hostname or IP address of the connection
     * @param username username credential
     * @param password password credential
     */
    public HttpsRequestDispatcher(URL endpoint, String username, String password) {
        this(endpoint, Optional.of(createBasicAuthToken(username, password)));
    }

    /**
     * @return the version of the Element OS endpoint used in the connection
     */
    @Override
    public String getVersion() {
        return this.endpointVersion;
    }

    /**
     * Dispatch an encoded request to the system and await some response.
     *
     * Can throw java.net.SocketTimeoutException if the connection or read timeout occurs.
     *
     * @param input The input string to send to the remote server.
     * @return The server's response.
     * @throws IOException if anything went wrong on the connection side of things.
     */
    @Override
    public String dispatchRequest(String input) throws IOException {
        final byte[] encodedRequest = input.getBytes();
        final HttpsURLConnection connection = (HttpsURLConnection) endpoint.openConnection();
        prepareConnection(connection);

        try (OutputStream out = connection.getOutputStream()) {
            out.write(encodedRequest);
            out.flush();
        }

        // JSON-RPC...we don't actually care about the response code
        final InputStream response = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
        try {
            return decodeResponse(response);
        } finally {
            if (null != response) {
                response.close();
            }
        }
    }

    /**
     * Constructs a HTTPS POST connection
     *
     * @param connection the https connection to a Element OS cluster
     */
    protected void prepareConnection(final HttpsURLConnection connection) {
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
        } catch (ProtocolException pe) {
            // ...because this could happen...ever...
            throw new RuntimeException("Your HTTP connection does not support \"POST\"", pe);
        }

        connection.addRequestProperty("Accept", "application/json");

        connection.setConnectTimeout(this.connectionTimeout);
        connection.setReadTimeout(this.readTimeout);

        authenticationToken.ifPresent(new Consumer<String>() {
            @Override
            public void accept(String token) {
                connection.addRequestProperty("Authorization", token);
            }
        });
    }

    /**
     * Decodes a response stream into a string
     *
     * @param response the response as a stream
     * @return the response as a string
     */
    protected String decodeResponse(InputStream response) throws IOException {
        try (final Scanner s = new Scanner(response)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }

    private static String createBasicAuthToken(String username, String password) {
        return "Basic " + Base64.encodeBytes((username + ":" + password).getBytes());
    }

    @Override
    public void setTimeoutToDefault() {
        this.connectionTimeout = DEFAULT_CONNECTION_TIMEOUT;
        this.readTimeout = DEFAULT_READ_TIMEOUT;
    }

    @Override
    public void setConnectionTimeout(int timeInMilliseconds) {
        this.connectionTimeout = timeInMilliseconds;
    }

    @Override
    public void setReadTimeout(int timeInMilliseconds) {
        this.readTimeout = timeInMilliseconds;
    }
}
