package com.gockelhut.jsvcgen.codegen

import scala.collection.immutable.Map
import com.gockelhut.jsvcgen.model.ServiceDefinition
import com.gockelhut.jsvcgen.model.TypeDefinition

class JavaCodeGenerator(options: CliConfig)
    extends BaseCodeGenerator(options, nickname=Some("java")) {
  def formatTypeName(src: String) = Util.camelCase(src, true)
  
  def pathFor(service: ServiceDefinition) =
    Util.pathForNamespace(options.namespace) + "/" + formatTypeName(service.serviceName) + ".java"
  
  def pathFor(typ: TypeDefinition) =
    Util.pathForNamespace(options.namespace) + "/" + formatTypeName(typ.name) + ".java"
  
  /**
   * In Java, we create a file for each TypeDefinition and for the ServiceDefinition.
   */
  def groupItemsToFiles(service: ServiceDefinition): Map[String, Any] = {
    Map(pathFor(service) -> service) ++
      (for (typ <- service.types) yield (pathFor(typ) -> typ))
  }
}
