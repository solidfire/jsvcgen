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

class JavaCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {
  // Get all the types that are just aliases for other types. This is used in getTypeName because Java somehow still
  // does not have type aliases.
  protected val typeAliases: Map[String, TypeUse] =
    (for (typ <- serviceDefintion.types;
          alias <- typ.alias
    ) yield (typ.name, alias)).toMap

  private val directTypeNames = options.typenameMapping.getOrElse(
    Map(
      "boolean" -> "Boolean",
      "integer" -> "Long",
      "number" -> "Double",
      "float" -> "Double",
      "string" -> "String",
      "object" -> "java.util.Map<String, Object>"
    )
  )

  def getTypeName( src: String ): String = {
      directTypeNames.get( src )
        .orElse( typeAliases.get( src ).map( ( alias: TypeUse ) => getTypeName( alias.typeName ) ) )
        .getOrElse( Util.camelCase( src, firstUpper = true ) )
  }

  def getTypeName( src: TypeDefinition ): String = getTypeName( src.name )

  def getTypeName( src: TypeUse ): String = src match {
    case TypeUse( name, false, isOptional, None ) => {
      if (isOptional)
        "Optional<" + getTypeName( name ) + ">"
      else
        getTypeName( name )
    }
    case TypeUse( name, true, isOptional, None ) => {
      if (isOptional)
        "Optional<" + getTypeName( name ) + "[]>"
      else
        getTypeName( name ) + "[]"
    }
    case TypeUse( name, false, false, dictType ) if name.toLowerCase == "dictionary" => s"TreeMap<String,${getTypeName(dictType.getOrElse( "Object" ))}>"
    case TypeUse( name, false, isOptional, dictType ) if name.toLowerCase == "object" => {
      if(isOptional)
        s"Optional<java.util.Map<String,${getTypeName(dictType.get)}>>"
      else
        s"java.util.Map<String,${getTypeName(dictType.get)}>"
    }
    case _ => {
      println(src)
      throw new IllegalArgumentException(src.toString)
    }
  }

  def getTypeName( src: Option[ReturnInfo] ): String = src match {
    case Some( info ) => getTypeName( info.returnType )
    case None => "void"
  }

  def buildExtends(typeDefinition: TypeDefinition, options: CliConfig) = {
    typeDefinition.inherits.map{"extends " + _}
      .getOrElse(options.requestBase.map{"extends " + _}
        .getOrElse(""))
  }

  def addImplements(typeDefinition: TypeDefinition) = {
    typeDefinition.implements.map { i => {
      ", " + i.mkString(", ")
    }}.getOrElse("")
  }

  // GSON uses the field names as the JSON object keys
  def getFieldName( src: String ): String = Util.camelCase( src, firstUpper = false )

  def getFieldName( src: Member ): String = getFieldName( src.name )

  def getFieldName( src: Parameter ): String = getFieldName( src.name )

  def getMemberAccessorName( src: String ): String = "get" + Util.camelCase( src, firstUpper = true )

  def getMemberAccessorName( src: Member ): String = getMemberAccessorName( src.name )

  def getMemberMutatorName( src: String ): String = "set" + Util.camelCase( src, firstUpper = true )

  def getMemberMutatorName( src: Member ): String = getMemberMutatorName( src.name )

  def getMethodName( src: String ): String = Util.camelCase( src, firstUpper = false )

  def getMethodName( src: Method ): String = getMethodName( src.name )

  def getConstructors( src: TypeDefinition ): String = {
    val revisions = List( src.since.getOrElse( "7.0" ) ) ++: src.members.flatMap( member => member.since ).distinct.sortWith( ( s1, s2 ) => s1.compareTo( s2 ) < 0 )
    val revisionMembers: Map[String, List[Member]] = revisions.map( ( revision: String ) => revision -> filterMembersByRevisions( revision, src.members ) ).toMap
    val constructors = revisionMembers.map( { case (k, v) => toConstructor( src, k, v ) } ).toList

    Util.stringJoin( constructors, s"""\n""" )
  }

