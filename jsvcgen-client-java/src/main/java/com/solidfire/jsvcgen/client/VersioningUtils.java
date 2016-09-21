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

import com.solidfire.jsvcgen.annotation.Since;
import com.solidfire.jsvcgen.javautil.Optional;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Helper methods for determining if the version of a connection is compatible with a constructor, service method, or parameters.
 */
public class VersioningUtils {

    /**
     * Extracts the Element OS version from a URL endpoint
     *
     * @param endpoint an Element OS connection endpoint
     * @return the version of the connection endpoint
     */
    public static String getVersionFromEndpoint(final URL endpoint) {
        for (String section : endpoint.toString().split("/")) {
            if (isVersionString(section))
                return section;
        }
        throw new ApiException("Version missing in endpoint url");
    }

    /**
     * Checks the format of the version.
     * Format: XXX.XX
     *
     * @param possibleVersion a possible Version string
     * @return true if the string matches the version format, otherwise false
     */
    public static boolean isVersionString(final String possibleVersion) {
        return (null != possibleVersion)
                && !possibleVersion.trim().isEmpty()
                && possibleVersion.matches("^\\d{1,3}.\\d{1,2}$");
    }

    /**
     * Confirms a version string is greater than a base version string
     *
     * @param baseVersion    the base version for comparison
     * @param checkedVersion the version of comparison
     * @return true if the checked version is greater than the base version, otherwise false
     */
    public static boolean isVersionGreater(final String baseVersion, final String checkedVersion) {
        return baseVersion.compareTo(checkedVersion) < 0;
    }

    /**
     * Confirms a version string is greater than or equal to a base version string
     *
     * @param baseVersion    the base version for comparison
     * @param checkedVersion the version of comparison
     * @return true if the checked version is greater than  or equal to the base version, otherwise false
     */
    public static boolean isVersionGreaterOrEqual(final String baseVersion, final String checkedVersion) {
        return baseVersion.compareTo(checkedVersion) <= 0;
    }

    /**
     * Confirms a version string is less than a base version string
     *
     * @param baseVersion    the base version for comparison
     * @param checkedVersion the version of comparison
     * @return true if the checked version is less than the base version, otherwise false
     */
    public static boolean isVersionLess(final String baseVersion, final String checkedVersion) {
        return baseVersion.compareTo(checkedVersion) > 0;
    }

    /**
     * Confirms a version string is less than or equal to a base version string
     *
     * @param baseVersion    the base version for comparison
     * @param checkedVersion the version of comparison
     * @return true if the checked version is less than or equal to the base version, otherwise false
     */
    public static boolean isVersionLessOrEqual(final String baseVersion, final String checkedVersion) {
        return baseVersion.compareTo(checkedVersion) >= 0;
    }

    /**
     * Creates a map of version annotated constructors for a given object.
     *
     * @param object an object with one or more constructors annotated with the @Since annotation
     * @return the map of constructor versions (key), and constructors (value)
     */
    @SuppressWarnings("unchecked")
    public static <T> Map<String, Constructor<T>> mapConstructorVersions(final T object) {
        final Map<String, Constructor<T>> versionConstructorMap = new HashMap<>(object.getClass().getConstructors().length);

        for (Constructor<T> constructor : (Constructor<T>[]) object.getClass().getConstructors()) {
            Annotation sinceAnnotation = constructor.getAnnotation(Since.class);
            Since since = (Since) sinceAnnotation;
            if (null != since) {
                versionConstructorMap.put(since.value(), constructor);
            }
        }

        return versionConstructorMap;
    }

    /**
     * Creates a map of version annotated constructors, greater than a base version, for a given object.
     *
     * @param object      an object with one or more constructors annotated with the @Since annotation
     * @param baseVersion the base version for comparison
     * @return the map of constructor versions greater than the base version (key), and constructors (value)
     */
    public static <T> Map<String, Constructor<T>> mapConstructorVersionsLargerThan(final T object, final String baseVersion) {
        final Map<String, Constructor<T>> versionConstructorMap = mapConstructorVersions(object);
        final Map<String, Constructor<T>> filteredVersionConstructorMap = new HashMap<>(versionConstructorMap.size());

        for (final Map.Entry<String, Constructor<T>> entry : versionConstructorMap.entrySet()) {
            if (isVersionGreater(baseVersion, entry.getKey())) {
                filteredVersionConstructorMap.put(entry.getKey(), entry.getValue());
            }
        }

        return filteredVersionConstructorMap;
    }

    /**
     * Creates a list of version annotated methods, greater than a base version, for a given object.
     *
     * @param object      an object with one or more methods annotated with the @Since annotation
     * @param baseVersion the base version for comparison
     * @return the list of methods with versions greater than the base version
     */
    public static <T> List<Method> getMethodsOfGreaterVersion(final T object, final String baseVersion) {
        final List<Method> methodsWithGreaterVersion = new ArrayList<>();
        for (final Method method : object.getClass().getDeclaredMethods()) {
            Annotation sinceAnnotation = method.getAnnotation(Since.class);
            Since since = (Since) sinceAnnotation;
            if (null != since && isVersionGreater(baseVersion, since.value())) {
                methodsWithGreaterVersion.add(method);
            }
        }
        return methodsWithGreaterVersion;
    }

    /**
     * Creates a map of version annotated methods, greater than a base version, for a given object, and the values returned by each method
     *
     * @param object      an object with one or more methods annotated with the @Since annotation
     * @param baseVersion the base version for comparison
     * @return the map of methods with versions greater than the base version (key), and the value returned by each method (value)
     */
    public static <T> Map<Method, Object> getValuesOfMethodsWithGreaterVersion(final T object, final String baseVersion) {
        final Map<Method, Object> valuesOfMethodsWithGreaterVersion = new LinkedHashMap<>();

        for (final Method method : getMethodsOfGreaterVersion(object, baseVersion)) {
            try {
                Object value = method.invoke(object);
                if (value != null && !Optional.empty().equals(value)) {
                    valuesOfMethodsWithGreaterVersion.put(method, value);
                }
            } catch (ReflectiveOperationException e) {
                // Intentionally left blank: Don't care about errors, just want values for the getters
            }
        }

        return valuesOfMethodsWithGreaterVersion;
    }

    /**
     * Creates a list of parameter names from a list of methods
     *
     * @param methods a list of methods
     * @return a list of parameter names
     */
    public static List<String> transformMethodsToParameterNames(final List<Method> methods) {
        final List<String> parameterNames = new ArrayList<>();
        for (Method method : methods) {
            final String methodName = method.getName().replace("get", "");
            parameterNames.add(methodName.substring(0, 1).toLowerCase() + methodName.substring(1));
        }
        return parameterNames;
    }

    /**
     * Creates a concatenated string of parameter names, from a collection of methods
     *
     * @param methods a collection of methods
     * @return a single string containing all corresponding parameter names
     */
    public static String methodParametersToString(final Collection<Method> methods) {
        return Arrays.toString(transformMethodsToParameterNames(new ArrayList<Method>(methods)).toArray());
    }
}
