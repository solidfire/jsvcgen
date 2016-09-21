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
import com.solidfire.jsvcgen.model.TypeDefinition
import org.scalatest.{Matchers, WordSpec}


class JavaCodeFormatterTests extends WordSpec with Matchers {

  val formatter = new JavaCodeFormatter( buildOptions.copy( namespace = "testNameSpace" ), buildServiceDefinition )


  "buildExtends" should {
    "Generate types with no inheritance or interface" in {
      val typeDefinition = new TypeDefinition(name = "SubType")
      val classDefinition = formatter.buildExtends(typeDefinition, buildOptions)
      classDefinition should be("")
    }

    "Generate types with inheritance" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"))
      val classDefinition = formatter.buildExtends(typeDefinition, buildOptions)
      classDefinition should be("extends SuperType")
    }
  }

  "addImplements" should {
    "Generate types with one interface" in {
      val typeDefinition = new TypeDefinition(name = "SubType", implements = Some(List("IImplement")))
      val classDefinition = formatter.addImplements(typeDefinition)
      classDefinition should be (", IImplement")
    }

    "Generate types with inheritance and one interface" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"), implements = Some(List("IImplement")))
      val classDefinition = formatter.addImplements(typeDefinition)
      classDefinition should be (", IImplement")
    }

    "Generate types with inheritance and two interfaces" in {
      val typeDefinition = new TypeDefinition(name = "SubType", inherits = Some("SuperType"), implements = Some(List("IImplement", "IInterface")))
      val classDefinition = formatter.addImplements(typeDefinition)
      classDefinition should be (", IImplement, IInterface")
    }
  }

  "getTypeName(String)" should {
    "map wrapper types when primitives are not allowed" in {
      formatter.getTypeName( "boolean" ) should be( "Boolean" )
      formatter.getTypeName( "integer" ) should be( "Long" )
      formatter.getTypeName( "long" ) should be( "Long" )
      formatter.getTypeName( "number" ) should be( "Double" )
      formatter.getTypeName( "float" ) should be( "Double" )
    }

    "map string, regardless of case, to String" in {
      formatter.getTypeName( "string" ) should be( "String" )
      formatter.getTypeName( "String" ) should be( "String" )
    }

    "map object, regardless of case, to Map<String, Object>" in {
      formatter.getTypeName( "object" ) should be( "java.util.Map<String, Object>" )
    }

    "map types to base alias types" in {
      formatter.getTypeName( "yesOrNo" ) should be( "Boolean" )
      formatter.getTypeName( "uint64" ) should be( "Long" )
      formatter.getTypeName( "uint32" ) should be( "Long" )
      formatter.getTypeName( "size_t" ) should be( "Long" )
      formatter.getTypeName( "ID" ) should be( "Long" )
      formatter.getTypeName( "bigID" ) should be( "Long" )
      formatter.getTypeName( "smallID" ) should be( "Long" )
      formatter.getTypeName( "ratio" ) should be( "Double" )
      formatter.getTypeName( "precision" ) should be( "Double" )
      formatter.getTypeName( "name" ) should be( "String" )
      formatter.getTypeName( "UUID" ) should be( "java.util.UUID" )
    }

    "map optional types to alias types even if canBePrimitive" in {
      formatter.getTypeName( "maybeYesOrNo" ) should be( "Boolean" )
      formatter.getTypeName( "someID" ) should be( "Long" )
      formatter.getTypeName( "someBigID" ) should be( "Long" )
      formatter.getTypeName( "someSmallID" ) should be( "Long" )
      formatter.getTypeName( "someRatio" ) should be( "Double" )
      formatter.getTypeName( "somePrecision" ) should be( "Double" )
    }

    "map non-aliased, non-primitive types to capitalized case" in {
      formatter.getTypeName( "myType" ) should be( "MyType" )
    }
  }

  "getTypeName(TypeDefinition)" should {

    "map optional types to alias wrapper types" in {
      formatter.getTypeName( maybeYesOrNo ) should be( "Boolean" )
      formatter.getTypeName( someID ) should be( "Long" )
      formatter.getTypeName( someBigID ) should be( "Long" )
      formatter.getTypeName( someSmallID ) should be( "Long" )
      formatter.getTypeName( someRatio ) should be( "Double" )
      formatter.getTypeName( somePrecision ) should be( "Double" )
    }
  }

  "getTypeName(TypeUse)" should {
    "map array types to alias primitive array types" in {
      formatter.getTypeName( yesOrNo.alias.get.copy( isArray = true ) ) should be( "Boolean[]" )
      formatter.getTypeName( uint64.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( uint32.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( size_t.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( ID.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( bigID.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( smallID.alias.get.copy( isArray = true ) ) should be( "Long[]" )
      formatter.getTypeName( ratio.alias.get.copy( isArray = true ) ) should be( "Double[]" )
      formatter.getTypeName( precision.alias.get.copy( isArray = true ) ) should be( "Double[]" )
    }

    "map optional types to alias wrapper types" in {
      formatter.getTypeName( maybeYesOrNo.alias.get ) should be( "Optional<Boolean>" )
      formatter.getTypeName( someID.alias.get ) should be( "Optional<Long>" )
      formatter.getTypeName( someBigID.alias.get ) should be( "Optional<Long>" )
      formatter.getTypeName( someSmallID.alias.get ) should be( "Optional<Long>" )
      formatter.getTypeName( someRatio.alias.get ) should be( "Optional<Double>" )
      formatter.getTypeName( somePrecision.alias.get ) should be( "Optional<Double>" )
    }

    "map optional array types to alias wrapper array types" in {
      formatter.getTypeName( maybeYesOrNo.alias.get.copy( isArray = true ) ) should be( "Optional<Boolean[]>" )
      formatter.getTypeName( someID.alias.get.copy( isArray = true ) ) should be( "Optional<Long[]>" )
      formatter.getTypeName( someBigID.alias.get.copy( isArray = true ) ) should be( "Optional<Long[]>" )
      formatter.getTypeName( someSmallID.alias.get.copy( isArray = true ) ) should be( "Optional<Long[]>" )
      formatter.getTypeName( someRatio.alias.get.copy( isArray = true ) ) should be( "Optional<Double[]>" )
      formatter.getTypeName( somePrecision.alias.get.copy( isArray = true ) ) should be( "Optional<Double[]>" )
    }
  }
}
