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

class GolangCodeGenerator( options: CliConfig )
  extends BaseCodeGenerator( options, nickname = Some( "golang" ) ) {

  def formatTypeName(src: String) = codegen.Util.camelCase(src, firstUpper = true)

  def pathFor(service: ServiceDefinition) =
    getProjectPathFromNamespace  + "services/" + formatTypeName(service.serviceName) + ".go"

  def pathFor(typ: TypeDefinition) =
    getProjectPathFromNamespace + "types/" + formatTypeName(typ.name) + ".go"

  def pathForRequestType(method: Method) =
    getProjectPathFromNamespace + "types/"+ formatTypeName(method.name + "Request") + ".go"

  private def getProjectPathFromNamespace: String = {
    val splitNamespace = options.namespace.split('.')
    val projectPath = splitNamespace.drop(splitNamespace.indexWhere(e => e == options.output.getName) + 1)
    val path = codegen.Util.pathForNamespace(projectPath.mkString(".")) + "/"
    path
  }

  override def groupItemsToFiles(service: ServiceDefinition): Map[String, Any] = {
    Map(pathFor(service) -> service) ++
      (
        for (typ <- service.types if typ.alias.isEmpty)
          yield pathFor(typ) -> typ
        ) ++
      (
        for (method <- service.methods.filter(m => m.params.nonEmpty))
          yield pathForRequestType(method) -> toTypeDefinition(method)
        )
  }

  override protected def getDefaultMap[T](service: ServiceDefinition, value: T)(implicit tag: ClassTag[T]): Map[String, Any] =
    super.getDefaultMap(service, value) ++ Map("format" -> new GolangCodeFormatter(options, service))

}
