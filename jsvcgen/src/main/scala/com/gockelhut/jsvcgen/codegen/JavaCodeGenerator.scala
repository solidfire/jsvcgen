package com.gockelhut.jsvcgen.codegen

import scala.collection.immutable.Map
import com.gockelhut.jsvcgen.model._

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
  private val directTypeNames = Map(
                                    "boolean" -> "boolean",
                                    "integer" -> "long",
                                    "number"  -> "double",
                                    "string"  -> "String",
                                    "float"   -> "float"
                                   )
  
  def getTypeName(src: String): String = directTypeNames.getOrElse(src, Util.camelCase(src, true))
  def getTypeName(src: TypeDefinition): String = getTypeName(src.name)
  def getTypeName(src: TypeUse): String = src match {
    case TypeUse(name, false, false) => getTypeName(name)
    case TypeUse(name, false, true)  => "Optional<" + getTypeName(name) + ">"
    case TypeUse(name, true,  false) => getTypeName(name) + "[]"
    case TypeUse(name, true,  true)  => "Optional<" + getTypeName(name) + "[]>"
  }
  def getTypeName(src: Option[ReturnInfo]): String = src match {
    case Some(src) => getTypeName(src.returnType)
    case None      => "void"
  }
  
  def getFieldName(src: String): String = Util.camelCase(src, false)
  def getFieldName(src: Member): String    = getFieldName(src.name)
  def getFieldName(src: Parameter): String = getFieldName(src.name)
  
  def getMemberAccessorName(src: String): String = "get" + Util.camelCase(src, true)
  def getMemberAccessorName(src: Member): String = getMemberAccessorName(src.name)
  
  def getMemberMutatorName(src: String): String = "set" + Util.camelCase(src, true)
  def getMemberMutatorName(src: Member): String = getMemberMutatorName(src.name)
  
  def getMethodName(src: String): String = Util.camelCase(src, false)
  def getMethodName(src: Method): String = getMethodName(src.name)
  
  def getParameterListForMembers(params: List[Member]): String =
    Util.stringJoin((for (member <- params) yield getTypeName(member.memberType) + " " + getFieldName(member)), ", ")
  
  def getParameterList(params: List[Parameter]): String =
    Util.stringJoin((for (param <- params) yield getTypeName(param.parameterType) + " " + getFieldName(param)), ", ")
    
  def getParameterUseList(params: List[Parameter]): String =
    Util.stringJoin((for (param <- params) yield getFieldName(param)), ", ")
}
