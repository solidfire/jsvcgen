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

import com.gockelhut.jsvcgen.model._

class JavaCodeFormatter(serviceDefintion: ServiceDefinition) {
  // Get all the types that are just aliases for other types. This is used in getTypeName because Java somehow still
  // does not have type aliases.
  protected val typeAliases: Map[String, TypeUse] = (for (typ <- serviceDefintion.types;
                                                          alias <- typ.alias
                                                         ) yield (typ.name, alias)
                                                    ).toMap
  
  private val directTypeNames = Map(
                                    "boolean" -> "boolean",
                                    "integer" -> "long",
                                    "number"  -> "double",
                                    "string"  -> "String",
                                    "float"   -> "double"
                                   )
  
  def getTypeName(src: String): String = {
    directTypeNames.get(src)
     .orElse(typeAliases.get(src).map(getTypeName))
     .getOrElse(Util.camelCase(src, true))
  }
  def getTypeName(src: TypeDefinition): String = getTypeName(src.name)
  def getTypeName(src: TypeUse): String = src match {
    case TypeUse(name, false, false) => getTypeName(name)
    case TypeUse(name, false, true)  => "Optional<" + getTypeName(name) + ">"
    case TypeUse(name, true,  false) => getTypeName(name) + "[]"
    case TypeUse(name, true,  true)  => "Optional<" + getTypeName(name) + "[]>"
  }
  def getTypeName(src: Option[ReturnInfo]): String = src match {
    case Some(src) => getTypeName(src.returnType)
    case None      => "void"
  }
  
  def getFieldName(src: String): String = Util.camelCase(src, false)
  def getFieldName(src: Member): String    = getFieldName(src.name)
  def getFieldName(src: Parameter): String = getFieldName(src.name)
  
  def getMemberAccessorName(src: String): String = "get" + Util.camelCase(src, true)
  def getMemberAccessorName(src: Member): String = getMemberAccessorName(src.name)
  
  def getMemberMutatorName(src: String): String = "set" + Util.camelCase(src, true)
  def getMemberMutatorName(src: Member): String = getMemberMutatorName(src.name)
  
  def getMethodName(src: String): String = Util.camelCase(src, false)
  def getMethodName(src: Method): String = getMethodName(src.name)
  
  def getParameterListForMembers(params: List[Member]): String =
    Util.stringJoin((for (member <- params) yield getTypeName(member.memberType) + " " + getFieldName(member)), ", ")
  
  def getParameterList(params: List[Parameter]): String =
    Util.stringJoin((for (param <- params) yield getTypeName(param.parameterType) + " " + getFieldName(param)), ", ")
    
  def getParameterUseList(params: List[Parameter]): String =
    Util.stringJoin((for (param <- params) yield getFieldName(param)), ", ")
  
  def getCodeDocumentation(lines: List[String], linePrefix: String): String = {
    val sb = new StringBuilder
    sb.append(linePrefix)
    sb.append("/**\n")
    for (line <- lines) {
      sb.append(linePrefix)
      sb.append(" * ")
      sb.append(line)
      sb.append('\n')
    }
    sb.append(linePrefix)
    sb.append("**/")
    sb.result
  }
  def getCodeDocumentation(doc: Documentation, linePrefix: String): String = getCodeDocumentation(doc.lines, linePrefix)
}
