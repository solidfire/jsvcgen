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
import scala.language.postfixOps
import scala.reflect.internal.util.StringOps

class PythonCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {
  val WS_0  = ""
  val WS_1  = " "
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

  def renderServiceBaseImport(options: CliConfig, serviceDefinition: ServiceDefinition): String = {
    if (ReleaseProcess.INTERNAL.equals( serviceDefintion.release ))
      s"""from ${options.namespace} import Element"""
    else
      s"""from ${options.namespace}.common import ${options.serviceBase.getOrElse("ServiceBase")}"""
  }

  def renderServiceBase(options: CliConfig, serviceDefinition: ServiceDefinition): String = {
    if (ReleaseProcess.INTERNAL.equals( serviceDefintion.release ))
      "Element"
    else
      options.serviceBase.getOrElse("ServiceBase")
  }

  def getTypeName( src: String ): String = {
    directTypeNames.get( src )
      .orElse( typeAliases.get( src ).map( getTypeName ) )
      .getOrElse( Util.camelCase( src, firstUpper = true ) )
  }

  def isDirectType( member: Member ): Boolean = {
    directTypeNames.values.exists( c => c == getTypeName( member.typeUse.typeName ) )
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
        case x :: xs if x.trim.isEmpty => wrapParameterDictImpl( xs, acc )
        case x :: xs if !x.contains( ' ' ) => wrapParameterDictImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length + linePrefix.length <= 79 => wrapParameterDictImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length + linePrefix.length > 79 && lastWhitespace( x ) > x.length + linePrefix.length => {
          val nextWS = x.indexOf( ' ' )
          wrapParameterDictImpl( xs, acc ::: lineBeforeLastWhiteSpace( x, nextWS ) :: s"$WS_4${lineAfterLastWhiteSpace( x, nextWS ).trim}" :: Nil )
        }
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

    sb ++= method.params.filter( p => p.typeUse.isOptional ).map( p => renderOptionalParameter( method, p ) ).mkString

    sb ++= s"""\n"""
    sb ++= renderServiceReturn( method )
    sb ++= s"""\n\n"""

    sb.result
  }


  def getProperty( member: Member ): List[String] = {
    val lb = new ListBuffer[String]

    lb += s"""$WS_4${getPropertyName( member )} = model.property(\n"""
    lb += s"""$WS_8"${member.name}","""
    lb += s"""$WS_1${getTypeName( member.typeUse.typeName )},\n"""
    lb += s"""${WS_8}array=${member.typeUse.isArray.toString.capitalize},"""
    lb += s"""${WS_1}optional=${member.typeUse.isOptional.toString.capitalize},\n"""
    lb += s"""${WS_8}documentation=${member.documentation.map( renderCodeDocumentation( _, List( ), "" ) ).getOrElse( "None\n" )}"""
    lb += s"""$WS_4)"""

    lb.toList
  }


  def renderProperty( member: Member ): String = {
    getProperty( member ).mkString + s"\n\n"
  }


  def getTypeImports( typeDefinitions: List[TypeDefinition] ): String = {

    val sb = new StringBuilder

    val members = typeDefinitions.filter( typeDef => typeDef.alias.isEmpty ).flatten( typeDef => typeDef.members ).distinct

    if (members.exists( p => "UUID".equalsIgnoreCase( getTypeName( p.typeUse.typeName ) ) )) {
      sb ++= s"from uuid import UUID\n"
    }
    val imports =
      for {
        memberTypes <- members.filterNot( m => isDirectType( m ) ).groupBy( m => m.typeUse.typeName )
      } yield memberTypes._1

    val filteredImports = imports.filterNot( typeDefinitions.map( t => t.name ) contains ).toList.distinct

    val modelImports = filteredImports.filterNot( p => p.endsWith( "Result" ) ).sorted
    val resultsImports = filteredImports.filter( p => p.endsWith( "Result" ) ).sorted

    sb ++= modelImports.map( p => s"""from solidfire.models import ${p}\n""" ).mkString
    sb ++= resultsImports.map( p => s"""from solidfire.results import ${p}\n""" ).mkString

    sb.result( ).trim
  }

  def renderImports( cliConfig: CliConfig, allSettings: Map[String, Any], value: List[TypeDefinition] ): String = {
    val sb = new StringBuilder
    if (cliConfig.headerTypeTemplate.isEmpty) {
      sb ++= s"""#!/usr/bin/python\n"""
      sb ++= s"""# -*- coding: utf-8 -*-\n"""
      sb ++= s"""#\n"""
      sb ++= s"""# DO NOT EDIT THIS CODE BY HAND! It has been generated with jsvcgen.\n"""
      sb ++= s"""#\n"""
      sb ++= s"""from __future__ import unicode_literals\n"""
      sb ++= s"""from __future__ import absolute_import\n"""
      sb ++= s"""from solidfire.common import model\n"""
    } else {
      sb ++= Util.layoutTemplate( options.headerTypeTemplate.get, allSettings )
    }
    sb ++= getTypeImports( value )

    sb.result.trim
  }

