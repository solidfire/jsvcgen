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

import com.solidfire.jsvcgen.model.{ Method, ServiceDefinition, TypeUse, ValidationException }

object Validator {
  /**
   * The built-in types for JSON.
   */
  def builtInTypes = Set( "boolean",
                          "integer",
                          "number",
                          "null",
                          "float",
                          "string",
                          "Object",
                          "Dictionary"
                        )
}

class Validator( config: CliConfig )
  extends CodeGenerator {
  override def generate( service: ServiceDefinition ) = {
    val problems = new StringBuilder
    val addProblem = ( problem: String ) => {problems.append( problem ); problems.append( "\n" ) }

    val typenames = Validator.builtInTypes ++ (for (typ <- service.types) yield typ.name)

    // check that all types with members refer to types which exist
    for (typ <- service.types; member <- typ.members; if !typenames.contains( member.memberType.typeName ))
      addProblem(
                  "Type \"" + typ.name + "\" " +
                    "has member \"" + member.name + "\" " +
                    "with type \"" + member.memberType.typeName + "\" " +
                    "which does not exist."
                )

    // check that all types have either an alias or members
    for (typ <- service.types; if typ.alias.isDefined && typ.members.nonEmpty)
      addProblem( "Type \"" + typ.name + "\" has both an alias and members." )

    // check that all methods have parameters and return types which exist
    val addMethodTypenameProblem = ( method: Method, ref: String, refName: Option[String], typ: TypeUse ) =>
      addProblem(
                  "Method \"" + method.name + "\" " +
                    ref + refName.map( x => " \"" + x + "\"" ).getOrElse( "" ) + " " +
                    "refers to a type \"" + typ.typeName + "\" " +
                    "which does not exist."
                )
    for (method <- service.methods) {
      for (param <- method.params; if !typenames.contains( param.parameterType.typeName )) {
        addMethodTypenameProblem( method, "parameter", Some( param.name ), param.parameterType )
      }
      for (returnInfo <- method.returnInfo; if !typenames.contains( returnInfo.returnType.typeName ))
        addMethodTypenameProblem( method, "return type", None, returnInfo.returnType )
    }

    val err = problems.result( )
    if (!err.isEmpty)
      throw new ValidationException( err )
  }
}