  def toConstructor( src: TypeDefinition, revision: String, revSpecificMembers: List[Member] ): String = {
    val typeName = getTypeName( src )
    val constructorParams = getParameterListForMembers( revSpecificMembers )
    val constructorFieldInitializersList = constructorFieldInitializers( src, revSpecificMembers )

    val sb = new StringBuilder

    sb ++= s"""    /**\n"""

    getClassDocumentation( src ).map( s => sb ++= s"""     * $s\n""" )

    if (src.members.nonEmpty) {
      src.members.filter( m => m.since.getOrElse( "7.0" ).compareTo( revision ) <= 0 ).map( m => sb ++= documentMemberAsParam( m ) )
    }
    sb ++= s"""     * @since $revision\n"""
    sb ++= s"""     **/\n"""

    sb ++= s"""    @Since(\"$revision\")\n    public $typeName($constructorParams) {\n$constructorFieldInitializersList\n    }\n"""

    sb.result
  }

  def documentMemberAsParam( member: Member ): String = {
    val docFirstLine = member.documentation.getOrElse( new Documentation( List( "" ) ) ).lines.head

    s"""     * @param ${Util.camelCase( member.name, false )}${if (member.typeUse.isOptional) " (optional) " else " [required] "}$docFirstLine\n"""
  }

  def constructorFieldInitializers( src: TypeDefinition, revSpecificMembers: List[Member] ): String = {
    val fields = src.members.map( member => member -> getFieldName( member ) ).toMap

    val initializers = fields.map( {
      case (k, v) => v -> (
                          if (revSpecificMembers.contains( k )) {
                            if (k.typeUse.isOptional && k.typeUse.isArray)
                              s"""($v == null) ? Optional.<${getTypeName( k.typeUse.typeName )}[]>empty() : $v;"""
                            else if (k.typeUse.isOptional && !k.typeUse.isArray)
                              s"""($v == null) ? Optional.<${getTypeName( k.typeUse.typeName )}>empty() : $v;"""
                            else
                              s"""$v;"""
                          } else if (k.typeUse.isOptional && k.typeUse.isArray) {
                            s"""Optional.<${getTypeName( k.typeUse.typeName )}[]>empty();"""
                          } else if (k.typeUse.isOptional && !k.typeUse.isArray) {
                            s"""Optional.<${getTypeName( k.typeUse.typeName )}>empty();"""
                          } else {
                            "null;"
                          })
    }).toList

    val initializedFields = initializers.map( { case (v, f) => s"""        this.$v = $f""" } )

    Util.stringJoin( initializedFields, s"""\n""" )
  }

  def filterMembersByRevisions( revision: String, members: List[Member] ): List[Member] = {
    members.filter( p => p.since.isEmpty || p.since.get.compare( revision ) <= 0 )
  }

  def getParameterListForMembers( members: List[Member] ): String =
    Util.stringJoin( for (member <- members) yield getTypeName( member.typeUse ) + " " + getFieldName( member ), ", " )

  def getParameterList( params: List[Parameter], isInterface: Boolean ): String = {
    Util.stringJoin(
      params.map( param => (getTypeName( param.typeUse ), getFieldName( param ), param.since) ).map(
        {
          case (typeName, fieldName, since) =>
            if (since.isDefined && !isInterface)
              s"""@Since(\"${since.get}\") $typeName $fieldName"""
            else
              s"""$typeName $fieldName"""
        } )
      , ", "
    )
  }

  def getParameterUseList( params: List[Parameter] ): String =
    Util.stringJoin( for (param <- params) yield getFieldName( param ), ", " )

  def getClassDocumentation( typeDefinition: TypeDefinition ): List[String] = {
    if (typeDefinition.documentation.isDefined) typeDefinition.documentation.get.lines
    else
      getRequestResultDocumentationLines( typeDefinition )
  }

  def getRequestResultDocumentationLines( typeDefinition: TypeDefinition ): List[String] = {
    if (typeDefinition.name.endsWith( "Request" ))
      List( s"""The Request object for the "${typeDefinition.name.replaceFirst( "Request", "" )}" API Service call.""" )
    else if (typeDefinition.name.endsWith( "Result" ))
      List( s"""The object returned by the "${typeDefinition.name.replaceFirst( "Result", "" )}" API Service call.""" )
    else List( "" )
  }