  def renderClasses( typeDefs: List[TypeDefinition] ): String = {
    val sb = new StringBuilder

    val orderedTypeDefs = ordered( typeDefs )

    sb ++= orderedTypeDefs.map( typeDef => renderClass( typeDef ) ).mkString(s"""\n\n""" )

    sb.result
  }

  def renderClass( typeDef: TypeDefinition ): String = {
    val sb = new StringBuilder

    sb ++= s"""class ${getTypeName( typeDef.name )}(model.DataObject):\n"""
    sb ++= s"""${renderCodeDocumentation( typeDef.documentation, typeDef.members, WS_4 )}\n"""
    sb ++= typeDef.members.map( m => s"""${renderProperty( m )}""" ).mkString
    sb ++= s"""${WS_4}def __init__(self, **kwargs):\n"""
    sb ++= s"""${WS_8}model.DataObject.__init__(self, **kwargs)\n"""

    sb.result
  }

  def renderResultsImports( methods: List[Method] ): String = {
    val sb = new StringBuilder

    val typeNames = methods.flatMap( f => f.returnInfo )
      .map( f => f.returnType )
      .distinct
      .sortBy( f => f.typeName )
      .map( t => getTypeName( t ) )

    if (typeNames.nonEmpty) {
      val (nonResult, result) = typeNames.partition( x => !x.contains( "Result" ) )
      sb ++= nonResult.map( p => s"""from solidfire.models import ${p}\n""" ).mkString
      sb ++= result.map( p => s"""from solidfire.results import ${p}\n""" ).mkString
    }

    sb.result.trim
  }

  def renderParameterDoc( aType: Typed, linePrefix: String ): String = {
    s"""$linePrefix:param ${getPropertyName( aType.name )}: ${aType.documentation.getOrElse( EmptyDoc ).lines.mkString( WS_1 )}"""
  }

  def renderParameterTypeDoc( aType: Typed, linePrefix: String ): String = {
    s"""$linePrefix:type ${getPropertyName( aType.name )}: {${getTypeName( aType.typeUse )}}"""
  }

  def renderCodeDocumentation( doc: Documentation, types: List[Typed], linePrefix: String ): String = {
    renderCodeDocumentation( doc.lines, types, linePrefix )
  }

  def renderCodeDocumentation( doc: Option[Documentation], types: List[Typed], linePrefix: String ): String = {
    renderCodeDocumentation( doc.getOrElse( EmptyDoc ).lines, types, linePrefix )
  }

  def renderCodeDocumentation( lines: List[String], types: List[Typed], linePrefix: String ): String = {
    val linesWithPrefix = getCodeDocumentationLines( lines, types, linePrefix )
    val wrappedLines = wrapLines( linesWithPrefix, linePrefix )
    val trimmedWrappedLines = wrappedLines.map( l => StringOps.trimTrailingSpace( l ) + "\n" )

    trimmedWrappedLines.mkString
  }

  def getCodeDocumentationLines( lines: List[String], params: List[Typed], linePrefix: String ): List[String] = {
    val lb = new ListBuffer[String]

    lb += s"""$linePrefix\"\"\""""
    lines.map( line => lb += s"""$linePrefix$line""" )
    lb += ""
    params.sortBy( _.typeUse.isOptional ).map( p => lb ++= List( renderParameterDoc( p, linePrefix ), renderParameterTypeDoc( p, linePrefix ) ) )
    lb += s"""$linePrefix\"\"\""""

    lb.toList
  }

