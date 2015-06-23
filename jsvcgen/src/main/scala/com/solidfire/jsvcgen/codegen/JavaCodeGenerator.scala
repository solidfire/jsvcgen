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

import scala.collection.immutable.Map
import scala.reflect.ClassTag

class JavaCodeGenerator( options: CliConfig )
  extends BaseCodeGenerator( options, nickname = Some( "java" ) ) {

  def formatTypeName( src: String ) = codegen.Util.camelCase( src, firstUpper = true )

  def pathFor( service: ServiceDefinition ) =
    codegen.Util.pathForNamespace( options.namespace ) + "/" + formatTypeName( service.serviceName ) + ".java"

  def pathFor( typ: TypeDefinition ) =
    codegen.Util.pathForNamespace( options.namespace ) + "/" + formatTypeName( typ.name ) + ".java"

  def pathFor( method: Method ) =
    codegen.Util.pathForNamespace( options.namespace ) + "/" + formatTypeName( method.name + "Request" ) + ".java"

  def toTypeDefinition( method: Method ): TypeDefinition = toTypeDefinition( method.name, method.params )

  def toTypeDefinition( requestName: String, params: List[Parameter] ): TypeDefinition = {
    TypeDefinition( requestName + "Request",
      None,
      params.map( param => Member( param.name, param.parameterType, param.documentation ) ) )
  }

  def asInterface( servicePath: String, service: ServiceDefinition ): Map[String, Any] = {
    Map(servicePath.replaceFirst( ".java", "IF.java" ) → service.asInstanceOf[ServiceDefinition].asInterface( ))
  }

  /**
   * In Java, we create a file for each TypeDefinition and for the ServiceDefinition.
   */
  override def groupItemsToFiles( service: ServiceDefinition ): Map[String, Any] = {
    Map( pathFor( service ) → service ) ++
      asInterface( pathFor( service ), service ) ++
      (for (typ ← service.types; if typ.alias.isEmpty) yield pathFor( typ ) → typ) ++
      (for (method ← service.methods) yield pathFor( method ) → toTypeDefinition( method ))
  }

  override protected def getDefaultMap[T]( service: ServiceDefinition, value: T )( implicit tag: ClassTag[T] ): Map[String, Any] =
    super.getDefaultMap( service, value ) ++ Map( "format" → new JavaCodeFormatter( options, service ) )
}
