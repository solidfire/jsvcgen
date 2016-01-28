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

import com.solidfire.jsvcgen.codegen
import com.solidfire.jsvcgen.model.Documentation.EmptyDoc
import com.solidfire.jsvcgen.model._

class PythonCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {
  val directTypeNames = options.typenameMapping.getOrElse(
    Map(
      "boolean" -> "bool",
      "integer" -> "int",
      "number" -> "float",
      "string" -> "str",
      "float" -> "float",
      "object" -> "dict"
    ) )

  // Get all the types that are just aliases for other types
  protected val typeAliases: Map[String, TypeUse] =
    (for (typ ← serviceDefintion.types;
          alias ← typ.alias
          ; if !directTypeNames.contains( typ.name ) // Filter out any aliases that are direct types
    ) yield (typ.name, alias)).toMap

  def getTypeName( src: String ): String = {
    directTypeNames.get( src )
      .orElse( typeAliases.get( src ).map( getTypeName ) )
      .getOrElse( Util.camelCase( src, firstUpper = true ) )
  }

  def isDirectType( member: Member ): Boolean = {
    directTypeNames.values.exists( c => c == getTypeName( member.memberType.typeName ) )
  }

  def getTypeName( src: TypeUse ): String = getTypeName( src.typeName )

  def getTypeName( src: Option[ReturnInfo] ): String = src match {
    case Some( info ) => getTypeName( info.returnType )
    case None => "None"
  }

  def getVariableName( src: String ): String = codegen.Util.underscores( src )

  def getMethodName( src: String ): String = codegen.Util.underscores( src )

  def getMethodName( src: Method ): String = getMethodName( src.name )

  def getParameterList( params: List[Parameter] ): String = {
    ("self" :: (for (param ← params) yield {
      getVariableName( param.name ) ++ (if (param.parameterType.isOptional) "=DEFAULT" else "")
    })).mkString( ", " )
  }

  def getParameterDict( params: List[Parameter] ): String = {
    "{" + (for (param ← params if !param.parameterType.isOptional)
      yield '"' + param.name + "\": " + getVariableName( param.name )).mkString( ", " ) + "}"
  }

  def getPropertyName( src: String ): String = codegen.Util.underscores( src )

  def getPropertyName( src: Member ): String = getPropertyName( src.name )

  def renderProperty( member: Member ): String = {
    val sb = new StringBuilder

    val offset = Util.whitespaceOffset( 12 )

    sb ++= s"""    ${getPropertyName( member )} = model.property(\n"""
    sb ++= s"""$offset"${member.name}",\n"""
    sb ++= s"""$offset${getTypeName( member.memberType.typeName )},\n"""
    sb ++= s"""${offset}array=${member.memberType.isArray.toString.capitalize},\n"""
    sb ++= s"""${offset}optional=${member.memberType.isOptional.toString.capitalize},\n"""
    sb ++= s"""${offset}documentation=${member.documentation.map( getCodeDocumentation( _, List( ), "" ) ).getOrElse( "None" )}\n"""
    sb ++= s"""$offset)\n\n"""

    sb.result( )
  }

  def getTypeImports( typeDefinition: TypeDefinition, options: Option[CliConfig] ): String = {

    val sb = new StringBuilder

    if(typeDefinition.members.exists( p => "UUID".equalsIgnoreCase( getTypeName(p.memberType.typeName) ) )) {
      sb ++= s"from uuid import UUID\n"
    }
    val imports =
      for {
        memberTypes <- typeDefinition.members.filterNot( m => isDirectType( m ) ).groupBy( m => m.memberType.typeName )
        if typeDefinition.alias.isEmpty
      } yield memberTypes._1

    if(imports.nonEmpty) {
      options match {
        case Some( option ) => imports.map( i => sb ++= s"""from ${option.namespace}.$i import $i""" )
        case None => sb ++= s"""from . import ${imports.mkString( ", " )}"""
      }
    }

    sb.result( ).trim
  }

