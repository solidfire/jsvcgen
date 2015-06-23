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

object Util {

  import java.io.FileInputStream

  import org.fusesource.scalate.{ TemplateEngine, TemplateSource }
  import org.json4s.jackson.JsonMethods

  import scala.io.Source

  def camelCase( src: String, firstUpper: Boolean ): String = {
    val out = new StringBuilder( )
    var nextUpper = firstUpper
    var isFirst = true
    for (c ← src) {
      if (c == '_') {
        nextUpper = true
      } else if (nextUpper) {
        out.append( c.toUpper )
        nextUpper = false
      } else {
        out.append( if (isFirst) c.toLower else c )
        isFirst = false
      }
    }
    out.result( )
  }

  def underscores( src: String ): String = {
    val out = new StringBuilder( )
    var sawUpper = true
    for (c ← src) {
      if (sawUpper) {
        if (c.isUpper) {
          out.append( c.toLower )
        } else {
          sawUpper = false
          out.append( c )
        }
      } else {
        if (c.isUpper) {
          sawUpper = true
          out.append( '_' )
          out.append( c.toLower )
        } else {
          out.append( c )
        }
      }
    }
    out.result( )
  }

  def loadJson( path: String ) =
    JsonMethods.parse( Source.fromFile( path ).mkString )

  def loadJsonAs[T]( path: String )( implicit mf: Manifest[T] ) = {
    implicit val formats = org.json4s.DefaultFormats
    loadJson( path ).extract[T]
  }

  def loadResource( path: String ) =
    Source
      .fromInputStream( Option( getClass.getResourceAsStream( path ) ).getOrElse( new FileInputStream( path ) ) )
      .mkString

  def loadTemplate( path: String ): TemplateSource = {
    TemplateSource.fromText( path, loadResource( path ) )
  }

  def layoutTemplate( path: String, attributes: Map[String, Any] ): String = {
    val templateEngine = new TemplateEngine {
      escapeMarkup = false
    }

    templateEngine.layout( loadTemplate( path ), attributes )
  }

  def pathForNamespace( namespace: String ) = namespace.replaceAll( "\\.", "/" )

  def stringJoin( input: List[String], sep: String ): String = input match {
    case Nil => ""
    case last :: Nil => last
    case s :: rest => s + sep + stringJoin( rest, sep )
  }
}
