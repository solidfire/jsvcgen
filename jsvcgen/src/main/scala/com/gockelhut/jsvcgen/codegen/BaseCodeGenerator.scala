/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
**/
package com.gockelhut.jsvcgen.codegen

import java.io.File
import com.gockelhut.jsvcgen.model._
import java.io.FileWriter

abstract class BaseCodeGenerator(protected val options: CliConfig,
                                 override val nickname: Option[String] = None
                                )
    extends CodeGenerator {
  def loadTemplate(name: String) = Util.loadTemplate(name)
  
  def getOutputFile(suffix: String): File = suffix match {
    case "." => options.output
    case _   => new File(options.output, suffix)
  }
  
  def groupItemsToFiles(service: ServiceDefinition): Map[String, Any]
  
  override def generate(service: ServiceDefinition): Unit = {
    for ((outputFileSuffix, item) <- groupItemsToFiles(service)) {
      val contents = item match {
        case x: TypeDefinition    => fileContents(x)
        case x: ServiceDefinition => fileContents(x)
        case x                    => fileContentsExtension(x)
      }
      
      val file = getOutputFile(outputFileSuffix)
      if (options.dryRun) {
        println("### DRY-RUN ### FILENAME: \"" + file.getPath() + "\"")
        println(contents)
      } else {
        // actually write the file contents
        file.getParentFile().mkdirs()
        val writer = new FileWriter(file)
        try {
          writer.write(contents)
          if (!contents.endsWith("\n"))
            writer.write("\n")
        } finally {
          writer.close()
        }
      }
    }
  }
  
  def getTemplatePath[T]()(implicit mf: Manifest[T]) =
    "/codegen/" + nickname.getOrElse(getClass().getName()) + "/" + mf.runtimeClass.getSimpleName() + ".ssp"
  
  protected def getOptionsMap() =
    Map(
        "namespace" -> options.namespace
       )
  
  protected def fileContents(typeDefinition: TypeDefinition): String =
    Util.layoutTemplate(getTemplatePath[TypeDefinition],
                        getOptionsMap() ++ Map(
                            "name"          -> typeDefinition.name,
                            "documentation" -> typeDefinition.documentation,
                            "members"       -> typeDefinition.members
                           )
                       )
  
  protected def fileContents(serviceDefinition: ServiceDefinition): String =
    Util.layoutTemplate(getTemplatePath[ServiceDefinition],
                        getOptionsMap() ++ Map(
                            "servicename"   -> serviceDefinition.serviceName,
                            "url"           -> serviceDefinition.url,
                            "types"         -> serviceDefinition.types,
                            "methods"       -> serviceDefinition.methods,
                            "documentation" -> serviceDefinition.documentation,
                            "version"       -> serviceDefinition.version
                           )
                       )
  
  /**
   * In the cases where there is not an overload in this class for #fileContents of the given type, this function will
   * be called.
   */
  protected def fileContentsExtension(something: Any): String = ???
}
