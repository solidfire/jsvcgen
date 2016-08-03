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
import com.solidfire.jsvcgen.model.{ServiceDefinition, TypeDefinition}

import scala.collection.immutable.Map
import scala.reflect.ClassTag

class PythonCodeGenerator( options: CliConfig )
  extends BaseCodeGenerator( options, nickname = Some( "python" ) ) {

  def formatTypeName( src: String ) = Util.camelCase( src, firstUpper = true )

  override def groupItemsToFiles( service: ServiceDefinition ): Map[String, Any] = {
    val types = service.types.filter( typ => typ.alias.isEmpty )
      .map( typeDef => (pathFor( typeDef ), typeDef) )
      .groupBy( _._1 )
      .mapValues( _.map( _._2 ) )

    Map( pathFor( service ) -> service ) ++ types
  }

  def pathFor( service: ServiceDefinition ) = {
    getProjectPathFromNamespace + "__init__.py"
  }

  def pathFor( typ: TypeDefinition ) = {
    if (typ.name.endsWith( "Result" ))
      getProjectPathFromNamespace + "results.py"
    else
      getProjectPathFromNamespace + "models.py"
  }

  private def getProjectPathFromNamespace: String = {
    val splitNamespace = options.namespace.split( '.' )
    val projectPath = splitNamespace.drop( splitNamespace.indexWhere( e => e == options.output.getName ) + 1 )
    val path = codegen.Util.pathForNamespace( projectPath.mkString( "." ) ) + "/"
    path
  }

  override def getTemplatePath[T]( )( implicit tag: ClassTag[T] ) = {
    if (tag.runtimeClass.getSuperclass.getSimpleName.endsWith( "List" ))
      "/codegen/" + nickname.getOrElse( getClass.getName ) + "/TypeDefinitions.ssp"
    else
      super.getTemplatePath[T]
  }

  override protected def getDefaultMap[T]( service: ServiceDefinition, value: T )( implicit tag: ClassTag[T] ): Map[String, Any] =
    super.getDefaultMap( service, value ) ++ Map( "format" -> new PythonCodeFormatter( options, service ) )
}
