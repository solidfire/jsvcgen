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

import java.io.File

import com.solidfire.jsvcgen.loader.JsvcgenDescription
import com.solidfire.jsvcgen.model
import com.solidfire.jsvcgen.model.ReleaseProcess._
import com.solidfire.jsvcgen.model.{ ReleaseProcess, ValidationException }

import scala.io.Source
import scala.util.{ Failure, Success, Try }


case class CliConfig( description:          File                        = new File( "." ),
                      output:               File                        = new File( "-" ),
                      generator:            String                      = "java",
                      namespace:            String                      = "com.example",
                      release:              Seq[StabilityLevel]    = Cli.defaultReleaseLevels,
                      headerTemplate:       Option[String]              = None,
                      headerTypeTemplate:   Option[String]              = None,
                      footerTemplate:       Option[String]              = None,
                      serviceBase:          Option[String]              = None,
                      requestBase:          Option[String]              = None,
                      serviceCtorTemplate:  Option[String]              = None,
                      typenameMapping:      Option[Map[String, String]] = None,
                      valueTypes:           Option[List[String]]        = None,
                      listFilesOnly:        Boolean                     = false
                      )

object Cli {
  val defaultReleaseLevels: Seq[StabilityLevel] = Seq(PUBLIC, INCUBATE)

  def createGenerator(config: CliConfig) = config.generator match {
    case "java" => new JavaCodeGenerator(config)
    case "python" => new PythonCodeGenerator(config)
    case "python2" => new PythonCodeGenerator(config)
    case "csharp" => new CSharpCodeGenerator(config)
    case "golang" => new GolangCodeGenerator(config)
    case "validate" => new Validator(config)
  }

  def validateWith[T](r: => T): Either[String, T] = {
    Try(r) match {
      case Success(x) => Right(x)
      case Failure(x) => Left(x.getMessage)
    }
  }

  def getParser() = {
    val ModelUtil = model.Util
    new scopt.OptionParser[CliConfig]("jsvcgen-generate") {
      head("jsvcgen")
      arg[File]( "description" )
        .text("JSON service description input file.")
        .required()
        .action { (x, c) => c.copy(description = x) }
      opt[File]( 'o', "output" )
        .text("Output destination -- where to put the generated code.")
        .optional()
        .action { (x, c) => c.copy(output = x) }
      opt[String]( 'g', "generator" )
        .text("Code generator to use.")
        .optional()
        .action { (x, c) => c.copy(generator = x) }
      opt[String]( "namespace" )
        .text("For namespace-based languages (such as Java), what namespace should the generated code be put in?")
        .optional()
        .action { (x, c) => c.copy(namespace = x) }
        .validate { x => validateWith(ModelUtil.validateNamespace(x)) }
      opt[Seq[String]]( "release" )
        .text("List of Release Process levels to generate (i.e. Public, Incubate, Private, ALL).")
        .optional()
        .action { (x, c) => c.copy(release = ReleaseProcess.fromNames(x.toList).getOrElse {
          Console.println(s"Unable to match Release Level from ${x.toList.mkString(",")}. Using default ${defaultReleaseLevels.mkString(",")} ")
          defaultReleaseLevels
        })
        }
      opt[String]( "header-template" )
        .text("Specify a template file to be used instead of the default header. " +
          "The value \"default\" means to use the generator's default header.")
        .optional()
        .action { (x, c) => c.copy(headerTemplate = if (x.equals("default")) None else Some(x)) }
      opt[String]( "header-type-template" )
        .text("Specify a template file to be used instead of the default type header. " +
          "The value \"default\" means to use the generator's default type header.")
        .optional()
        .action { (x, c) => c.copy(headerTypeTemplate = if (x.equals("default")) None else Some(x)) }
      opt[String]( "footer-template" )
        .text("Specify a template file to be used instead of the default footer. " +
          "The value \"default\" means to use the generator's default footer.")
        .optional()
        .action { (x, c) => c.copy(footerTemplate = if (x.equals("default")) None else Some(x)) }
      opt[String]( "service-base" )
        .text("When generating the output of a ServiceDefinition, the base class to use. " +
          "The value \"default\" means use the generator's default.")
        .optional()
        .action { (x, c) => c.copy(serviceBase = if (x.equals("default")) None else Some(x)) }
      opt[String]( "request-base" )
        .text("When generating the output of a TypeDefinition, the base class to use. " +
          "The value \"default\" means use the generator's default.")
        .optional()
        .action { (x, c) => c.copy(requestBase = if (x.equals("default")) None else Some(x)) }
      opt[String]( "service-constructor-template" )
        .text("Specify a template file to use instead of the default style. " +
          "The value \"default\" means use the generator's default.")
        .optional()
        .action { (x, c) => c.copy(serviceCtorTemplate = if (x.equals("default")) None else Some(x)) }
      opt[String]( "typename-mapping" )
        .text("A JSON file specifying a mapping of JSON name to native representation name.")
        .optional()
        .action { (x, c) => c
          .copy(typenameMapping = if (x.equals("default")) None
          else Some(Util
        .loadJsonAs[Map[String, String]]( x )))
        }
      opt[String]( "value-types" )
        .text("A JSON file specifying a list of type names to consider as value (struct) types (C# specific).")
        .optional()
        .action { (x, c) => c
          .copy(valueTypes = if (x.equals("default")) None else Some(Util.loadJsonAs[List[String]]( x )))
        }
      opt[Boolean]( "list-files-only" )
        .text("Instead of performing any output, tell the generator to simply list the files that it would output.")
        .optional()
        .action { (x, c) => c.copy(listFilesOnly = x) }
    }
  }

  def main(args: Array[String]): Unit = {
    getParser().parse(args, CliConfig()) map { config =>
      import org.json4s.jackson.JsonMethods

      Console.println(s"Generating with the following values")
      Console.println(s"input: ${config.description.getName}")
      Console.println(s"output: ${config.output.getName}")
      Console.println(s"release: ${config.release.mkString(",")}")
      Console.println(s"header-template: ${config.headerTemplate.getOrElse("None")}")
      Console.println(s"header-type-template: ${config.headerTypeTemplate.getOrElse("None")}")
      Console.println(s"footer-template: ${config.footerTemplate.getOrElse("None")}")
      Console.println(s"service-base: ${config.serviceBase.getOrElse("None")}")
      Console.println(s"request-base: ${config.requestBase.getOrElse("None")}")
      Console.println(s"service-constructor-template: ${config.serviceCtorTemplate.getOrElse("None")}")
      Console.println(s"typename-mapping: ${config.typenameMapping.getOrElse("None")}")
      Console.println(s"value-types: ${config.valueTypes.getOrElse("None")}")
      Console.println(s"list-files-only: ${config.listFilesOnly.toString}")

      // arguments are valid
      val generator = createGenerator(config)

      val service = JsvcgenDescription.load(JsonMethods.parse(Source.fromFile(config.description).mkString), config.release)

      try {
        //noinspection UnitInMap
        generator.generate(service)
      } catch {
        case err:
          ValidationException => println(err.getMessage)
          //noinspection UnitInMap
          System.exit(1)
      }
    } getOrElse {
      System.exit(1)
    }
  }
}
