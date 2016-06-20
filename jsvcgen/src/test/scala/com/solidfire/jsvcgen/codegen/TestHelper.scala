package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.model._


object TestHelper {

  def buildServiceDefinition: ServiceDefinition = {
    ServiceDefinition(
      serviceName = "testServiceDefinition",
      host = "testHost",
      endpoint = "testEndpoint",
      types = List( buildTypeDefinition) ::: aliasedTypes,
      methods = List( buildMethod )
    )
  }

  def buildTypeDefinition: TypeDefinition = {
    TypeDefinition(
      name = "test",
      alias = None,
      members = List( buildMemberNumber,
        buildMemberCustom.copy( name = "CustomTypeField1" ),
        buildMemberCustom.copy( name = "CustomTypeField2" )
      ) )
  }

  def buildMemberNumber: Member = {
    Member( "SomeNumber", TypeUse( "integer" ) )
  }

  def buildMemberCustom: Member = {
    Member( "CustomTypeField", TypeUse( "CustomType" ) )
  }

  def buildMethod: Method = {
    Method(
      name = "testMethod",
      params = List( buildParameterString ),
      returnInfo = Some( ReturnInfo( TypeUse( "string" ) ) )
    )
  }

  def buildAliasedTypeDefinition: TypeDefinition = {
    TypeDefinition(
      name = "",
      alias = None,
      members = List.empty
    )
  }

  val yesOrNo   = buildAliasedTypeDefinition.copy( "yesOrNo", Some( TypeUse( "boolean" ) ) )
  val uint64    = buildAliasedTypeDefinition.copy( "uint64", Some( TypeUse( "integer" ) ) )
  val uint32    = buildAliasedTypeDefinition.copy( "uint32", Some( TypeUse( "integer" ) ) )
  val size_t    = buildAliasedTypeDefinition.copy( "size_t", Some( TypeUse( "integer" ) ) )
  val ID        = buildAliasedTypeDefinition.copy( "ID", Some( TypeUse( "uint32" ) ) )
  val bigID     = buildAliasedTypeDefinition.copy( "bigID", Some( TypeUse( "uint64" ) ) )
  val smallID   = buildAliasedTypeDefinition.copy( "smallID", Some( TypeUse( "size_t" ) ) )
  val ratio     = buildAliasedTypeDefinition.copy( "ratio", Some( TypeUse( "float" ) ) )
  val precision = buildAliasedTypeDefinition.copy( "precision", Some( TypeUse( "number" ) ) )
  val name      = buildAliasedTypeDefinition.copy( "name", Some( TypeUse( "string" ) ) )
  val UUID      = buildAliasedTypeDefinition.copy( "UUID", Some( TypeUse( "string" ) ) )

  val maybeYesOrNo  = buildAliasedTypeDefinition.copy( "maybeYesOrNo", Some( TypeUse( "boolean", isOptional = true ) ) )
  val someID        = buildAliasedTypeDefinition.copy( "someID", Some( TypeUse( "uint32", isOptional = true ) ) )
  val someBigID     = buildAliasedTypeDefinition.copy( "someBigID", Some( TypeUse( "uint64", isOptional = true ) ) )
  val someSmallID   = buildAliasedTypeDefinition.copy( "someSmallID", Some( TypeUse( "size_t", isOptional = true ) ) )
  val someRatio     = buildAliasedTypeDefinition.copy( "someRatio", Some( TypeUse( "float", isOptional = true ) ) )
  val somePrecision = buildAliasedTypeDefinition.copy( "somePrecision", Some( TypeUse( "number", isOptional = true ) ) )
  val someName      = buildAliasedTypeDefinition.copy( "someName", Some( TypeUse( "string", isOptional = true ) ) )
  val someUUID      = buildAliasedTypeDefinition.copy( "someUUID", Some( TypeUse( "string", isOptional = true ) ) )

  val aliasedTypes = List(yesOrNo, uint64, uint32, size_t, ID, bigID, smallID, ratio, precision, name, UUID, maybeYesOrNo, someID, someBigID, someSmallID, someRatio, somePrecision, someName, someUUID)

  def buildParameterString: Parameter = {
    Parameter(
      name = "testParameter",
      typeUse = TypeUse( "string" ),
      documentation = Some( Documentation( List( "This is a string parameter named testParameter" ) ) )
    )
  }

  def buildOptions: CliConfig = {
    CliConfig( )
  }

}
