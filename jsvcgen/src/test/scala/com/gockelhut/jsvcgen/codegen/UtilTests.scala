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
package com.gockelhut.jsvcgen.codegen

import org.scalatest._

class UtilSpec
    extends FlatSpec
    with Matchers {
  "camelCase" should "format underscored with firstUpper=false" in {
    Util.camelCase("some_method", false) should be ("someMethod")
  }
  
  "camelCase" should "format underscored with firstUpper=true" in {
    Util.camelCase("some_method", true) should be ("SomeMethod")
  }
  
  "camelCase" should "format force first letter lower if firstUpper=false" in {
    Util.camelCase("SomeMethod", false) should be ("someMethod")
  }
  
  "camelCase" should "leave a CamelCased string unchanged if firstUpper=true" in {
    Util.camelCase("SomeMethod", true) should be ("SomeMethod")
  }
}
