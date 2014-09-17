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

import org.fusesource.scalate.{TemplateEngine, TemplateSource}

object Util {
  import java.io.{FileInputStream, InputStream}
  import scala.io.Source
  
  def camelCase(src: String, firstUpper: Boolean): String = {
    val out = new StringBuilder()
    var nextUpper = firstUpper
    for (c <- src) {
      if (c == '_') {
        nextUpper = true
      } else if (nextUpper) {
        out.append(c.toUpper)
        nextUpper = false
      } else {
        out.append(c)
      }
    }
    out.result
  }
  
  def loadResource(path: String) =
    Source.fromInputStream(Option(getClass().getResourceAsStream(path)).getOrElse(new FileInputStream(path))).mkString
  
  def loadTemplate(path: String): TemplateSource = {
    TemplateSource.fromText(path, loadResource(path))
  }
  
  def layoutTemplate(path: String, attributes: Map[String, Any]): String = {
    val templateEngine = new TemplateEngine
    templateEngine.layout(loadTemplate(path), attributes)
  }
  
  def pathForNamespace(namespace: String) = namespace.replaceAll("\\.", "/")
}
