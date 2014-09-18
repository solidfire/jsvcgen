/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
**/
package com.gockelhut.jsvcgen;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.Scanner;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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
    protected void prepareConnection(HttpURLConnection connection) {
    	try {
    		connection.setRequestMethod("POST");
    	} catch (ProtocolException pe) {
    		// Checked exceptions are SUCH a GREAT IDEA!
    		throw new RuntimeException("Your HTTP connection does not support \"POST\"", pe);
    	}
    	connection.addRequestProperty("Accept", "application/json");
    }
    
    /**
     * Send the request to the remote system.
     * 
     * @param requestParams
     * @param requestParamsClass
     * @param resultParamsClass
     * @return
     */
    protected <TResult, TRequest> TResult sendRequest(String method,
    		                                          TRequest requestParams,
                                                      Class<TRequest> requestParamsClass,
                                                      Class<TResult> resultParamsClass) {
    	try {
	    	byte[] encodedRequest = encodeRequest(method, requestParams, requestParamsClass);
	    	HttpURLConnection connection = (HttpURLConnection)endpoint.openConnection();
	    	prepareConnection(connection);
	    	
	    	OutputStream out = connection.getOutputStream();
	    	try {
	    		out.write(encodedRequest);
	    		out.flush();
	    	} finally {
	    		out.close();
	    	}
	    	
	    	InputStream input = connection.getResponseCode() == 200 ? connection.getInputStream() : connection.getErrorStream();
	    	try {
	    		return gsonBuilder.create().fromJson(new InputStreamReader(input), resultParamsClass);
	    	} finally {
	    		input.close();
	    	}
    	} catch (IOException ioe) {
    		throw getExceptionForIOException(ioe);
    	}
    }
    
    protected <TRequest> byte[] encodeRequest(String method, TRequest requestParams, Class<TRequest> requestParamsClass) {
    	Gson gson = gsonBuilder.create();
    	JsonObject requestObj = new JsonObject();
    	requestObj.addProperty("id", 1);
    	requestObj.addProperty("method", method);
    	requestObj.addProperty("json-rpc", "2.0");
    	requestObj.add("params", gson.toJsonTree(requestParams, requestParamsClass));
    	return gson.toJson(requestObj).getBytes();
    }
    
    protected <TResponse> TResponse decodeResponse(InputStream responseInput, Class<TResponse> responseClass) {
    	JsonObject responseObj = new JsonParser().parse(new InputStreamReader(responseInput)).getAsJsonObject();
    	if (responseObj.has("error")) {
    		throw extractErrorResponse(responseObj);
    	} else {
    		return gsonBuilder.create().fromJson(responseObj.get("result"), responseClass);
    	}
    }
    
    protected RuntimeException extractErrorResponse(JsonObject obj) {
    	String msg = obj.get("error").getAsJsonObject().get("message").getAsString();
    	return new JsonRpcException(msg);
    }
    
    protected RuntimeException getExceptionForIOException(IOException ioe) {
    	return new JsonRpcException(ioe);
    }
    
    protected String convertStreamToString(InputStream input) {
    	Scanner s = new Scanner(input);
    	try {
    		s.useDelimiter("\\A");
    		return s.hasNext() ? s.next() : "";
    	} finally {
    		s.close();
    	}
    }
}
