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

import javax.net.ssl.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

/**
 * A request dispatcher that completely disables security checking.
 */
public class HttpsRequestDispatcherWithoutSecurity extends HttpsRequestDispatcher {

    private final HostnameVerifier hostnameAlwaysOkay = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    };
    private final TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                @Override
                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                @Override
                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
    };

    /**
     * Create a dispatcher using no authentication.
     */
    public HttpsRequestDispatcherWithoutSecurity(URL endpoint) {
        super(endpoint);
    }

    /**
     * Create a dispatcher using HTTP basic authentication using the supplied username and password.
     *
     * @param endpoint the hostname or IP address of the connection
     * @param username username credential
     * @param password password credential
     */
    public HttpsRequestDispatcherWithoutSecurity(URL endpoint, String username, String password) {
        super(endpoint, username, password);
    }

    /**
     * Constructs a security disabled HTTPS POST connection,
     *
     * @param connection the https connection to a Element OS cluster
     */
    @Override
    protected void prepareConnection(HttpsURLConnection connection) {
        super.prepareConnection(connection);
        disableAllSecurity(connection);
    }

    private void disableAllSecurity(HttpsURLConnection connection) {
        // Disable hostname verification
        connection.setHostnameVerifier(hostnameAlwaysOkay);

        // Disable SSL certificate checking
        final SSLContext sc;
        try {
            sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Couldn't get SSL from SSLContext", nsae);
        } catch (KeyManagementException kme) {
            throw new RuntimeException("Failed to initialize SSLContext", kme);
        }
        connection.setSSLSocketFactory(sc.getSocketFactory());
    }

}