  def getCodeDocumentation( lines: List[String], linePrefix: String, since: Option[String] ): String = {
    val sb = new StringBuilder
    sb ++= s"""$linePrefix/**\n"""
    for (line <- lines) {
      sb ++= s"""$linePrefix * $line\n"""
    }
    if (since.isDefined) {
      sb ++= s"""$linePrefix * @since ${since.get} \n"""
    }
    sb ++= s"""$linePrefix **/"""

    sb.result
  }

  def concatDocumentationLine( offset: Int, first: String, second: String ) = {
    (first + s"""\n     *""" + (" " * (9 + offset)) + second).trim
  }


  val BlankLine = List( " " )

  def getCodeDocumentation( method: Method, serviceName: String, linePrefix: String, useRequestObject: Boolean ): String = {
    val sb = new StringBuilder

    if (method.documentation.isDefined) {
      val lines =
        if (!useRequestObject) {
          List( s"""Convenience method for ${getMethodName( method )} """ )
        } else {
          method.documentation.get.lines
        }

      val params: List[String] =
        if (!useRequestObject) {
          if (method.params.nonEmpty) {
            BlankLine ++
              method.params
                .filter( p => p.documentation.isDefined )
                .map( p => s"""@param ${p.name} ${p.documentation.get.lines.foldRight( "" )( concatDocumentationLine( p.name.length, _, _ ) )}""" )
          } else Nil
        } else {
          BlankLine ++ List( s"""@param request The request @see com.solidfire.element.api.${getTypeName( method.name )}Request """ )
        }

      val returns: List[String] =
        BlankLine ++ List( s"""@return the response""" ) ++
          (
          if (!useRequestObject) {
            List( s"""@see com.solidfire.element.api.${getTypeName( serviceName )}#${getMethodName( method )}(${getTypeName( method.name )}Request) """ )
          } else Nil
          )

      sb ++= getCodeDocumentation( lines ++ params ++ returns, linePrefix, method.since )
    }

    sb.result
  }

  def renderHashCode( typeDefinition: TypeDefinition ): String = {
    val sb = new StringBuilder

    sb ++= s"""    @Override\n"""
    sb ++= s"""    public int hashCode() {\n"""
    if (typeDefinition.members.isEmpty) {
      sb ++= s"""        return this.getClass().hashCode();\n"""
    } else if (typeDefinition.members.length == 1) {
      val cast = if ("Object".equals( typeDefinition.members.head.typeUse.typeName )) "" else "(Object) "
      sb ++= s"""        return Objects.hash( $cast${typeDefinition.members.map( ( x: Member ) => getFieldName( x ) ).mkString} );\n"""
    } else {
      sb ++= s"""        return Objects.hash( ${typeDefinition.members.map( ( x: Member ) => getFieldName( x ) ).mkString( ", " )} );\n"""
    }
    sb ++= s"""    }\n"""

    return sb.result
  }

  def getServiceMethod( method: Method, serviceName: String, isInterface: Boolean, useRequestObject: Boolean ): String = {
    val sb = new StringBuilder

    if (isInterface && method.documentation.isDefined) {
      sb ++= s"""${getCodeDocumentation( method, serviceName, "    ", useRequestObject: Boolean )}\n"""
    }
    if (!isInterface) {
      sb ++= s"""    @Override\n"""
    }
    if (method.since.isDefined) {
      sb ++= s"""    @Since("${method.since.get}")\n"""
    }
    if (isInterface) {
      sb ++= s"""    """

    } else {
      sb ++= s"""    public """
    }
    sb ++= s"""${getTypeName( method.returnInfo )} ${getMethodName( method )}("""
    if (useRequestObject) {
      sb ++= s"""final ${getTypeName( method.name )}Request request"""
    } else {
      sb ++= s"""${getParameterList( method.params, isInterface )}"""
    }
    sb ++= s""")"""
    if (isInterface) {
      sb ++= s""";\n"""
    } else {
      sb ++= s""" {\n"""
      val hasValueAdaptor = method.returnInfo.get.adaptor.isDefined && method.returnInfo.get.adaptor.get.supports.contains("java")

      val sendRequest =
        if (useRequestObject && hasValueAdaptor) {
          sb ++= s"""        final ${getTypeName( method.returnInfo )} result = this.${getMethodName( method )}( request );\n"""
          sb ++= s"""\n"""
          sb ++= s"""        return ${options.adaptorBase}.${Util.camelCase(method.returnInfo.get.adaptor.get.name, firstUpper = false)}(request, result);\n"""
        } else if (useRequestObject && !hasValueAdaptor) {
          sb ++= s"""        return super.sendRequest( "${method.name}", request, ${getTypeName( method.name )}Request.class, ${getTypeName( method.returnInfo ).split("<")(0)}.class );\n"""
        } else if(!useRequestObject && hasValueAdaptor) {
            sb ++= s"""        final ${getTypeName( method.name )}Request request = new ${getTypeName( method.name )}Request( ${getParameterUseList( method.params )});\n"""
            sb ++= s"""        final ${getTypeName( method.returnInfo )} result = this.${getMethodName( method )}( request );\n"""
            sb ++= s"""\n"""
            sb ++= s"""        return ${options.adaptorBase}.${Util.camelCase(method.returnInfo.get.adaptor.get.name, firstUpper = false)}(request, result);\n"""
        } else if(!useRequestObject && !hasValueAdaptor) {
            sb ++= s"""        return this.${getMethodName( method )}( new ${getTypeName( method.name )}Request( ${getParameterUseList( method.params )}) );\n"""
        }
      sb ++= s"""    }\n"""
    }

    sb.result
  }

