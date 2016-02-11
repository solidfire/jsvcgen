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

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer
import scala.reflect.internal.util.StringOps

class PythonCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {
  val WS_4 = " " * 4
  val WS_8 = " " * 8
  val WS_12 = " " * 12
  val WS_16 = " " * 16
  val WS_20 = " " * 20

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
    (for (typ <- serviceDefintion.types;
          alias <- typ.alias
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

  def getParameterList( params: List[Parameter] ): List[String] = {
    "self" ::
      (for (param <- params.sortBy(_.parameterType.isOptional)) yield {
        getVariableName( param.name ) ++ (if (param.parameterType.isOptional) "=OPTIONAL" else "")
      })
  }

  def renderParameterList( params: List[Parameter], linePrefix: String ): String = {
    getParameterList( params ).map( p => s"""\n$linePrefix$p,""" ).mkString
  }

  def getParameterDict( params: List[Parameter] ): List[String] = {
    for (param <- params if !param.parameterType.isOptional)
      yield '"' + param.name + "\": " + getVariableName( param.name )
  }

  def formatParameterDictLine( paramLine: String, linePrefix: String ): String = {
    if (paramLine.endsWith( ":" ))
      s"\n$linePrefix$paramLine"
    else
      s"\n$linePrefix$paramLine,"
  }

  def renderParameterDict( params: List[Parameter], linePrefix: String ): String = {
    val sb = new StringBuilder
    val paramDict = wrapParameterDict( getParameterDict( params ), linePrefix )

    sb ++= "{"

    sb ++= paramDict.map( p => formatParameterDictLine( p, linePrefix ) ).mkString
    if (paramDict.nonEmpty)
      sb ++= s"\n${linePrefix.substring( 0, linePrefix.length - 4 )}}"
    else
      sb ++= "}"

    sb.result
  }

  def wrapParameterDict( params: List[String], linePrefix: String ): List[String] = {
    @tailrec
    def wrapParameterDictImpl( params: List[String], acc: List[String] ): List[String] = {
      params match {
        case Nil => acc
        case x :: xs if x.trim.isEmpty => wrapParameterDictImpl( xs, acc )
        case x :: xs if x.length + linePrefix.length <= 79 => wrapParameterDictImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length + linePrefix.length > 79 => wrapParameterDictImpl( xs, acc ::: lineBeforeLastWhiteSpace( x ) :: s"$WS_4${lineAfterLastWhiteSpace( x ).trim}" :: Nil )
      }
    }
    wrapParameterDictImpl( params, List( ) )
  }

  def getPropertyName( src: String ): String = codegen.Util.underscores( src )

  def getPropertyName( src: Member ): String = getPropertyName( src.name )

  def renderMethods( methods: List[Method] ): String = {
    val sb = new StringBuilder

    sb ++= methods.map( m => renderMethod( m ) ).mkString

    sb.result
  }

  def renderMethod( method: Method ): String = {
    val sb = new StringBuilder

    sb ++= s"""${WS_4}def ${getMethodName( method )}(${renderParameterList( method.params, WS_12 )}):\n"""

    if (method.documentation.isDefined) {
      sb ++= s"""${renderCodeDocumentation( method.documentation.get, method.params, WS_8 )}"""
    }

    sb ++= s"""\n"""
    sb ++= s"""${WS_8}params = ${renderParameterDict( method.params, WS_12 )}\n"""
    sb ++= renderVersionChecks( method )

    sb ++= method.params.filter( p => p.parameterType.isOptional ).map( p => renderOptionalParameter( method, p ) ).mkString

    sb ++= s"""\n"""
    sb ++= renderServiceReturn( method )
    sb ++= s"""\n\n"""

    sb.result
  }


  def renderProperty( member: Member ): String = {
    val sb = new StringBuilder

    val offset = Util.whitespaceOffset( 12 )

    sb ++= s"""    ${getPropertyName( member )} = model.property(\n"""
    sb ++= s"""$offset"${member.name}",\n"""
    sb ++= s"""$offset${getTypeName( member.memberType.typeName )},\n"""
    sb ++= s"""${offset}array=${member.memberType.isArray.toString.capitalize},\n"""
    sb ++= s"""${offset}optional=${member.memberType.isOptional.toString.capitalize},\n"""
    sb ++= s"""${offset}documentation=${member.documentation.map( renderCodeDocumentation( _, List( ), "" ) ).getOrElse( "None" )}\n"""
    sb ++= s"""$offset)\n\n"""

    sb.result
  }

  def getTypeImports( typeDefinition: TypeDefinition, options: Option[CliConfig] ): String = {

    val sb = new StringBuilder

    if (typeDefinition.members.exists( p => "UUID".equalsIgnoreCase( getTypeName( p.memberType.typeName ) ) )) {
      sb ++= s"from uuid import UUID\n"
    }
    val imports =
      for {
        memberTypes <- typeDefinition.members.filterNot( m => isDirectType( m ) ).groupBy( m => m.memberType.typeName )
        if typeDefinition.alias.isEmpty
      } yield memberTypes._1

    if (imports.nonEmpty) {
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

    sb.result.trim
  }

  def renderResultsImports( methods: List[Method] ): String = {
    val sb = new StringBuilder

    val typeNames = methods.flatMap( f => f.returnInfo )
      .map( f => f.returnType )
      .distinct
      .sortBy( f => f.typeName )
      .map( t => getTypeName( t ) )

    if (typeNames.distinct.nonEmpty) {
      sb ++= s"""from . import ${typeNames.mkString( ", " )}"""
    }

    sb.result.trim
  }

  def renderParameterDoc( param: Parameter, linePrefix: String ): String = {
    s"""$linePrefix:param ${getPropertyName( param.name )}: ${param.documentation.getOrElse( EmptyDoc ).lines.mkString( " " )}"""
  }

  def renderParameterTypeDoc( param: Parameter, linePrefix: String ): String = {
    s"""$linePrefix:type ${getPropertyName( param.name )}: {${getTypeName( param.parameterType )}}"""
  }

  def renderCodeDocumentation( doc: Documentation, params: List[Parameter], linePrefix: String ): String = {
    renderCodeDocumentation( doc.lines, params, linePrefix )
  }

  def renderCodeDocumentation( lines: List[String], params: List[Parameter], linePrefix: String ): String = {
    val linesWithPrefix = getCodeDocumentationLines( lines, params, linePrefix )
    val wrappedLines = wrapLines( linesWithPrefix, linePrefix )
    val trimmedWrappedLines = wrappedLines.map( l => StringOps.trimTrailingSpace( l ) + "\n" )

    trimmedWrappedLines.mkString
  }

  def getCodeDocumentationLines( lines: List[String], params: List[Parameter], linePrefix: String ): List[String] = {
    val lb = new ListBuffer[String]

    lb += s"""$linePrefix\"\"\""""
    lines.map( line => lb += s"""$linePrefix$line""" )
    lb += ""
    params.sortBy(_.parameterType.isOptional).map( p => lb ++= List( renderParameterDoc( p, linePrefix ), renderParameterTypeDoc( p, linePrefix ) ) )
    lb += s"""$linePrefix\"\"\""""

    lb.toList
  }

  def wrapLines( lines: List[String], linePrefix: String ): List[String] = {
    @tailrec
    def wrapLinesImpl( lines: List[String], acc: List[String] ): List[String] = {
      lines match {
        case Nil => acc
        case x :: xs if x.trim.isEmpty => wrapLinesImpl( xs, acc )
        case x :: xs if x.length <= 79 || !x.contains( ' ' ) => wrapLinesImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length > 79 => wrapLinesImpl( s"""$linePrefix${lineAfterLastWhiteSpace( x )}""" :: xs, acc ::: s"""${lineBeforeLastWhiteSpace( x )}\n""" :: Nil )
      }
    }
    wrapLinesImpl( lines, List( ) )
  }

  def renderVersionChecks( method: Method ): String = {
    val sb = new StringBuilder

    if (method.params.exists( p => p.since.isDefined )) {
      sb ++= s"""${WS_8}self.check_param_versions(\n"""
      sb ++= s"""$WS_16'${getMethodName( method )}',\n"""
      sb ++= s"""$WS_16(\n"""
      for (param <- method.params) {
        if (param.since.isDefined) {
          sb ++= s"""$WS_20("${getVariableName( param.name )}",\n"""
          sb ++= s"""$WS_20 ${getVariableName( param.name )}, ${param.since.get}, None),\n"""
        }
      }
      sb ++= s"""$WS_16)\n"""
      sb ++= s"""$WS_8)\n"""
    }
    sb.result
  }

  def renderOptionalParameter( method: Method, param: Parameter ): String = {
    val sb = new StringBuilder

    sb ++= s"""${WS_8}if ${getVariableName( param.name )} is not None:\n"""
    val optionalParameterAssignment = s"""${WS_12}params["${param.name}"] = ${getVariableName( param.name )}\n"""
    if (optionalParameterAssignment.length <= 79) {
      sb ++= optionalParameterAssignment
    } else {
      sb ++= s"""${WS_12}params["${param.name}"] = \\\n"""
      sb ++= s"""$WS_16${getVariableName( param.name )}\n"""
    }

    sb.result
  }

  def renderServiceReturn( method: Method ): String = {
    val sb = new StringBuilder
    sb ++= s"""${WS_8}return self._send_request(\n"""
    sb ++= s"""$WS_12'${method.name}',\n"""
    sb ++= s"""$WS_12${getTypeName( method.returnInfo )},\n"""
    sb ++= s"""${WS_12}params,\n"""
    if (method.since.isDefined) {
      sb ++= s"""${WS_12}since=${method.since.get},\n"""
    }
    if (method.deprecated.isDefined) {
      sb ++= s"""${WS_12}deprecated=${method.deprecated.get.version}\n"""
    }
    sb ++= s"""$WS_8)"""

    sb.result
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

  private def lineBeforeLastWhiteSpace( line: String ): String = {
    val lastWS = lastWhitespace( line );
    if (lastWS <= 0 || lastWS > 79) line
    else line.substring( 0, lastWS )
  }

  private def lineAfterLastWhiteSpace( line: String ): String = {
    if (line.trim.isEmpty) ""
    else if (line.indexOf( ' ' ) > 79 || line.indexOf( ' ' ) == -1) line
    else line.substring( lastWhitespace( line ), line.length )
  }

  private def lastWhitespace( line: String ): Int = {
    Util.lastWhitespace( line, 79 )
  }

  def typeFulfilled( typ: TypeDefinition, types: List[TypeDefinition] ): Boolean = typ match {
    case TypeDefinition( _, Some( use ), List( ), _, _, _, _ ) => true
    case TypeDefinition( _, None, members, _, _, _, _ ) => members.forall( mem => types.exists( x => x.name == mem.memberType.typeName ) )
  }
}
