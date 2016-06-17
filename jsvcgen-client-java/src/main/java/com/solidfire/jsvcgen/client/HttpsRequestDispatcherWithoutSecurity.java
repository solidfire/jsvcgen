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

import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import javax.net.ssl.SSLContext;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * A request dispatcher that completely disables security checking.
 */
public class HttpsRequestDispatcherWithoutSecurity extends HttpsRequestDispatcher {

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

    @Override
    public SSLContext getSSLContext() {
        final SSLContext sslContext;
        try {
            SSLContextBuilder builder = SSLContexts.custom();
            builder.loadTrustMaterial(null, new TrustStrategy() {
                @Override
                public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
                    return true;
                }
            });

            sslContext = builder.build();
            return sslContext;
        } catch (NoSuchAlgorithmException nsae) {
            throw new RuntimeException("Couldn't get SSL from SSLContext", nsae);
        } catch (KeyManagementException kme) {
            throw new RuntimeException("Failed to initialize SSLContext", kme);
        } catch (KeyStoreException kse) {
            throw new RuntimeException("Failed to initialize KeyStore", kse);
        }
    }

    @Override
    public HttpClientConnectionManager getConnectionManager() {
        final PoolingHttpClientConnectionManager cm;


        // Disable hostname verification
        SSLConnectionSocketFactory sslcsf = new SSLConnectionSocketFactory(getSSLContext(), SUPPORTED_TLS_PROTOCOLS, null, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        cm = new PoolingHttpClientConnectionManager(
                RegistryBuilder.<ConnectionSocketFactory>create()
                        .register("https", sslcsf)
                        .build()
        );
        // Increase max total connection to 200
        cm.setMaxTotal(200);
        // Increase default max connection per route to 20
        cm.setDefaultMaxPerRoute(20);

        return cm;
    }
}