  def getRequestBuilder( typeDefinition: TypeDefinition ): String = {
    val sb = new StringBuilder

    sb ++= s"""    public static final Builder builder() {\n"""
    sb ++= s"""        return new Builder();\n"""
    sb ++= s"""    }\n"""
    sb ++= s"""\n"""

    sb ++= s"""    public final Builder asBuilder() {\n"""
    sb ++= s"""        return new Builder().buildFrom(this);\n"""
    sb ++= s"""    }\n"""
    sb ++= s"""\n"""

    sb ++= s"""    public static class Builder {\n"""
    for (member <- typeDefinition.members) {
      sb ++= s"""        private ${getTypeName( member.typeUse )} ${getFieldName( member )};\n"""
    }
    sb ++= s"""\n"""
    sb ++= s"""        private Builder() { }\n"""
    sb ++= s"""\n"""
    sb ++= s"""        public ${typeDefinition.name} build() {\n"""
    sb ++= s"""            return new ${typeDefinition.name} (\n"""
    sb ++= Util.stringJoin( typeDefinition.members.map( member => s"""                         this.${getFieldName( member )}""" ), ",\n" )
    sb ++= s"""            );\n"""
    sb ++= s"""        }\n\n"""

    sb ++= s"""        private ${typeDefinition.name}.Builder buildFrom(final ${typeDefinition.name} req) {\n"""
    sb ++= Util.stringJoin( typeDefinition.members.map( member => s"""            this.${getFieldName( member )} = req.${getFieldName( member )};""" ), "\n" )
    sb ++= s"""\n\n"""
    sb ++= s"""            return this;\n"""
    sb ++= s"""        }\n\n"""

    for (member <- typeDefinition.members) {
      if (member.typeUse.isOptional) {
        val optionalArrayBrackets = if (member.typeUse.isArray) "[]" else ""
        sb ++=
          s"""        public ${typeDefinition.name}.Builder optional${Util.camelCase( member.name, firstUpper = true )}(final ${
            getTypeName( member.typeUse.typeName)}$optionalArrayBrackets ${getFieldName( member )}) {\n"""
        sb ++=
          s"""            this.${getFieldName( member )} = (${getFieldName( member )} == null) ? Optional.<${
            getTypeName( member.typeUse.typeName )}$optionalArrayBrackets>empty() : Optional.of(${getFieldName( member )});\n"""
        sb ++= s"""            return this;\n"""
        sb ++= s"""        }\n\n"""
      } else {
        sb ++= s"""        public ${typeDefinition.name}.Builder ${member.name}(final ${getTypeName( member.typeUse )} ${getFieldName( member )}) {\n"""
        sb ++= s"""            this.${getFieldName( member )} = ${getFieldName( member )};\n"""
        sb ++= s"""            return this;\n"""
        sb ++= s"""        }\n\n"""
      }
    }

    sb ++= s"""    }\n"""

    sb.result
  }
}

