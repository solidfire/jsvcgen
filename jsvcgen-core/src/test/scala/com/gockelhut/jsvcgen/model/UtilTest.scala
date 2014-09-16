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
package com.gockelhut.jsvcgen.model

import org.scalatest._

class UtilSpec
    extends FlatSpec
    with Matchers {
  "validateNamespace" should "validate a simple namespace" in {
    Util.validateNamespace("nodots")
  }
  
  "validateNamespace" should "validate a nested namespace" in {
    Util.validateNamespace("yes.there_is.s0m37h1ng.h3r3")
  }
  
  "validateNamespace" should "throw ValidationException on empty" in {
    a [ValidationException] should be thrownBy {
      Util.validateNamespace("")
    }
  }
  
  "validateNamespace" should "throw ValidationException on numeric" in {
    a [ValidationException] should be thrownBy {
      Util.validateNamespace("987")
    }
  }
  
  "validateNamespace" should "throw ValidationException on numeric prefix" in {
    a [ValidationException] should be thrownBy {
      Util.validateNamespace("987ebin")
    }
  }
}
