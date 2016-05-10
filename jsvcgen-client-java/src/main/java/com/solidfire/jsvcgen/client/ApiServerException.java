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

/**
 * An exception returned from the service with specific fields for server exceptions
 * <p/>
 * Exception message is a human-readable description of the error, possibly with additional details.
 */
public class ApiServerException extends ApiException {

    private static final long serialVersionUID = -7903990370607680449L;

    private final String name;
    private final String code;


    public ApiServerException(final String name, final String code, final String message) {
        super(message);
        this.name = name;
        this.code = code;
    }

    /**
     * Unique identifier for the specific error that occurred.
     * Each method returns a documented set of errors, although you should be prepared to handle unrecognized errors as well.
     *
     * @return SolidFire Element OS Error Name
     */
    public String getName() {
        return name;
    }

    /**
     * Numeric code used to identify the error.
     *
     * @return Currently all SolidFire generated errors returns a code of 500.
     */
    public String getCode() {
        return code;
    }
}
