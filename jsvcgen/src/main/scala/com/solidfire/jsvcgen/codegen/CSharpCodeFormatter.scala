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

import com.solidfire.jsvcgen.model._

class CSharpCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {

  private val directTypeNames = options.typenameMapping.getOrElse(
    Map(
      "boolean" → "bool",
      "integer" → "Int64",
      "number" → "double",
      "string" → "string",
      "float" → "double",
      "object" → "Newtonsoft.Json.Linq.JObject",
      "uint64" → "UInt64"
    )
  )
  private val structTypes = options.valueTypes.getOrElse(List("bool", "long", "double")).toSet

  // Get all the types that are just aliases for other types
  protected val typeAliases: Map[String, TypeUse] =
    (for (typ ← serviceDefintion.types;
          alias ← typ.alias
          ; if !directTypeNames.contains(typ.name) // Filter out any aliases that are direct types
    ) yield (typ.name, alias)).toMap

  def getTypeName(src: String): String = {
    directTypeNames.get(src)
      .orElse(typeAliases.get(src).map(getTypeName))
      .getOrElse(Util.camelCase(src, firstUpper = true))
  }

  def getTypeDefinition(src: TypeUse): Option[TypeDefinition] = serviceDefintion.types.find(t => t.name == src.typeName)

  def getTypeName(src: TypeDefinition): String = getTypeName(src.name)

  def getTypeName(src: TypeUse): String = src match {
    case TypeUse(name, false, false, None) => getTypeName(name)
    case TypeUse(name, false, true, None) => getTypeName(name) +
      (if (structTypes.contains(getTypeName(name))) "?" else "")
    case TypeUse(name, true, false, None) => s"${getTypeName(name)}[]"
    case TypeUse(name, true, true, None) => s"${getTypeName(name)}[]" // Lists in .NET are nullable by default
    case TypeUse(name, false, false, dictType) if name.toLowerCase == "dictionary" => s"Dictionary<string,${dictType.getOrElse("")}>"
  }

  def getResultType(src: Option[ReturnInfo]): String = src match {
    case Some(info) => "Task<" + getTypeName(info.returnType) + ">"
    case None => "Task"
  }

  def getMethodName(src: String): String = Util.camelCase(src, firstUpper = true)

  def getMethodName(src: Method): String = getMethodName(src.name)

  def getPropertyName(src: String): String = Util.camelCase(src, firstUpper = true)

  def getPropertyName(src: Member): String = getPropertyName(src.name)

  def getPropertyName(src: Parameter): String = getPropertyName(src.name)

  def getRequiredParams(params: List[Parameter]): List[Parameter] = {
    params.filterNot(p => p.optional)
  }

  def buildMethod(method: Method): String = {
    getRequestObjMethod(method, isInterface = false) + getConvenienceMethod(method, isInterface = false) + getOneRequiredParamMethod(method, isInterface = false)
  }
  
  def buildMember(member: Member): String = {
    val sb = buildDocumentationAndAttributes(member)
    if (member.memberType.isOptional){
      sb.append("\n[Optional]")
    }
    getConverter(member).map(s => sb.append(s"\n$s"))
    sb.append(
    s"""
      |[DataMember(Name="${member.name}")]
      |public ${getTypeName(member.memberType)} ${getPropertyName(member)} { get; set; }
    """.stripMargin
    ).result()
  } 

  def buildInterfaceMethod(method: Method): String = {
    getRequestObjMethod(method, true) + getConvenienceMethod(method, true) + getOneRequiredParamMethod(method, true)
  }

  def getOneRequiredParamMethod(method: Method, isInterface: Boolean): String = {
    val req = getRequiredParams(method.params)
    if (req.size == 1) {
      val param: Parameter = req.head
      if (isInterface) {
        val sb = buildDocumentation(method)
        sb.append(getParamsDocumentation(method.params))
        sb.append(getReturnsDocumentation(method.name))
        sb.append(
        s"""
           |${getResultType(method.returnInfo)} ${getMethodName(method)}Async(${getTypeName(param.parameterType)} ${getParamName(param)});
       """.stripMargin
        ).result()
      }
      else {
        val sb = buildDocumentation(method)
        sb.append(getParamsDocumentation(method.params))
        sb.append(getReturnsDocumentation(method.name))
        sb.append(getAttributes(method))
        sb.append(
        s"""
           |public async ${getResultType(method.returnInfo)} ${getMethodName(method)}Async(${getTypeName(param.parameterType)} ${getParamName(param)})
           |{
           |    var obj = new {${getParamName(req.head)}};
           |    ${getSendRequestWithObj(method)}
           |}
       """.stripMargin
        ).result()
      }
    }
    else ""
  }

  def getConvenienceMethod(method: Method, isInterface: Boolean): String = {
    val req = getRequiredParams(method.params)
    if (req.isEmpty) {
      if (isInterface) {
        val sb = buildDocumentation(method)
        sb.append(getReturnsDocumentation(method.name))
        sb.append(
        s"""
           |${getResultType(method.returnInfo)} ${getMethodName(method)}Async();
       """.stripMargin
        ).result()
      }
      else {
        val sb = buildDocumentation(method)
        sb.append(getReturnsDocumentation(method.name))
        sb.append(getAttributes(method))
        sb.append(
          s"""
             |public async ${getResultType(method.returnInfo)} ${getMethodName(method)}Async()
             |{
             |    ${getSendRequest(method)}
             |}
       """.stripMargin)
          .result()
      }
    }
    else ""
  }


