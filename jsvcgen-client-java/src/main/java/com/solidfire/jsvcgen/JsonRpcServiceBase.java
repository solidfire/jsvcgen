/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/
package com.solidfire.jsvcgen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

/**
 * A helpful base class for issuing requests to a JSON-RPC web service.
 */
public class JsonRpcServiceBase {
    private final URL endpoint;
    private final GsonBuilder gsonBuilder = new GsonBuilder();

    protected JsonRpcServiceBase(URL endpoint) {
        this.endpoint = endpoint;
    }

    public URL getEndpoint() {
        return endpoint;
    }

    protected final GsonBuilder getGsonBuilder() {
        return gsonBuilder;
    }

    /**
     * When issuing a request, this function is called before the request is dispatched to allow a derived class to
     * override various properties. By default, the only action is to set the request property <tt>Accept</tt> to be
     * <tt>application/json</tt>.
     *
     * @param connection The connection before getting dispatched. The actual type is dependent on the URL scheme. If
     *                   getEndpoint is <tt>https</tt>, this will be an HttpsURLConnection.
     */
    protected static void prepareConnection(HttpURLConnection connection) {
        try {
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
        } catch (ProtocolException pe) {
            throw new RuntimeException("Your HTTP connection does not support \"POST\"", pe);
        }
        connection.addRequestProperty("Accept", "application/json");
    }

    /**
     * Send the request to the remote system.
     *
     * @param method the name of the service method
     * @param requestParams the request object
     * @param requestParamsClass class representing the request parameters
     * @param resultParamsClass class representing the result parameters
     * @param <TRequest> class representing the request parameters
     * @param <TResult> class representing the result parameters
     * @return the results
     */
    protected <TResult, TRequest> TResult sendRequest(String method,
                                                      TRequest requestParams,
                                                      Class<? extends TRequest> requestParamsClass,
                                                      Class<? extends TResult> resultParamsClass) {

        if(null == method || method.trim().isEmpty()) throw new IllegalArgumentException("method is null or empty");
        if(null == requestParams) throw new IllegalArgumentException("request params is null");
        if(null == requestParamsClass) throw new IllegalArgumentException("request params class is null");
        if(null == resultParamsClass) throw new IllegalArgumentException("result params class is null");

        try {
            byte[] encodedRequest = encodeRequest(method, requestParams, requestParamsClass);
            HttpURLConnection connection = (HttpURLConnection) endpoint.openConnection();
            prepareConnection(connection);

            try (OutputStream out = connection.getOutputStream()) {
                out.write(encodedRequest);
                out.flush();
            }

            try (InputStream input = getConnectionStream(connection)) {
                final TResult tResult = gsonBuilder.create().fromJson(new InputStreamReader(input), resultParamsClass);
                return tResult;
            }

        } catch (IOException ioe) {
            throw getExceptionForIOException(ioe);
        }
    }

    protected <TRequest> byte[] encodeRequest(String method, TRequest requestParams, Class<? extends TRequest> requestParamsClass) {

        if(null == method || method.trim().isEmpty()) throw new IllegalArgumentException("method is null or empty");
        if(null == requestParams) throw new IllegalArgumentException("request params is null");
        if(null == requestParamsClass) throw new IllegalArgumentException("request params class is null");

        Gson gson = gsonBuilder.create();
        JsonObject requestObj = new JsonObject();
        requestObj.addProperty("id", 1);
        requestObj.addProperty("method", method);
        requestObj.addProperty("json-rpc", "2.0");
        requestObj.add("params", gson.toJsonTree(requestParams, requestParamsClass));
        return gson.toJson(requestObj).getBytes();
    }

    protected <TResponse> TResponse decodeResponse(InputStream responseInput, Class<? extends TResponse> resultParamsClass) {

        if(null == responseInput) throw new IllegalArgumentException("request params is null");
        if(null == resultParamsClass) throw new IllegalArgumentException("result params class is null");

        JsonObject responseObj = new JsonParser().parse(new InputStreamReader(responseInput)).getAsJsonObject();
        if (responseObj.has("error")) {
            throw extractErrorResponse(responseObj);
        } else {
            return gsonBuilder.create().fromJson(responseObj.get("result"), resultParamsClass);
        }
    }

    protected static InputStream getConnectionStream(HttpURLConnection connection) throws IOException {
        return connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
    }

    protected static RuntimeException extractErrorResponse(JsonObject obj) {
        String msg = obj.get("error").getAsJsonObject().get("message").getAsString();
        return new JsonRpcException(msg);
    }

    protected static RuntimeException getExceptionForIOException(IOException ioe) {
        if(null == ioe)
            return new JsonRpcException();
        return new JsonRpcException(ioe);
    }

    protected static String convertStreamToString(InputStream input) {
        try (Scanner s = new Scanner(input)) {
            s.useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }
    }
}
