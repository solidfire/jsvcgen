package com.gockelhut.jsvcgen.codegen

import scala.collection.immutable.Map
import com.gockelhut.jsvcgen.model.{Member, ServiceDefinition, TypeDefinition, TypeUse}

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

object JavaCodeGenerator {
  def getTypeName(src: String): String = Util.camelCase(src, true)
  def getTypeName(src: TypeDefinition): String = getTypeName(src.name)
  def getTypeName(src: TypeUse): String = src match {
    case TypeUse(name, false, false) => getTypeName(name)
    case TypeUse(name, false, true)  => "Optional<" + getTypeName(name) + ">"
    case TypeUse(name, true,  false) => getTypeName(name) + "[]"
    case TypeUse(name, true,  true)  => "Optional<" + getTypeName(name) + "[]>"
  }
  
  def getFieldName(src: String): String = Util.camelCase(src, false)
  def getFieldName(src: Member): String = getFieldName(src.name)
  
  def getMemberAccessorName(src: String): String = "get" + Util.camelCase(src, true)
  def getMemberAccessorName(src: Member): String = getMemberAccessorName(src.name)
  
  def getMemberMutatorName(src: String): String = "set" + Util.camelCase(src, true)
  def getMemberMutatorName(src: Member): String = getMemberMutatorName(src.name)
  
  def getParameterListFrom(params: List[Member]) =
    Util.stringJoin((for (member <- params) yield getTypeName(member.memberType) + " " + getFieldName(member)), ", ")
}
