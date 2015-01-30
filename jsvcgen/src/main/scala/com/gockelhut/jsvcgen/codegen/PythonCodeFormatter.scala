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

class PythonCodeFormatter(options: CliConfig, serviceDefintion: ServiceDefinition) {
  private val directTypeNames = options.typenameMapping.getOrElse(
                                                          Map(
                                                              "boolean" -> "bool",
                                                              "integer" -> "long",
                                                              "number"  -> "float",
                                                              "string"  -> "str",
                                                              "float"   -> "float",
                                                              "object"  -> "dict"
                                                          )
                                                       )
  
  def getTypeName(src: String): String =
    directTypeNames.get(src)
      .getOrElse(Util.camelCase(src, true))
  def getTypeName(src: TypeUse): String = getTypeName(src.typeName)
  def getTypeName(src: Option[ReturnInfo]): String = src match {
    case Some(src) => getTypeName(src.returnType)
    case None      => "None"
  } 
  
  def getVariableName(src: String): String = Util.underscores(src)
  
  def getMethodName(src: String): String = Util.underscores(src)
  def getMethodName(src: Method): String = getMethodName(src.name)
  
  def getParameterList(params: List[Parameter]): String = {
    ("self" :: (for (param <- params) yield getVariableName(param.name))).mkString(", ") 
  }
  
  def getParameterDict(params: List[Parameter]): String = {
    "{" + (for (param <- params) yield ('"' + param.name + "\": " + getVariableName(param.name))).mkString(", ") + "}"
  }
  
  def getPropertyName(src: String): String = Util.underscores(src)
  def getPropertyName(src: Member): String = getPropertyName(src.name)
  
  def getCodeDocumentation(lines: List[String], linePrefix: String): String = {
    val sb = new StringBuilder
    sb.append(linePrefix)
    sb.append("\"\"\"")
    var first = true
    for (line <- lines) {
      if (first)
        first = false
      else
        sb.append(linePrefix)
      sb.append(line)
      sb.append('\n')
    }
    if (sb.last == '\n')
      sb.setLength(sb.length - 1)
    sb.append("\"\"\"")
    sb.result
  }
  def getCodeDocumentation(doc: Documentation, linePrefix: String): String = getCodeDocumentation(doc.lines, linePrefix)
}
