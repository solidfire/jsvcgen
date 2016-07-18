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

import com.solidfire.jsvcgen.loader.JsvcgenDescription.{DocumentationSerializer, MemberSerializer, ParameterSerializer, ReturnInfoSerializer, ServiceDefinitionSerializer, StabilityLevelSerializer, TypeUseSerializer}
import com.solidfire.jsvcgen.model.{Adaptor, ReleaseProcess, ServiceDefinition}
import org.json4s.DefaultFormats
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

  implicit def formats = DefaultFormats +
    StabilityLevelSerializer +
    new DocumentationSerializer() +
    new MemberSerializer() +
    new ParameterSerializer() +
    new ReturnInfoSerializer() +
    new ServiceDefinitionSerializer() +
    new TypeUseSerializer()

  val simpleJson = Descriptions.getDescriptionJValue("simple.json")
  val simpleService = simpleJson.extract[ServiceDefinition]

  "load" should {
    "load expected types for \"simple.json\"" in {
      val desc = JsvcgenDescription.load(Descriptions.getDescriptionJValue("simple.json"), List(ReleaseProcess.PUBLIC))
      desc.types.exists(t => t.name == "FooPortInfoResult") should be(true)
    }

    "load expected types for \"adaptors.json\"" in {
      val desc = JsvcgenDescription.load(Descriptions.getDescriptionJValue("adaptors.json"), List(ReleaseProcess.PUBLIC))
      val adaptor: Option[Adaptor] = desc.methods.filter(m => m.name == "createUser").head.returnInfo.get.adaptor
      adaptor.nonEmpty should be (true)
      adaptor.get.name should be ("CreateUserAdaptor")
      adaptor.get.supports.size should be (2)
      adaptor.get.supports.head should be ("csharp")
      adaptor.get.supports.tail.head should be ("java")
    }

    "load expected type and properties for \"inherits.json\"" in {
      val desc = JsvcgenDescription.load(Descriptions.getDescriptionJValue("inherits.json"), List(ReleaseProcess.PUBLIC))
      desc.types.exists(t => t.name == "SubType") should be(true)
      val subtpe = desc.types.filter(t => t.name == "SubType").head
      subtpe.inherits.get should be ("SuperType")
      subtpe.userDefined should be (false)

      val userdefined = desc.types.filter(t => t.name == "UserDefined").head
      userdefined.userDefined should be (true)

    }

  }

  "getTypesWithinType" should {

    val userJson = Descriptions.getDescriptionJValue("user.json")
    val userService = userJson.extract[ServiceDefinition]

    "return a list of all types under User" in {
      val allTypes = JsvcgenDescription.getTypeAndSubTypes(userService, "User")
      allTypes.contains("UserID") should be (true)
      allTypes.contains("User") should be (true)
    }

    "return a list of all types under CreateUserResponse" in {
      val allTypes = JsvcgenDescription.getTypeAndSubTypes(userService, "CreateUserResponse")
      allTypes.contains("UserID") should be (true)
      allTypes.contains("CreateUserResponse") should be (true)
    }
  }

  "discoverOrdinality" should {
    "return a List of MethodType objects with ordinality populated" in {
      val typeOrdinals = JsvcgenDescription.discoverOrdinality(simpleService)
      typeOrdinals.find(t => t.name == "FooPortList").get.lowestOrdinal should be(1)
      typeOrdinals.find(t => t.name == "FooPortInfo").get.lowestOrdinal should be(1)
      typeOrdinals.find(t => t.name == "Group").get.lowestOrdinal should be(1)
      typeOrdinals.find(t => t.name == "CreateGroupResponse").get.lowestOrdinal should be(2)
      typeOrdinals.find(t => t.name == "ListFooPortsResult").get.lowestOrdinal should be(3)
      typeOrdinals.find(t => t.name == "User").get.lowestOrdinal should be(1)
      typeOrdinals.find(t => t.name == "UserID").get.lowestOrdinal should be(1)
    }
  }

  "filterMethodsToRelease" should {
    "only include methods and types for the stability levels requested" in {
      val filteredService = JsvcgenDescription.filterMethodsToRelease(simpleService, List(ReleaseProcess.INTERNAL, ReleaseProcess.INCUBATE))
      // Types that should NOT be generated because they are PUBLIC
      filteredService.types.exists(t => t.name == "User") should be (false)
      filteredService.types.exists(t => t.name == "Group") should be (false)

      // Types that belong to PUBLIC but are included because they are aliases
      filteredService.types.exists(t => t.name == "UserID") should be (true)
      filteredService.types.exists(t => t.name == "GroupID") should be (true)
      filteredService.types.exists(t => t.name == "AsyncResultID") should be (true)

      // Types that should be generated because they are INTERNAL or INCUBATE
      filteredService.types.exists(t => t.name == "CreateUserResponse") should be (true)
      filteredService.types.exists(t => t.name == "CreateGroupResponse") should be (true)

      // Methods that should NOT be generated because they are PUBLIC
      filteredService.methods.exists(t => t.name == "listGroups") should be (false)
      filteredService.methods.exists(t => t.name == "ListFooPortInfo") should be (false)
      filteredService.methods.exists(t => t.name == "IamAPublicMethod") should be (false)

      // Methods that should be generated because they are INTERNAL or INCUBATE
      filteredService.methods.exists(t => t.name == "ListFooPorts") should be (true)
      filteredService.methods.exists(t => t.name == "listUsers") should be (true)
      filteredService.methods.exists(t => t.name == "createGroup") should be (true)
      filteredService.methods.exists(t => t.name == "createUser") should be (true)
    }
    "should not have intersecting types or methods between stability levels" in {
      val publicService = JsvcgenDescription.filterMethodsToRelease(simpleService, List(ReleaseProcess.PUBLIC))
      publicService.types.size should be (9)
      publicService.methods.size should be (3)

      val incubateService = JsvcgenDescription.filterMethodsToRelease(simpleService, List(ReleaseProcess.INCUBATE))
      incubateService.types.size should be (4)
      incubateService.methods.size should be (1)

      val internalService = JsvcgenDescription.filterMethodsToRelease(simpleService, List(ReleaseProcess.INTERNAL))
      internalService.types.size should be (5)
      internalService.methods.size should be (3)

      val publicInternalMethodsIntersect = publicService.methods.intersect(internalService.methods)
      val publicIncubateMethodsIntersect = publicService.methods.intersect(incubateService.methods)
      val incubateInternalMethodsIntersect = incubateService.methods.intersect(internalService.methods)

      val publicInternalTypesIntersect = publicService.types.intersect(internalService.types)
      val publicIncubateTypesIntersect = publicService.types.intersect(incubateService.types)
      val incubateInternalTypesIntersect = incubateService.types.intersect(internalService.types)

      publicInternalMethodsIntersect.isEmpty should be (true)
      publicIncubateMethodsIntersect.isEmpty should be (true)
      incubateInternalMethodsIntersect.isEmpty should be (true)
      publicInternalTypesIntersect.isEmpty should be (false)
      publicIncubateTypesIntersect.isEmpty should be (false)
      incubateInternalTypesIntersect.isEmpty should be (false)
    }
  }

}