/**
  * Licensed to the Apache Software Foundation (ASF) under one
  * or more contributor license agreements.  See the NOTICE file
  * distributed with this work for additional information
  * regarding copyright ownership.  The ASF licenses this file
  * to you under the Apache License, Version 2.0 (the
  * "License"); you may not use this file except in compliance
  * with the License.  You may obtain a copy of the License at
  *
  * http://www.apache.org/licenses/LICENSE-2.0
  *
  * Unless required by applicable law or agreed to in writing,
  * software distributed under the License is distributed on an
  * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  * KIND, either express or implied.  See the License for the
  * specific language governing permissions and limitations
  * under the License.
  **/
package com.solidfire.jsvcgen.loader

import com.solidfire.jsvcgen.model.ReleaseProcess
import org.json4s.JsonAST._
import org.json4s.jackson.JsonMethods
import org.scalatest._

import scala.io.Source

private object Descriptions {
  def getDescriptionJValue( name: String ): JValue = {
    val contents = Source.fromURL( getClass.getResource( "/descriptions/jsvcgen-description/" + name ) ).mkString
    JsonMethods.parse( contents )
  }
}

class JsvcgenDescriptionTest extends WordSpec with Matchers {
  "load" should {
    "work for \"simple.json\"" in {
      val desc = JsvcgenDescription.load( Descriptions.getDescriptionJValue( "simple.json" ), List( ReleaseProcess.PUBLIC ) )
      desc.types.exists( t => t.name == "FooPortInfoResult" ) should be( true )
    }
  }
}