  def getRequestObjMethod(method: Method, isInterface: Boolean): String = {
    if (method.params.nonEmpty) {
      if (isInterface) {
        val sb = buildDocumentation(method)
        sb.append(getReturnsDocumentation(method.name))
        sb.append(
        s"""
           |${getResultType(method.returnInfo)} ${getMethodName(method)}Async(${getMethodName(method)}Request obj);
       """.stripMargin
        ).result()
      }
      else {
        val sb = buildDocumentation(method)
        sb.append(getReturnsDocumentation(method.name))
        sb.append(getAttributes(method))
        sb.append(
          s"""
             |public async ${getResultType(method.returnInfo)} ${getMethodName(method)}Async(${getMethodName(method)}Request obj)
             |{
             |    ${getSendRequestWithObj(method)}
             |}
       """.stripMargin)
          .result()
      }
    }
    else ""
  }

  def buildDocumentationAndAttributes(member: Member): StringBuilder = {
    val sb = buildDocumentation(member)
    getSinceAttribute(member).map(s => sb.append(s"\n$s"))
    getDeprecatedAttribute(member).map(s => sb.append(s"\n$s"))
    sb
  }

  def getAttributes(method: Method) = {
    val sb = new StringBuilder()
    getSinceAttribute(method).map(s => sb.append(s"\n$s"))
    getDeprecatedAttribute(method).map(s => sb.append(s"\n$s"))
    sb.result()
  }

  def buildDocumentation(member: Member): StringBuilder = {
    val sb = new StringBuilder()
    sb.append(getCodeDocumentation(member, ""))
    sb
  }
  
  def buildDocumentation(method: Method): StringBuilder = {
    val sb = new StringBuilder()
    sb.append(getCodeDocumentation(method))
    sb
  }

  def getConverter(member: Member): Option[String] = {
    if (getTypeDefinition(member.memberType).isDefined && getTypeDefinition(member.memberType).get.converter.isDefined) {
      Option(s"[JsonConverter(typeof(${getTypeDefinition(member.memberType).get.converter.get}))]")
    }
    else None
  }

  def getSinceAttribute(attribute: Attribute): Option[String] = {
    attribute.since.map(s"[Since(" + _ + "f)]")
  }

  def getDeprecatedAttribute(attribute: Attribute): Option[String] = {
    attribute.deprecated.map(s"[DeprecatedAfter(" + _.version + "f)]")
  }

  def getSendRequestWithObj(method: Method): String = {
    if (method.returnInfo.isEmpty) {
      "await SendRequestAsync(\"" + method.name + "\", obj);"
    }
    else {
      "return await SendRequestAsync<" + getTypeName(method.returnInfo.get.returnType) + ">(\"" + method.name + "\", obj);"
    }
  }

  def getSendRequest(method: Method): String = {
    if (method.returnInfo.isEmpty) {
      "await SendRequestAsync(\"" + method.name + "\");"
    }
    else {
      "return await SendRequestAsync<" + getTypeName(method.returnInfo.get.returnType) + ">(\"" + method.name + "\");"
    }
  }


  def getParamName(src: String): String = Util.camelCase(src, firstUpper = false)

  def getParamName(src: Member): String = getParamName(src.name)

  def getParamName(src: Parameter): String = getParamName(src.name)

  def getParameterList(params: List[Parameter]): String =
    Util
      .stringJoin(for (param ← params) yield getTypeName(param.parameterType) + " " + getParamName(param), ", ")

  def getParameterUseList(params: List[Parameter]): String =
    Util.stringJoin(for (param ← params) yield "@" + param.name + " = " + getParamName(param), ", ")

  def getDocumentation(maybeDocs: Option[Documentation], name: String, linePrefix: String = "", maybeParams: Option[Seq[Parameter]] = None): String = {
    val sb = new StringBuilder
    sb.append(linePrefix)
      .append("/// <summary>\n")
    maybeDocs.map(d => d.lines.map { line => {
      sb.append(linePrefix)
        .append("/// ")
        .append(line)
        .append("\n")
    }
    }).getOrElse {
      sb.append(linePrefix)
        .append("/// ")
        .append(name)
        .append("\n")
    }
    sb.append(linePrefix)
      .append("/// </summary>")

    sb.result().trim()
  }

  def getParamsDocumentation(params: Seq[Parameter], linePrefix: String = "") = {
    val sb = new StringBuilder
    params.map { param => {
      sb.append("\n")
      sb.append(linePrefix)
      sb.append("/// <param name =\"" + param.name + "\">")
      param.documentation.map {
        doc => doc.lines.map {
          line => {
            sb.append(line) + " "
          }
        }
      }
      sb.append("</param>")
      sb.append(linePrefix)
    }
    }
    sb.result()
  }

  def getReturnsDocumentation(name: String, linePrefix: String = "") = {
    s"\n$linePrefix/// <returns>${name}Result Task</returns>"
  }

  def getCodeDocumentation(member: Member, linePrefix: String): String = {
    getDocumentation(member.documentation, member.name, linePrefix)
  }

  def getCodeDocumentation(typeDef: TypeDefinition, linePrefix: String): String = {
    getDocumentation(typeDef.documentation, typeDef.name, linePrefix)
  }
  
  def getCodeDocumentation(serviceDef: ServiceDefinition, linePrefix: String): String = {
    getDocumentation(serviceDef.documentation, serviceDef.serviceName, linePrefix)
  }

  def getCodeDocumentation(method: Method): String = {
    getDocumentation(method.documentation, method.name, "", Some(method.params))
  }

  def ordered(types: List[TypeDefinition]): List[TypeDefinition] = {
    val (aliases, fulls) = types.partition(x => x.alias.isDefined)
    aliases ++ fulls
  }
}