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
import com.solidfire.jsvcgen.loader.JsvcgenDescription.TypeOrdinal
import com.solidfire.jsvcgen.model.Documentation.EmptyDoc
import com.solidfire.jsvcgen.model._

import scala.annotation.tailrec
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.language.postfixOps
import scala.reflect.internal.util.StringOps

class PythonCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {
  val WS_0  = ""
  val WS_1  = " "
  val WS_2  = " " * 2
  val WS_4  = " " * 4
  val WS_8  = " " * 8
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

  def getTypeName( serviceDefinition: ServiceDefinition ): String = {
    if (ReleaseProcess.INTERNAL.equals( serviceDefintion.release ))
      "InternalElement"
    else
      "Element"
  }

  def renderServiceBaseImport( serviceDefinition: ServiceDefinition ): String = {
    if (ReleaseProcess.INTERNAL.equals( serviceDefintion.release ))
      s"""from ${options.namespace.replace( "_internal", "" )} import Element"""
    else
      s"""from ${options.namespace.replace( "_internal", "" )}.common import ${options.serviceBase.getOrElse( "ServiceBase" )}, ApiVersionExceededError, \\\n    ApiVersionUnsupportedError"""
  }

  def renderServiceBase( serviceDefinition: ServiceDefinition ): String = {
    if (ReleaseProcess.INTERNAL.equals( serviceDefintion.release ))
      "Element"
    else
      options.serviceBase.getOrElse( "ServiceBase" )
  }

  def renderServiceBaseConstructor( serviceDefinition: ServiceDefinition ): String = {
    val sb = new StringBuilder
    if (!ReleaseProcess.INTERNAL.equals( serviceDefintion.release )) {
      sb ++= s"""${WS_4}def __init__(self, mvip=None, username=None, password=None,\n"""
      sb ++= s"""$WS_4             api_version=8.0, verify_ssl=True, dispatcher=None):\n"""
      sb ++= s"""$WS_8\"\"\"\n"""
      sb ++= s"""${WS_8}Constructor for initializing a connection to an instance of Element OS\n"""
      sb ++= s"""\n"""
      sb ++= s"""$WS_8:param mvip: the management IP (IP or hostname)\n"""
      sb ++= s"""$WS_8:type mvip: str\n"""
      sb ++= s"""$WS_8:param username: username use to connect to the Element OS instance.\n"""
      sb ++= s"""$WS_8:type username: str\n"""
      sb ++= s"""$WS_8:param password: authentication for username\n"""
      sb ++= s"""$WS_8:type password: str\n"""
      sb ++= s"""$WS_8:param api_version: specific version of Element OS to connect\n"""
      sb ++= s"""$WS_8:type api_version: float or str\n"""
      sb ++= s"""$WS_8:param verify_ssl: disable to avoid ssl connection errors especially\n"""
      sb ++= s"""${WS_12}when using an IP instead of a hostname\n"""
      sb ++= s"""$WS_8:type verify_ssl: bool\n"""
      sb ++= s"""$WS_8:param dispatcher: a prebuilt or custom http dispatcher\n"""
      sb ++= s"""$WS_8:return: a configured and tested instance of Element\n"""
      sb ++= s"""$WS_8\"\"\"\n"""
      sb ++= s"""\n"""
      sb ++= s"""${WS_8}ServiceBase.__init__(self, mvip, username, password, api_version,\n"""
      sb ++= s"""$WS_8                     verify_ssl, dispatcher)\n"""
      sb ++= s"""\n"""
    }
    sb.result
  }

  def getTypeName( src: String ): String = {
    directTypeNames.get( src )
      .orElse( typeAliases.get( src ).map( getTypeName ) )
      .getOrElse( Util.camelCase( src, firstUpper = true ) )
  }

  def isDirectType( member: Member ): Boolean = {
    directTypeNames.values.exists( _ == getTypeName( member.typeUse.typeName ) )
  }

  def getTypeName( src: TypeUse ): String = getTypeName( src.typeName )

  def getTypeName( src: Option[ReturnInfo] ): String = src match {
    case Some( info ) => getTypeName( info.returnType )
    case None => "None"
  }

