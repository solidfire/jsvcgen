package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.model._


object TestHelper {

  def buildServiceDefinition: ServiceDefinition = {
    ServiceDefinition(
      serviceName = "testServiceDefinition",
      host = "testHost",
      endpoint = "testEndpoint",
      types = List(buildTypeDefinition),
      methods = List(buildMethod)
    )
  }

  def buildTypeDefinition: TypeDefinition = {
    TypeDefinition(
      name = "test",
      alias = None,
      members = List(buildMemberNumber,
        buildMemberCustom.copy(name = "CustomTypeField1"),
        buildMemberCustom.copy(name = "CustomTypeField2")
      ))
  }

  def buildMemberNumber: Member = {
    Member("SomeNumber", TypeUse("int"))
  }

  def buildMemberCustom: Member = {
    Member("CustomTypeField", TypeUse("CustomType"))
  }

  def buildMethod: Method = {
    Method(
      name = "testMethod",
      params = List(buildParameterString),
      returnInfo = Some(ReturnInfo(TypeUse("string")))
    )
  }

  def buildParameterString: Parameter = {
    Parameter(
      name = "testParameter",
      parameterType = TypeUse("string"),
      documentation = Some(Documentation(List("This is a string parameter named testParameter")))
    )
  }

  def buildOptions : CliConfig = {
    CliConfig()
  }

}
