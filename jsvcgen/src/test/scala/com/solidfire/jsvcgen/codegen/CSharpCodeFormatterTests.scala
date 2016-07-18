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
package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.codegen.TestHelper._
import com.solidfire.jsvcgen.loader.JsvcgenDescription
import com.solidfire.jsvcgen.model.{ReleaseProcess, TypeDefinition}
import org.json4s.JValue
import org.json4s.jackson.JsonMethods
import org.scalatest.{Matchers, WordSpec}

import scala.io.Source


class CSharpCodeFormatterTests extends WordSpec with Matchers {

  val formatter = new CSharpCodeFormatter( buildOptions.copy( namespace = "testNameSpace" ), buildServiceDefinition )
  val generator = new CSharpCodeGenerator( buildOptions )


  "buildTypeClassDefinition" should {
    "Generate types with correct inheritance" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"))
      val classDefinition = formatter.buildTypeClassDefinition(typeDefinition, buildOptions)
      classDefinition should be ("public class SubType : SuperType")
    }
  }

  "groupItemsToFiles" should {
    "not include user defined types" in {
      val serviceDefinition = buildServiceDefinition
      serviceDefinition.types.filter(td => td.userDefined).head.name should be ("userDefined")
      val files = generator.groupItemsToFiles(serviceDefinition)
      val thrown = intercept[NoSuchElementException]{
        files("com/example/userDefined.cs")
      }
    }

    "not include user defined types from parsed json" in {
      val resource = "inherits.json"
      val contents = Source.fromURL( getClass.getResource( "/descriptions/jsvcgen-description/" + resource ) ).mkString
      val parsed: JValue = JsonMethods.parse( contents )
      val definition = JsvcgenDescription.load(parsed, List(ReleaseProcess.PUBLIC))
      definition.types.filter(td => td.userDefined).head.name should be ("UserDefined")
      val files = generator.groupItemsToFiles(definition)
      val thrown = intercept[NoSuchElementException]{
        files("com/example/userDefined.cs")
      }
    }

    "not include types that are aliased from parsed json" in {
      val resource = "inherits.json"
      val contents = Source.fromURL( getClass.getResource( "/descriptions/jsvcgen-description/" + resource ) ).mkString
      val parsed: JValue = JsonMethods.parse( contents )
      val definition = JsvcgenDescription.load(parsed, List(ReleaseProcess.PUBLIC))
      definition.types.filter(td => td.alias.isDefined).head.name should be ("AnAlias")
      val files = generator.groupItemsToFiles(definition)
      val thrown = intercept[NoSuchElementException]{
        files("com/example/anAlias.cs")
      }
    }
  }
}