  def filterUserDefinedTypeNames( types: List[TypeDefinition] ): List[String] = {
    types.filter( td => td.userDefined )
      .sortBy( _.name )
      .map( t => getTypeName( t.name ) )
      .distinct
  }

  def findTypeOrdinality( name: String ): Option[TypeOrdinal] = {
    serviceDefintion.typeOrdinality.find( _.name == name )
  }

  def filterReturnTypeNames( methods: List[Method] ): List[String] = {
    methods.flatMap( _.returnInfo )
      .map( _.returnType )
      .distinct
      .sortBy( _.typeName )
      .map( getTypeName )
      .filterNot( directTypeNames.values.toList.contains( _ ) )
  }

  def getVariableName( src: String ): String = codegen.Util.underscores( src )

  def getMethodName( src: String ): String = codegen.Util.underscores( src )

  def getMethodName( src: Method ): String = getMethodName( src.name )

  def getParameterList( params: List[Parameter] ): List[String] = {
    "self" ::
      (for (param <- params.sortBy( _.typeUse.isOptional )) yield {
        getVariableName( param.name ) ++ (if (param.typeUse.isOptional) "=OPTIONAL" else WS_0)
      })
  }

  def renderParameterList( params: List[Parameter], linePrefix: String ): String = {
    getParameterList( params ).map( p => s"""\n$linePrefix$p,""" ).mkString
  }