  def wrapLines( lines: List[String], linePrefix: String ): List[String] = {
    @tailrec
    def wrapLinesImpl( lines: List[String], acc: List[String] ): List[String] = {
      lines match {
        case Nil => acc
        case x :: xs if x.trim.isEmpty => wrapLinesImpl( xs, acc )
        case x :: xs if x.length <= 79 || !x.trim.contains( ' ' ) => wrapLinesImpl( xs, acc ::: x :: Nil )
        case x :: xs if x.length + linePrefix.length > 79 && lastWhitespace( x ) > x.length + linePrefix.length => {
          val nextWS = x.indexOf( ' ' )
          wrapLinesImpl( s"${lineAfterLastWhiteSpace( x, nextWS )}" :: xs, acc ::: s"""${lineBeforeLastWhiteSpace( x, nextWS )}\n""" :: Nil )
        }
        case x :: xs if x.length > 79 => wrapLinesImpl( s"""$linePrefix${lineAfterLastWhiteSpace( x )}""" :: xs, acc ::: s"""${lineBeforeLastWhiteSpace( x )}\n""" :: Nil )
        case x :: xs => wrapLinesImpl( xs, acc ::: x :: Nil )

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

    def memberTypes( typeDefinition: TypeDefinition ): List[String] = {
      typeDefinition.members.map( m => m.typeUse.typeName ).filterNot( p => directTypeNames.keySet.contains( p ) )
    }

    def sortByDependancies( types: List[TypeDefinition] ): List[TypeDefinition] = {
      def score( types: List[TypeDefinition] ): List[TypeDefinition] = {
        case class ScoreCard( memberNames: List[String], score: Int ) {
          def apply( a: (List[String], Int) ): ScoreCard = ScoreCard( a._1, a._2 )

          def apply( a: (List[String], Int), scoreUpdate: Int ): ScoreCard = ScoreCard( a._1, a._2 + scoreUpdate )

          def update( scoreUpdate: Int ): ScoreCard = ScoreCard( this.memberNames, this.score + scoreUpdate )
        }
        case class TypeDefScore( typeDef: TypeDefinition, card: ScoreCard )

        val scoredTypes: List[TypeDefScore] = types.map( t => TypeDefScore( t, ScoreCard( memberTypes( t ), 1 ) ) )

        def hasAllScores( scoreCard: ScoreCard, scores: List[TypeDefScore] ): Boolean = {
          scores.map( s => s.typeDef.name ).count( p => scoreCard.memberNames.distinct.contains( p ) ) == scoreCard.memberNames.distinct.length
        }

        def memberTypeExistsAsTypeDef( typeDef: TypeDefinition ): Boolean = {
          memberTypes( typeDef ).forall( p => types.map( t => t.name ).contains( p ) )
        }

        def nonExistantMemberType( typeDef: TypeDefinition ): List[String] = {
          memberTypes( typeDef ).filterNot( p => types.map( t => t.name ).contains( p ) )
        }

        def calcScore( scoreCard: ScoreCard, scores: List[TypeDefScore] ): Int = {
          scores.filter( p => scoreCard.memberNames.contains( p.typeDef.name ) )
            .map( s => s.card.score )
            .sum
        }

        def doesDependOn(a: TypeDefScore, b: TypeDefScore): Boolean = {
          if (a.card.memberNames.contains( b.typeDef.name ))
            false
          else if (b.card.memberNames.contains( a.typeDef.name ))
            true
          else
            a.typeDef.name.compareTo( b.typeDef.name ) < 0
        }

        @tailrec
        def scoreImpl( scores: List[TypeDefScore], acc: List[TypeDefScore] ): List[TypeDefScore] = {
          scores match {
            case Nil => acc
            case s :: xs if s.card.memberNames.isEmpty => scoreImpl( xs, acc ::: s :: Nil )
            case s :: xs if s.card.memberNames.nonEmpty && hasAllScores( s.card, acc ) =>
              scoreImpl( xs, acc ::: TypeDefScore( s.typeDef, s.card.update( calcScore( s.card, acc ) ) ) :: Nil )
            case s :: xs if s.card.memberNames.nonEmpty && !memberTypeExistsAsTypeDef( s.typeDef ) =>
              scoreImpl( xs, acc ::: TypeDefScore( s.typeDef, s.card.update( calcScore( s.card, acc ) + nonExistantMemberType( s.typeDef ).length ) ) :: Nil )
            case s :: xs => scoreImpl( xs ::: s :: Nil, acc )
          }
        }
        val scoredResults = scoreImpl( scoredTypes, List( ) )
        val sorted = scoredResults.sortWith {
          case (a, b) if a.card.score == b.card.score => doesDependOn(a, b)
          case (a, b) => a.card.score < b.card.score
        } map (a => a.typeDef)

        sorted
      }
      score( types )
    }

    val (nonResult, result) = types.sortBy( x => x.name ).partition( x => !x.name.contains( "Result" ) )
    sortByDependancies( nonResult ) ++ sortByDependancies( result )
  }

  private def lineBeforeLastWhiteSpace( line: String ): String = lineBeforeLastWhiteSpace( line, 79 )

  private val lineBeforeLastWhiteSpace = ( line: String, max: Int ) => {
    val lastWS: Int = lastWhitespace( line, max )
    line.substring( 0, if (lastWS <= 0) line.length else lastWS )
  }

  private def lineAfterLastWhiteSpace( line: String ): String = lineAfterLastWhiteSpace( line, 79 )

  private def lineAfterLastWhiteSpace = ( line: String, max: Int ) => {
    if (line.trim.isEmpty) ""
    else {
      val lastWS: Int = lastWhitespace( line, max )
      line.substring( if (lastWS < 0) 0 else lastWS, line.length ).trim
    }
  }

  private def lastWhitespace( line: String ): Int = lastWhitespace( line, 79 )

  private def lastWhitespace = ( line: String, max: Int ) => {
    Util.lastWhitespace( line, max )
  }


}
