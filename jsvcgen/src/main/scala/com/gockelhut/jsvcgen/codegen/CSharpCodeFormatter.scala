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

class CSharpCodeFormatter(options: CliConfig, serviceDefintion: ServiceDefinition) {
  private val directTypeNames = options.typenameMapping.getOrElse(
                                                          Map(
                                                              "boolean" -> "bool",
                                                              "integer" -> "long",
                                                              "number"  -> "double",
                                                              "string"  -> "string",
                                                              "float"   -> "double",
                                                              "object"  -> "Newtonsoft.Json.Linq.JObject"
                                                          )
                                                       )
  private val structTypes = options.valueTypes.getOrElse(List("bool", "long", "double")).toSet
  
  def getTypeName(src: String): String = {
    directTypeNames.get(src)
     .getOrElse(Util.camelCase(src, true))
  }
  def getTypeName(src: TypeDefinition): String = getTypeName(src.name)
  def getTypeName(src: TypeUse): String = src match {
    case TypeUse(name, false, false) => getTypeName(name)
    case TypeUse(name, false, true)  => getTypeName(name) + (if (structTypes.contains(getTypeName(name))) "?" else "")
    case TypeUse(name, true,  _)     => "List<" + getTypeName(name) + ">"
  }
  
  def getResultType(src: Option[ReturnInfo]): String = src match {
    case Some(src) => "Task<" + getTypeName(src.returnType) + ">"
    case None      => "Task"
  }
  
  def getMethodName(src: String): String = Util.camelCase(src, true)
  def getMethodName(src: Method): String = getMethodName(src.name)
  
  def getPropertyName(src: String): String    = Util.camelCase(src, true)
  def getPropertyName(src: Member): String    = getPropertyName(src.name)
  def getPropertyName(src: Parameter): String = getPropertyName(src.name)
  
  def getParamName(src: String): String    = Util.camelCase(src, false)
  def getParamName(src: Member): String    = getParamName(src.name)
  def getParamName(src: Parameter): String = getParamName(src.name)
  
  def getParameterList(params: List[Parameter]): String =
    Util.stringJoin((for (param <- params) yield getTypeName(param.parameterType) + " " + getParamName(param)), ", ")
  
  def getParameterUseList(params: List[Parameter]): String =
    Util.stringJoin((for (param <- params)
                       yield ("@" + param.name + " = " + getParamName(param))),
                    ", ")
  
  def getCodeDocumentation(lines: List[String], linePrefix: String): String = {
    val sb = new StringBuilder
    sb.append(linePrefix)
    sb.append("/// <summary>\n")
    for (line <- lines) {
      sb.append(linePrefix)
      sb.append("/// ")
      sb.append(line)
      sb.append('\n')
    }
    sb.append(linePrefix)
    sb.append("/// </summary>")
    sb.result
  }
  def getCodeDocumentation(doc: Documentation, linePrefix: String): String = getCodeDocumentation(doc.lines, linePrefix)
  
  def ordered(types: List[TypeDefinition]): List[TypeDefinition] = {
    val (aliases, fulls) = types.partition(x => !x.alias.isEmpty)
    aliases ++ fulls
  }
}