  def getParameterDict( params: List[Parameter] ): List[String] = {
    for (param <- params if !param.typeUse.isOptional)
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
        case x :: xs if !x.contains( ' ' ) => wrapParameterDictImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length + linePrefix.length <= 79 => wrapParameterDictImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length + linePrefix.length > 79 && lastWhitespace( x ) > x.length + linePrefix.length =>
          val nextWS = x.indexOf( ' ' )
          wrapParameterDictImpl( xs, acc ::: lineBeforeLastWhiteSpace( x, nextWS ) :: s"$WS_4${lineAfterLastWhiteSpace( x, nextWS ).trim}" :: Nil )

        case x :: xs if x.length + linePrefix.length > 79 => wrapParameterDictImpl( xs, acc ::: lineBeforeLastWhiteSpace( x ) :: s"$WS_4${lineAfterLastWhiteSpace( x ).trim}" :: Nil )
      }
    }
    wrapParameterDictImpl( params, List( ) )
  }

  def getPropertyName( src: String ): String = codegen.Util.underscores( src )

  def getPropertyName( src: Member ): String = getPropertyName( src.name )

  def renderMethods( methods: List[Method] ): String = {
    val sb = new StringBuilder

    sb ++= methods.map( renderMethod ).mkString

    sb.result
  }

  def renderMethod( method: Method ): String = {
    val sb = new StringBuilder

    sb ++= s"""${WS_4}def ${getMethodName( method )}(${renderParameterList( method.params, WS_12 )}):\n"""

    if (method.documentation.isDefined) {
      sb ++= s"""${renderCodeDocumentation( method.documentation.get, method.params, method.returnInfo, WS_8, true )}"""
    }

    sb ++= s"""\n"""
    sb ++= s"""${WS_8}params = ${renderParameterDict( method.params, WS_12 )}\n"""
    sb ++= renderVersionChecks( method )

    sb ++= method.params.filter( _.typeUse.isOptional ).map( renderOptionalParameter( method, _ ) ).mkString

    sb ++= s"""\n"""
    sb ++= renderServiceReturn( method )
    sb ++= s"""\n\n"""

    sb.result
  }


  def getProperty( member: Member ): List[String] = {
    val lb = new ListBuffer[String]

    lb += s"""$WS_4${getPropertyName( member )} = data_model.property(\n"""
    lb += s"""$WS_8"${member.name}","""
    lb += s"""$WS_1${getTypeName( member.typeUse.typeName )},\n"""
    lb += s"""${WS_8}array=${member.typeUse.isArray.toString.capitalize},"""
    lb += s"""${WS_1}optional=${member.typeUse.isOptional.toString.capitalize},\n"""
    lb += s"""${WS_8}documentation=${member.documentation.map( renderCodeDocumentation( _, List( ), None, WS_8, useDocStringQuotes = false ) ).getOrElse( "None" )},\n"""
    lb += s"""${WS_8}dictionaryType=${member.typeUse.dictionaryType.getOrElse("None")}\n"""
    lb += s"""$WS_4)"""

    lb.toList
  }


  def renderProperty( member: Member ): String = {
    getProperty( member ).mkString + s"\n\n"
  }

  def isTypeNameOfHigherOrdinal( typeName: String ): Boolean = {
    val typeOrdinal = findTypeOrdinality( typeName )

    typeOrdinal.isDefined && typeOrdinal.get.lowestOrdinal < options.release.map( _.ordinal ).min
  }

  def getTypeImports( typeDefinitions: List[TypeDefinition] ): List[String] = {

    val lb = new ListBuffer[String]

    val members = typeDefinitions.filter( _.alias.isEmpty ).flatten( _.members ).distinct

    if (members.exists( p => "UUID".equalsIgnoreCase( getTypeName( p.typeUse.typeName ) ) )) {
      lb += s"from uuid import UUID"
    }

    val (higherOrdinalUserTypes, userTypes) = filterUserDefinedTypeNames( typeDefinitions ).partition( isTypeNameOfHigherOrdinal )

    lb ++= higherOrdinalUserTypes.map( p => s"""from ${options.namespace.replace( "_internal", "" )}.models import $p""" )
    lb ++= userTypes.map( p => s"""from ${options.namespace}.custom.models import $p as UserDefined$p""" )

    val imports =
      for {
        memberTypes <- members.filterNot( isDirectType ).groupBy( _.typeUse.typeName )
      } yield memberTypes._1

    val filteredImports = imports.filterNot( typeDefinitions.map( _.name ) contains ).toList.distinct

    val (modelImports, resultsImports) = filteredImports.sorted.partition( !_.endsWith( "Result" ) )

    lb ++= modelImports.map( p =>
      if (isTypeNameOfHigherOrdinal( p )) {
        s"""from ${options.namespace.replace( "_internal", "" )}.models import $p"""
      } else {
        s"""from ${options.namespace}.models import $p"""
      }
    )
    lb ++= resultsImports.map( p => s"""from ${options.namespace}.results import $p""" )

    wrapLinesAt( lb.toList, WS_0, wrapOver = true, 79 ).map( line => if (line.trim.endsWith( "import" )) s"""${line.trim} \\""" else line )
  }

  def renderImports( allSettings: Map[String, Any], value: List[TypeDefinition] ): String = {
    val sb = new StringBuilder
    if (options.headerTypeTemplate.isEmpty) {
      sb ++= s"""#!/usr/bin/python\n"""
      sb ++= s"""# -*- coding: utf-8 -*-\n"""
      sb ++= s"""#\n"""
      sb ++= s"""# Copyright &copy; 2014-2016 NetApp, Inc. All Rights Reserved.\n"""
      sb ++= s"""#\n"""
      sb ++= s"""# DO NOT EDIT THIS CODE BY HAND! It has been generated with jsvcgen.\n"""
      sb ++= s"""#\n"""
      sb ++= s"""from __future__ import unicode_literals\n"""
      sb ++= s"""from __future__ import absolute_import\n"""
      sb ++= s"""from ${options.namespace.replace( "_internal", "" )}.common import model as data_model\n"""
    } else {
      sb ++= Util.layoutTemplate( options.headerTypeTemplate.get, allSettings )
    }
    sb ++= getTypeImports( value ).mkString( "\n" ).trim
    sb ++= s"\n\n"

    sb.result.trim
  }

  def renderUserDefinedClasses( typeDefs: List[TypeDefinition] ): String = {
    val sb = new StringBuilder

    val orderedTypeDefs =
      filterUserDefinedTypeNames( typeDefs )
        .filter( name =>
          findTypeOrdinality( name ).get.lowestOrdinal == options.release.map( _.ordinal ).min
        )

    sb ++= orderedTypeDefs.map( renderUserDefinedClass ).mkString(s"""\n\n""" )

    if (sb.nonEmpty)
      sb ++= s"""\n\n"""

    sb.result
  }

  def renderUserDefinedClass( typeName: String ): String = {
    val sb = new StringBuilder

    sb ++= s"""class $typeName(UserDefined$typeName):\n"""
    sb ++= s"""${WS_4}def __init__(self, **kwargs):\n"""
    sb ++= s"""${WS_8}self = UserDefined$typeName()\n"""
    sb ++= s"""${WS_8}data_model.DataObject.__init__(self, **kwargs)\n"""

    sb.result
  }

  def renderClasses( typeDefs: List[TypeDefinition] ): String = {
    val sb = new StringBuilder

    val orderedTypeDefs = orderByDependencies( typeDefs.filterNot( _.userDefined ) )

    sb ++= orderedTypeDefs.map( renderClass ).mkString(s"""\n\n""" )

    if (sb.nonEmpty)
      sb ++= s"""\n"""

    sb.result
  }

  def renderClass( typeDef: TypeDefinition ): String = {
    val sb = new StringBuilder

    sb ++= s"""class ${getTypeName( typeDef.name )}(data_model.DataObject):\n"""
    sb ++= s"""${renderCodeDocumentation( typeDef, typeDef.members, WS_4, useDocStringQuotes = true )}\n"""
    sb ++= typeDef.members.map( m => s"""${renderProperty( m )}""" ).mkString
    sb ++= s"""${WS_4}def __init__(self, **kwargs):\n"""
    sb ++= s"""${WS_8}data_model.DataObject.__init__(self, **kwargs)\n"""

    sb.result
  }

  def renderAdaptorImport( methods: List[Method] ): String = {
    if (hasAdaptors( methods ))
      s"""from $getAdaptorNamespace import $getAdaptorName\n"""
    else
      s"\n"
  }

  def renderResultsImports( methods: List[Method] ): String = {
    val lb = new ListBuffer[String]

    val typeNames = filterReturnTypeNames( methods )

    if (typeNames.nonEmpty) {
      val (nonResult, result) = typeNames.partition( x => !x.contains( "Result" ) )
      lb ++= nonResult.map( p => s"""from ${options.namespace}.models import $p""" )
      lb ++= result.map( p => s"""from ${options.namespace}.results import $p""" )
    }

    wrapLinesAt( lb.toList, WS_0, true, 79 ).map( line => if (line.trim.endsWith( "import" )) s"""${line.trim} \\""" else line ).mkString( "\n" )
  }

  def renderParameterDoc( aType: Typed, linePrefix: String ): String = {
    val optionalLabel = if (aType.typeUse.isOptional) "(optional)" else "[required]"
    s"""$linePrefix:param ${getPropertyName( aType.name )}: $optionalLabel ${aType.documentation.getOrElse( EmptyDoc ).lines.mkString( WS_1 )}"""
  }

  def renderParameterTypeDoc( aType: Typed, linePrefix: String ): String = {
    val array = if (aType.typeUse.isArray) "[]" else ""
    s"""$linePrefix:type ${getPropertyName( aType.name )}: ${getTypeName( aType.typeUse )}$array"""
  }

  def renderCodeDocumentation( typeDef: TypeDefinition, types: List[Typed], linePrefix: String, useDocStringQuotes: Boolean ): String = {
    val doc =
      if (typeDef.documentation.isEmpty && typeDef.name.endsWith( "Result" )) {
        val serviceName = codegen.Util.underscores( typeDef.name.replace( "Result", "" ) )
        Option( Documentation( List( s"""The object returned by the \"$serviceName\" API Service call.""" ) ) )
      }
      else
        typeDef.documentation

    renderCodeDocumentation( doc, types, linePrefix, useDocStringQuotes )
  }

  def renderCodeDocumentation( doc: Option[Documentation], types: List[Typed], linePrefix: String, useDocStringQuotes: Boolean ): String = {
    renderCodeDocumentation( doc.getOrElse( EmptyDoc ).lines, types, None, linePrefix, useDocStringQuotes )
  }

  def renderCodeDocumentation( doc: Documentation, types: List[Typed], returnInfo: Option[ReturnInfo], linePrefix: String, useDocStringQuotes: Boolean ): String = {
    renderCodeDocumentation( doc.lines, types, returnInfo, linePrefix, useDocStringQuotes )
  }


  def renderCodeDocumentation( lines: List[String], types: List[Typed], returnInfo: Option[ReturnInfo], linePrefix: String, useDocStringQuotes: Boolean ): String = {
    val lineEnding = if (useDocStringQuotes) "\n" else "\\\n"
    val lineColumn = if (useDocStringQuotes) 79 else 78
    val quotes = if (useDocStringQuotes) s"""\"\"\"""" else s"""\""""
    val startQuote = if (useDocStringQuotes) s"""$linePrefix$quotes""" else s"""$quotes"""

    val lineBreaksRemoved = convertLineBreaks( lines )
    val underscored = convertToUnderscoreNotation( lineBreaksRemoved )
    val linesWithPrefix = getCodeDocumentationLines( underscored, types, linePrefix, useDocStringQuotes )
    val linesSnapToIndent = snapToIndentBoundary( linesWithPrefix )
    val wrappedLines = wrapLinesAt( linesSnapToIndent, linePrefix, wrapOver = false, lineColumn )
    val trimmedWrappedLines = wrappedLines.map( StringOps.trimTrailingSpace )

    val paramLinesWithPrefix = getParameterDocumentationLines( types, linePrefix )
    val paramLineBreaksRemoved = convertLineBreaks( paramLinesWithPrefix )
    val paramRemovedHtml = paramLineBreaksRemoved.map( removeHtml( _, linePrefix, useDocStringQuotes = true ) )
    val paramUnderscored = convertToUnderscoreNotation( paramRemovedHtml )
    val paramSnapToIndent = snapToIndentBoundary( paramUnderscored )
    val wrappedParamLines = wrapLinesAt( paramSnapToIndent, linePrefix, wrapOver = true, lineColumn )
    val trimmedWrappedParamLines = wrappedParamLines.map( StringOps.trimTrailingSpace )

    val returnStatement = if (returnInfo.isEmpty)
      List( )
    else
      List( "", s"""$linePrefix:returns: a response""", s"""$linePrefix:rtype: ${returnInfo.get.returnType.typeName}""" )

    val allWrappedLines = List( startQuote ) ::: trimmedWrappedLines ::: trimmedWrappedParamLines ::: returnStatement ::: List(s"""$linePrefix$quotes""" )
    allWrappedLines.mkString( lineEnding ) + "\n"
  }

  def convertToUnderscoreNotation( lines: List[String] ): List[String] = {
    lines.map( convertToUnderscoreNotation )
  }

  def convertToUnderscoreNotation( line: String ): String = {
    val BEGIN_ESCAPE = ">>>"
    val END_ESCAPE = "<<<"



    val escapeStart = line.lastIndexOf( BEGIN_ESCAPE )
    val escapeEnd = line.lastIndexOf( END_ESCAPE )

    // Works from the end of the line, to the beginning, and prevents the underlining of words that are escaped.
    if (line.trim.length == 0) {
      line
    } else if (line.contains( BEGIN_ESCAPE ) && escapeStart < escapeEnd) {
      val doNotUnderline = line.substring( escapeStart + BEGIN_ESCAPE.length, escapeEnd )
      convertToUnderscoreNotation( line.substring( 0, escapeStart ) ) + doNotUnderline + convertToUnderscoreNotation( line.substring( escapeEnd + END_ESCAPE.length ) )
    } else if (line.contains( BEGIN_ESCAPE ) && escapeStart > escapeEnd) {
      convertToUnderscoreNotation( line.substring( 0, escapeStart ) ) + line.substring( escapeStart + BEGIN_ESCAPE.length )
    } else if (line.contains( END_ESCAPE )) {
      line.substring( 0, escapeEnd) + convertToUnderscoreNotation( line.substring( escapeEnd + END_ESCAPE.length ) )
    } else if (line.contains( ":type" )) {
      line
    } else {
      line.split( WS_1 ).map( word => underscoreDocumentation( word ) ).mkString( WS_1 )
    }
  }

  def snapToIndentBoundary( lines: List[String] ) = {
    lines.map( {
      case l if nonBoundaryIndent( l ) =>
        val indentIndex = firstNonWhiteSpaceIndex( l )
        val indexBoundary = indentIndex - (indentIndex % 4)
        " " * indexBoundary + l.trim

      case l => l
    } )
  }

  def convertLineBreaks( lines: List[String] ): List[String] = {
    val lb = new ListBuffer[String]

    lines.map( {
      case l if l.contains( "<br/>" ) =>
        val indentIndex = firstNonWhiteSpaceIndex( l )
        val linePrefix = " " * (indentIndex + 4)
        lb ++= l.replaceAll( "<br/><br/>", "<br/>" ).replaceAll( "<br/>", "\n\n" + linePrefix ).split( "\n" ).toList ::: List( "" )

      case l => lb += l
    } )


    lb.toList
  }

  def removeHtml( line: String, linePrefix: String, useDocStringQuotes: Boolean ): String = {
    line.replaceAll( "<[^>]*>", "**" ).replaceAll( "\"", "\\\\\"" ).replaceAll( "&quot;", "\\\\\"" )
  }

  def getCodeDocumentationLines( lines: List[String], params: List[Typed], linePrefix: String, useDocStringQuotes: Boolean ): List[String] = {
    val lb = new ListBuffer[String]

    lines.map( line => lb += s"""$linePrefix${removeHtml( line, linePrefix, useDocStringQuotes )}""" )

    lb.toList
  }

  def getParameterDocumentationLines( params: List[Typed], linePrefix: String ): List[String] = {
    val lb = new ListBuffer[String]

    params.sortBy( _.typeUse.isOptional ).map( p => lb ++= List( "", renderParameterDoc( p, linePrefix ), renderParameterTypeDoc( p, linePrefix ) ) )

    lb.toList
  }


  val wrapLines = ( lines: List[String], linePrefix: String ) => wrapLinesAt( lines, linePrefix, wrapOver = false, 79 )

  def wrapLinesAt( lines: List[String], linePrefix: String, wrapOver: Boolean, lineColumn: Int ): List[String] = {
    val wrapOverPrefix = if (wrapOver) WS_4 else WS_0
    @tailrec
    def wrapLinesImpl( lines: List[String], acc: List[String] ): List[String] = {
      lines match {
        case Nil => acc
        case x :: xs if x.trim.isEmpty => wrapLinesImpl( xs, acc ::: x.trim :: Nil )
        case x :: xs if x.length <= lineColumn || !x.trim.contains( ' ' ) => wrapLinesImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.trim.length + linePrefix.length > lineColumn && lastWhitespace( x, lineColumn ) > x.trim.length + linePrefix.length =>
          val nextWS = x.indexOf( ' ' )
          wrapLinesImpl( s"${lineAfterLastWhiteSpace( x, nextWS )}" :: xs, acc ::: s"""${lineBeforeLastWhiteSpace( x, nextWS )}\n""" :: Nil )

        case x :: xs if x.length > lineColumn =>
          val additionalWrapOver = if (!wrapOver && isDashAtIndentBoundry( x )) WS_2 else WS_0
          wrapLinesImpl( s"""$linePrefix$wrapOverPrefix$additionalWrapOver${lineAfterLastWhiteSpace( x, lineColumn )}""" :: xs, acc ::: s"""${lineBeforeLastWhiteSpace( x, lineColumn )}\n""" :: Nil )

        case x :: xs => wrapLinesImpl( xs, acc ::: x :: Nil )

      }
    }
    wrapLinesImpl( lines, List( ) )
  }

  def renderVersionChecks( method: Method ): String = {
    val sb = new StringBuilder

    if (method.params.exists( p => p.since.isDefined )) {
      sb ++= s"""${WS_8}self._check_param_versions(\n"""
      sb ++= s"""$WS_12'${getMethodName( method )}',\n"""
      sb ++= s"""$WS_12[\n"""
      for (param <- method.params) {
        if (param.since.isDefined) {
          sb ++= s"""$WS_16("${getVariableName( param.name )}",\n"""
          sb ++= s"""$WS_16 ${getVariableName( param.name )}, ${param.since.get}, None),\n"""
        }
      }
      sb ++= s"""$WS_12]\n"""
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

    val hasValueAdaptor = method.returnInfo.get.adaptor.isDefined && method.returnInfo.get.adaptor.get.supports.contains( "python" )

    if (hasValueAdaptor) {
      sb ++= s"""${WS_8}since = ${method.since.getOrElse( "None" )}\n"""
      if (method.deprecated.isDefined) {
        sb ++= s"""${WS_8}deprecated = ${method.deprecated.get.version}\n"""
      } else {
        sb ++= s"""${WS_8}deprecated = None\n"""
      }
      val returnStatement = s"""${WS_8}return $getAdaptorName.${Util.underscores( method.returnInfo.get.adaptor.get.name )}("""
      sb ++= s"""\n"""
      sb ++= s"""${returnStatement}self, params,\n"""
      sb ++= WS_1 * returnStatement.length + s"""since, deprecated)"""

    } else {
      sb ++= s"""${WS_8}return self.send_request(\n"""
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
    }
    sb.result
  }

  val getAdaptorNamespace: String = {
    if (options.adaptorBase.contains( '.' ))
      options.adaptorBase.substring( 0, options.adaptorBase.lastIndexOf( '.' ) )
    else
      ""
  }

  val getAdaptorName: String = {
    if (options.adaptorBase.contains( '.' ))
      options.adaptorBase.substring( options.adaptorBase.lastIndexOf( '.' ) + 1, options.adaptorBase.length )
    else
      options.adaptorBase
  }

  def hasAdaptors( methods: List[Method] ): Boolean = {
    methods.map( m => m.returnInfo ).flatMap( ri => ri.map( _.adaptor ) ).flatMap( ad => ad.map( _.supports ) ).flatten.contains( "python" )
  }

/*
  def orderByDependencies2(types: List[TypeDefinition] ): List[TypeDefinition] = {
    // Types that are aliases are not worth ordering
    val typesNonAliased: List[TypeDefinition] = types.filter(t => t.alias.isEmpty)

    // Dependencies can come from 3 places. Here, we check all 3.
    // 1. The members:
    // 2. The dictionaryType
    def getDependencies(typeDefinition: TypeDefinition): mutable.LinkedHashSet[TypeDefinition] = {
      // 1. Check if the name of a member matches any of the non-aliased types and recurse:
      val dependentTypesFromMembers = typesNonAliased.foreach(
        t => typeDefinition.members.foreach(
          m => if (t.name == m.typeUse.typeName) t
        )
      )
      // 2. Check if the dictionaryType matches any of the non-aliased types and recurse:
      val dependentTypesFromDictType = typesNonAliased.foreach(
        t => if (typeDefiniton.typeUse.dictionaryType.typeName == t.name) yield getDependencies(t)
      )
      return dependentTypesFromMembers ++ dependentTypesFromDictType ++ dependentTypesFromListType
    }

    typesNonAliased.reduceLeft(getDependencies(_)++_)
  }*/

  def orderByDependencies(types: List[TypeDefinition] ): List[TypeDefinition] = {

    // class to pair a TypeDefinition with its score
    case class ScoreCard( typeDef: TypeDefinition, score: Int )

    // Types that are aliases are not worth ordering
    val typesNonAliased: List[TypeDefinition] = types.filter(t => t.alias.isEmpty)

    // The ListBuffer that stores the ScoreCards and is updated during the calculations
    // All types are initialized with a score of 1
    val scores: ListBuffer[ScoreCard] = new ListBuffer[ScoreCard]()
    typesNonAliased.map(t => scores += ScoreCard( t,  1 ))

    // Function to recurse through the parent dependencies and sum up the scores from all parents
    // The result of this will be that the deeper dependencies are monotonically increasing in score.
    // so if A, B, and C all depend on D, which depends on E, their scores would be:
    // A: 1, B: 1, C: 1, D: A+B+C+1=4, E: D+1=5
    // Thus, the highest scoring parameters are the deepest dependencies.
    def countDependencies(typeDefinition: TypeDefinition, score: Int) : Int = {
      // This collects all the scores associated with the types which depend on the typeDefinition.
      val dependentTypeDefScores = scores.filter(
        s =>
          s.typeDef.members.exists(
          m =>
            m.typeUse.typeName == typeDefinition.name ||
            m.typeUse.dictionaryType.getOrElse("") == typeDefinition.name))

      if (dependentTypeDefScores.isEmpty){
        score
      }
      else {
        dependentTypeDefScores.map(dt => countDependencies(dt.typeDef, score + dt.score)).sum
      }
    }

    // iterate through all the non-alias types and their members.
    // when a member is found that has a supporting type, find its supporting score card
    // send the type into the countDependencies function to gather the right score
    for (
      typeDef <- typesNonAliased;
      member <- typeDef.members;
      foundType <- typesNonAliased.find(_.name == member.typeUse.typeName);
      foundOrNewScore: ScoreCard = scores.find(_.typeDef == foundType).getOrElse(ScoreCard(foundType, 0))
    ){
      scores -= foundOrNewScore
      scores += foundOrNewScore.copy(score = countDependencies(foundType, foundOrNewScore.score))
    }

    // turn all the scores into am ordered list of type definitions
    val definitions = for (
      score <- scores.sortBy(s => (-s.score, s.typeDef.name))
    ) yield score.typeDef

    scores.sortBy(s => (-s.score, s.typeDef.name)).map(s => println(s"${s.typeDef.name},${s.score}"))

    definitions.toList
  }

  def lineBeforeLastWhiteSpace( line: String ): String = lineBeforeLastWhiteSpace( line, 79 )

  val lineBeforeLastWhiteSpace = ( line: String, max: Int ) => {
    val lastWS: Int = lastWhitespace( line, max )
    line.substring( 0, if (lastWS <= 0) line.length else lastWS )
  }

  def lineAfterLastWhiteSpace( line: String ): String = lineAfterLastWhiteSpace( line, 79 )

  val lineAfterLastWhiteSpace = ( line: String, max: Int ) => {
    if (line.trim.isEmpty) ""
    else {
      val lastWS: Int = lastWhitespace( line, max )
      line.substring( if (lastWS < 0) 0 else lastWS, line.length ).trim
    }
  }

  def lastWhitespace( line: String ): Int = lastWhitespace( line, 79 )

  val lastWhitespace = ( line: String, max: Int ) => {
    Util.lastWhitespace( line, max )
  }

  def firstNonWhiteSpaceIndex( line: String ) = line.indexWhere( p => !p.isWhitespace )

  def firstNonLetterDigitOrWSIndex( line: String ) = line.indexWhere( p => !p.isLetterOrDigit && !p.isWhitespace )

  def nonLetterOrDigitAtIndentBoundary( line: String ) = firstNonWhiteSpaceIndex( line ) == firstNonLetterDigitOrWSIndex( line )

  def isDashAtIndentBoundry( line: String ) = firstNonWhiteSpaceIndex( line ) == line.indexWhere( '-'.equals( _ ) )

  def isIndentOnBoundary( line: String ) = firstNonWhiteSpaceIndex( line ) % 4 == 0

  def nonBoundaryIndent( line: String ) = !isIndentOnBoundary( line )

  def containsAny( src: String, matching: String ): Boolean = matching.toCharArray.exists( p => src.contains( p ) )

  def underscoreDocumentation( src: String ): String = {
    if (src.trim.isEmpty)
      return src
    if ("SolidFire".equals( src ) || "NetApp".equals( src ) || "iSCSI".equals( src ) || "IQNs".equals( src ))
      return src
    if (src.charAt( 0 ).isUpper && src.lastIndexWhere( _.isUpper ) == 0)
      return src
    if (containsAny( src, "\\/-*<>()[][]." ))
      return src
    if (src.equals( src.toUpperCase( ) ))
      return src
    if (src.indexWhere( p => p.isDigit ) != -1)
      return src

    val underscored = codegen.Util.underscores( src )

    if (underscored.equals( src ))
      src
    else
      '*' + underscored.replace( "qo_s", "qos" ).replace( "\\\"_", "\\\"" ).replace( "&quot;_", "&quot;" ) + '*'
  }

}