  def renderImports( cliConfig: CliConfig, allSettings: Map[String, Any], value: TypeDefinition ): String = {
    val sb = new StringBuilder
    if (cliConfig.headerTypeTemplate.isEmpty) {
      sb ++= s"""#!/usr/bin/python\n"""
      sb ++= s"""# -*- coding: utf-8 -*-\n"""
      sb ++= s"""#\n"""
      sb ++= s"""# DO NOT EDIT THIS CODE BY HAND! It has been generated with jsvcgen.\n"""
      sb ++= s"""#\n"""
      sb ++= s"""from __future__ import unicode_literals\n"""
      sb ++= s"""from __future__ import absolute_import\n"""
      sb ++= s"""from solidfire.common.api import model\n"""
    } else {
      sb ++= Util.layoutTemplate( options.headerTypeTemplate.get, allSettings )
    }
    sb ++= getTypeImports( value, None )

    sb.result( ).trim
  }

  def renderResultsImports( methods: List[Method] ): String = {
    val sb = new StringBuilder

    val typeNames = methods.flatMap( f => f.returnInfo )
      .map( f => f.returnType )
      .distinct
      .sortBy( f => f.typeName )
      .map(t => getTypeName( t ) )

    if(typeNames.distinct.nonEmpty) {
      sb ++= s"""from . import ${typeNames.mkString( ", " )}"""
    }

    sb.result( ).trim
  }

  def getCodeDocumentation( lines: List[String], params: List[Parameter], linePrefix: String ): String = {
    val sb = new StringBuilder
    sb ++= s"""$linePrefix\"\"\""""
    lines.map( line => sb ++= s"""\n$linePrefix$line""" )
    sb ++= "\n"
    params.map( p => sb ++= renderParameterDoc( p, linePrefix ) ++= renderParameterTypeDoc( p, linePrefix ) )
    sb ++= s"""\n$linePrefix\"\"\""""
    sb.result( )
  }

  def renderParameterDoc( param: Parameter, linePrefix: String ) = {
    s"""\n$linePrefix:param ${getPropertyName( param.name )}: ${param.documentation.getOrElse( EmptyDoc ).lines.mkString( " " )}"""
  }

  def renderParameterTypeDoc( param: Parameter, linePrefix: String ) = {
    s"""\n$linePrefix:type ${getPropertyName( param.name )}: ${getTypeName( param.parameterType )}"""
  }

  def getCodeDocumentation( doc: Documentation, params: List[Parameter], linePrefix: String ): String = {
    getCodeDocumentation( doc.lines, params, linePrefix )
  }

  def ordered( types: List[TypeDefinition] ): List[TypeDefinition] = {
    val (nonResult, result) = types.sortBy( x => x.name ).partition( x => !x.name.contains( "Result" ) )
    nonResult ++ result
  }

  private def orderedImpl( unwritten: List[TypeDefinition], unblocked: List[TypeDefinition] ): List[TypeDefinition] = {
    if (unwritten.isEmpty) {
      List( )
    } else {
      val (freed, blocked) = unwritten.partition( x => typeFulfilled( x, unblocked ) )
      if (freed.isEmpty)
        throw new UnsupportedOperationException(
          "Cannot get proper ordering (potential missing type or circular loop)"
            ++ unwritten.mkString( ", " )
        )
      freed ++ orderedImpl( blocked, unblocked ++ freed )
    }
  }

  private def lastWhitespace(line: String): Int = {
    Util.lastWhitespace(line, 79)
  }

  def typeFulfilled( typ: TypeDefinition, types: List[TypeDefinition] ): Boolean = typ match {
    case TypeDefinition( _, Some( use ), List( ), _, _, _, _ ) => true
    case TypeDefinition( _, None, members, _, _, _, _ ) => members.forall( mem => types.exists( x => x.name == mem.memberType.typeName ) )
  }
}
