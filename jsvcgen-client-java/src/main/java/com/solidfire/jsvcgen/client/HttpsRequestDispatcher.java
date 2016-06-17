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
import org.apache.http.HttpHeaders;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.config.SocketConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

import static java.lang.String.format;

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
    protected HttpClientConnectionManager cm;
    private CloseableHttpClient client;

    private HttpsRequestDispatcher(URL endpoint, Optional<String> authenticationToken) {
        if (!endpoint.getProtocol().equals("https"))
            throw new IllegalArgumentException("Unsupported endpoint protocol \"" + endpoint.getProtocol() + "\"." + "Only \"https\" is supported.");

        this.endpointVersion = VersioningUtils.getVersionFromEndpoint(endpoint);
        this.endpoint = endpoint;
        this.authenticationToken = authenticationToken;
        this.cm = getConnectionManager();
        this.client = null;
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


    public CloseableHttpClient getClient() {
        if (client == null) {
            SocketConfig socketConfig = SocketConfig.custom()
                                                    .setSoKeepAlive(true)
                                                    .setTcpNoDelay(true)
                                                    .build();
            this.client = HttpClients.custom()
                                     .setConnectionManager(cm)
                                     .setDefaultSocketConfig(socketConfig)
                                     .build();
        }
        return this.client;
    }


    @Override
    public SSLContext getSSLContext() {
        try {
            return SSLContext.getDefault();
        } catch (NoSuchAlgorithmException e) {
            throw new ApiException(e);
        }
    }

    @Override
    public HttpClientConnectionManager getConnectionManager() {
        final PoolingHttpClientConnectionManager cm;

        cm = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", new SSLConnectionSocketFactory(getSSLContext(), SUPPORTED_TLS_PROTOCOLS, null, SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER))
                        .build()
        );
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);

        return cm;
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
     * <p/>
     * Can throw java.net.SocketTimeoutException if the connection or read timeout occurs.
     *
     * @param input The input string to send to the remote server.
     * @return The server's response.
     * @throws IOException if anything went wrong on the connection side of things.
     */
    @Override
    public String dispatchRequest(String input) throws IOException {

        CloseableHttpResponse response = null;

        try {
            final HttpPost httpPost = new HttpPost(this.endpoint.toURI());

            prepareConnection(httpPost);
            httpPost.setEntity(new ByteArrayEntity(input.getBytes()));

            response = getClient().execute(httpPost);

            // JSON-RPC...we don't actually care about the response code
            return decodeResponse(response.getEntity().getContent());
        } catch (URISyntaxException e) {
            throw new ApiException(format("Url %s improperly formatted", this.endpoint.toString()), e);
        } finally {
            if (null != response) {
                response.close();
            }
        }
    }

    /**
     * Constructs a HTTPS POST connection
     *
     * @param httpPost the https connection to a Element OS cluster
     */
    protected void prepareConnection(final HttpPost httpPost) {

        authenticationToken.ifPresent(new Consumer<String>() {
            @Override
            public void accept(String token) {
                httpPost.setHeader(HttpHeaders.AUTHORIZATION, token);
            }
        });

        httpPost.addHeader(HttpHeaders.CONTENT_TYPE, ContentType.APPLICATION_JSON.getMimeType());
        httpPost.addHeader(HttpHeaders.ACCEPT, ContentType.APPLICATION_JSON.getMimeType());

        httpPost.setConfig(RequestConfig.custom()
                                        .setConnectionRequestTimeout(15000)
                                        .setConnectTimeout(this.connectionTimeout)
                                        .setSocketTimeout(this.readTimeout)
                                        .build());
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
