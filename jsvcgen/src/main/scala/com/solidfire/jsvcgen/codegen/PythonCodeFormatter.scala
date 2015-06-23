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
import com.solidfire.jsvcgen.model._

class PythonCodeFormatter( options: CliConfig, serviceDefintion: ServiceDefinition ) {
  private val directTypeNames = options.typenameMapping.getOrElse(
                                                                   Map(
                                                                        "boolean" → "bool",
                                                                        "integer" → "int",
                                                                        "number" → "float",
                                                                        "string" → "str",
                                                                        "float" → "float",
                                                                        "object" → "dict"
                                                                      )
                                                                 )

  def getTypeName( src: String ): String =
    directTypeNames.getOrElse( src, codegen.Util.camelCase( src, firstUpper = true ) )

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

  def getCodeDocumentation( lines: List[String], linePrefix: String ): String = {
    val sb = new StringBuilder
    sb.append( linePrefix )
      .append( "\"\"\"" )
    var first = true
    for (line ← lines) {
      if (first)
        first = false
      else
        sb.append( linePrefix )
      sb.append( line )
        .append( '\n' )
    }
    if (sb.last == '\n')
      sb.setLength( sb.length - 1 )
    sb.append( "\"\"\"" )
      .result( )
  }

  def getCodeDocumentation( doc: Documentation, linePrefix: String ): String = getCodeDocumentation( doc
    .lines, linePrefix )

  def ordered( types: List[TypeDefinition] ): List[TypeDefinition] = {
    orderedImpl( types, directTypeNames
      .map { case (name, _) => TypeDefinition( name, None, List( ), None ) }
      .toList )
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

  def typeFulfilled( typ: TypeDefinition, types: List[TypeDefinition] ): Boolean = typ match {
    case TypeDefinition( _, Some( use ), List( ), _ ) => true
    case TypeDefinition( _, None, members, _ ) => members
      .forall( mem => types.exists( x => x.name == mem.memberType.typeName ) )
  }
}
