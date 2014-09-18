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
import scala.reflect.ClassTag

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
      val contents = fileContents(service, item)(ClassTag(item.getClass))
      
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
  
  def getTemplatePath[T]()(implicit tag: ClassTag[T]) =
    "/codegen/" + nickname.getOrElse(getClass().getName()) + "/" + tag.runtimeClass.getSimpleName() + ".ssp"
  
  protected def getDefaultMap[T](service: ServiceDefinition, value: T)(implicit tag: ClassTag[T]): Map[String, Any] =
    Map(
        "codegen" -> this,
        "options" -> options,
        "value"   -> value,
        "service" -> service
       )
  
  protected def fileContents[T](service: ServiceDefinition, value: T)(implicit tag: ClassTag[T]): String =
    Util.layoutTemplate(getTemplatePath[T], getDefaultMap(service, value))
}
