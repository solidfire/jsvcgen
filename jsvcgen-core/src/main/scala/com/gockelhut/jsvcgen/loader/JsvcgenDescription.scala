package com.gockelhut.jsvcgen.loader

import org.json4s.JsonAST._
import com.gockelhut.jsvcgen.model._
import org.json4s.{CustomSerializer, DefaultFormats, FieldSerializer}
import org.json4s.`package`.MappingException
import org.json4s.`package`.MappingException

private object JArrayOfStrings {
  private def unapplyImpl(xs: List[JValue]): Option[List[String]] = xs match {
    case Nil                => Some(Nil)
    case JString(s) :: rest => unapplyImpl(rest) map (s :: _)
    case _                  => None
  }
  
  def unapply(xs: JArray): Option[List[String]] = unapplyImpl(xs.children)
}

/**
 * Loads objects in the custom-built jsvcgen description format.
 */
object JsvcgenDescription {
  import org.json4s.FieldSerializer._
  
  class DocumentationSerializer
      extends CustomSerializer[Documentation](format => (
          {
            case JArrayOfStrings(lst) => Documentation(lst)
            case JString(s) =>           Documentation(List(s))
          },
          {
            case Documentation(lines) => JArray(lines.map { x => JString(x) })
          }
        ))
  
  class MemberSerializer
      extends FieldSerializer[Member](renameTo("memberType", "type"),
                                      renameFrom("type", "memberType")
                                     )
  
  
  class MethodSerializer
      extends FieldSerializer[Method](renameTo("returnInfo", "return_info"),
                                      renameFrom("return_info", "returnInfo")
                                     )
  
  class ParameterSerializer
      extends FieldSerializer[Parameter](renameTo("parameterType", "type"),
                                         renameFrom("type", "parameterType")
                                        )
  
  class ReturnInfoSerializer
      extends FieldSerializer[ReturnInfo](renameTo(  "returnType", "type"),
                                          renameFrom("type", "returnType"))
  
  class ServiceDefinitionSerializer
      extends FieldSerializer[ServiceDefinition](renameTo(  "serviceName", "servicename"),
                                                 renameFrom("servicename", "serviceName"))
  
  class TypeUseSerializer
    extends CustomSerializer[TypeUse](format => (
          {
            case JString(name)               => TypeUse(name, false, false)
            case JArray(List(JString(name))) => TypeUse(name, true,  false)
            case JObject(JField("name",     JString(name))
                      :: JField("optional", JBool(isOptional))
                      :: Nil
                      )
                                             => TypeUse(name, false, isOptional)
            case JObject(JField("name",     JArray(List(JString(name))))
                      :: JField("optional", JBool(isOptional))
                      :: Nil
                      )
                                             => TypeUse(name, true, isOptional)
          },
          {
            case TypeUse(name, false, false) => JString(name)
            case TypeUse(name, false, true)  => JObject(JField("name",     JString(name))
                                                     :: JField("optional", JBool(true))
                                                     :: Nil
                                                     )
            case TypeUse(name, true, false) => JArray(List(JString(name)))
            case TypeUse(name, true, true)  => JObject(JField("name",     JArray(List(JString(name))))
                                                    :: JField("optional", JBool(true))
                                                    :: Nil
                                                    )
          }
        ))
  
  implicit def formats = DefaultFormats +
                         new DocumentationSerializer() +
                         new MemberSerializer() +
                         new MethodSerializer() +
                         new ParameterSerializer() +
                         new ReturnInfoSerializer() +
                         new ServiceDefinitionSerializer() +
                         new TypeUseSerializer()
  
  def load(input: JValue): ServiceDefinition = {
    input.extract[ServiceDefinition]
  }
}
