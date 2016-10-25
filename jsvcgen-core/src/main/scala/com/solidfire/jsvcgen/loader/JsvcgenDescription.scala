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
package com.solidfire.jsvcgen.loader

import com.solidfire.jsvcgen.model._
import org.json4s.JsonAST._
import org.json4s.{CustomSerializer, DefaultFormats, FieldSerializer}

import scala.collection.mutable.ListBuffer
import scala.util.Try

private object JArrayOfStrings {
  private def unapplyImpl( xs: List[JValue] ): Option[List[String]] = xs match {
    case Nil => Some( Nil )
    case JString( s ) :: rest => unapplyImpl( rest ) map (s :: _)
    case _ => None
  }

  def unapply( xs: JArray ): Option[List[String]] = unapplyImpl( xs.children )
}

/**
  * Loads objects in the custom-built jsvcgen description format.
  */
object JsvcgenDescription {

  import com.solidfire.jsvcgen.model.ReleaseProcess.StabilityLevel
  import org.json4s.FieldSerializer._

  object StabilityLevelSerializer extends CustomSerializer[ReleaseProcess.StabilityLevel]( format =>
    ( {
      case JString( s ) => ReleaseProcess.fromName( s ).getOrElse( ReleaseProcess.INTERNAL )
      case JNull => ReleaseProcess.INTERNAL
    }, {
      case x: StabilityLevel => JString( x.toString )
    }
      )
  )

  class DocumentationSerializer
    extends CustomSerializer[Documentation]( format => ( {
      case JArrayOfStrings( lst ) => Documentation( lst )
      case JString( s ) => Documentation( List( s ) )
    }, {
      case Documentation( lines ) => JArray( lines.map { x => JString( x ) } )
    }
      ) )

  class MemberSerializer extends FieldSerializer[Member](
    renameTo( "typeUse", "type" ),
    renameFrom( "type", "typeUse" )
  )

  class ParameterSerializer extends FieldSerializer[Parameter](
    renameTo( "typeUse", "type" ),
    renameFrom( "type", "typeUse" )
  )

  class ReturnInfoSerializer extends FieldSerializer[ReturnInfo](
    renameTo( "returnType", "type" ),
    renameFrom( "type", "returnType" )
  )

  class ServiceDefinitionSerializer extends FieldSerializer[ServiceDefinition](
    renameTo( "serviceName", "servicename" ),
    renameFrom( "servicename", "serviceName" )
  )

  class TypeUseSerializer extends CustomSerializer[TypeUse]( ser = format =>
    ( {
      case JString( name ) => TypeUse( name, isArray = false, isOptional = false )
      case JArray( List( JString( name ) ) ) => TypeUse( name, isArray = true, isOptional = false )
      case JObject( List( ("name", JString( name )) ) ) => TypeUse( name, isArray = false, isOptional = false )
      case JObject( List( ("name", JString( name )), ("dictionaryType", JString( dictionaryType )) ) ) => TypeUse( name, isArray = false, isOptional = false, Option( dictionaryType.toString ) )
      // The JObject unapply cares about the order of the fields...there has to be a better way to do this...
      case JObject( JField( "name", JString( name ) )
        :: JField( "optional", JBool( isOptional ) )
        :: Nil
      )
      => TypeUse( name, isArray = false, isOptional = isOptional )
      case JObject( JField( "optional", JBool( isOptional ) )
        :: JField( "name", JString( name ) )
        :: Nil
      )
      => TypeUse( name, isArray = false, isOptional = isOptional )
      case JObject( JField( "name", JArray( List( JString( name ) ) ) )
        :: JField( "optional", JBool( isOptional ) )
        :: Nil
      )
      => TypeUse( name, isArray = true, isOptional = isOptional )
      case JObject( JField( "optional", JBool( isOptional ) )
        :: JField( "name", JArray( List( JString( name ) ) ) )
        :: Nil
      )
      => TypeUse( name, isArray = true, isOptional = isOptional )
    }, {
      case TypeUse( name, false, false, None ) => JString( name )
      case TypeUse( name, false, true, None ) =>
        JObject( JField( "name", JString( name ) ) :: JField( "optional", JBool( value = true ) ) :: Nil )

      case TypeUse( name, true, false, None ) => JArray( List( JString( name ) ) )
      case TypeUse( name, true, true, None ) =>
        JObject(
          JField( "name", JArray( List( JString( name ) ) ) ) :: JField( "optional", JBool( value = true ) ) :: Nil )
      case TypeUse( name, false, true, optionalString ) =>
        JObject(
          JField( "name", JString( name ) ) :: JField( "optional", JBool( value = true ) ) :: JField( "dictionaryType", JString( optionalString.getOrElse( "" ) ) ) :: Nil )
    }
      )
  )

  implicit def formats = DefaultFormats +
    StabilityLevelSerializer +
    new DocumentationSerializer( ) +
    new MemberSerializer( ) +
    new ParameterSerializer( ) +
    new ReturnInfoSerializer( ) +
    new ServiceDefinitionSerializer( ) +
    new TypeUseSerializer( )

  def load( input: JValue, stabilityLevels: Seq[StabilityLevel] ): ServiceDefinition = {
    filterMethodsToRelease( input.extract[ServiceDefinition], stabilityLevels )
  }

  // This function checks the input service definition and filters down to only the methods and types
  // required for the inputted stability levels
  def filterMethodsToRelease( inputService: ServiceDefinition, releaseLevels: Seq[StabilityLevel] ): ServiceDefinition = {

    // First filter down the methods.
    val methodsForRelease = inputService.methods.filter( method => releaseLevels.contains( method.release ) )

    // Get a list of all the types from the parameters of the methods
    val methodParamNamesForRelease = methodsForRelease.flatMap( method => method.params ).map( param => param.typeUse.typeName ).distinct

    // Now, iterate through ALL the methods in the service. Go through all their return types and discover all the members.
    // Figure out what the lowest ordinal value is for each type as it may have already been generated in a lower stability level release.
    // Filter out the types to only those needed for this stability level list.

    val typeOrdinals = discoverOrdinality( inputService )
    val methodReturnTypes = typeOrdinals
      .filter( t => releaseLevels.map( _.ordinal ).contains( t.lowestOrdinal ) )
      .map( _.name )

    val aliasTypes = inputService.types.filter( _.alias.nonEmpty ).map( _.name )

    // Aggregate the return types with the parameter types.
    val typeNamesForRelease = (methodReturnTypes ++ methodParamNamesForRelease ++ aliasTypes).distinct

    // Now using the string names of the needed types, gather a list of TypeDefinition objects
    val typesForRelease = inputService.types.filter( typDef => typeNamesForRelease.contains( typDef.name ) )

    // Print the list of methods to be generated
    Console.println( "----------- Methods for release ----------------" )
    for (method <- methodsForRelease.sortBy( _.name )) {
      Console.println( s"${method.objectGroup.getOrElse("Common")} ${method.name}, ${method.release}" )
    }

    // Print the list of types to be generated
    Console.println( "----------- Types for release ------------------" )
    for (typ <- typeNamesForRelease.sortBy( t => t )) {
      Console.println( s"$typ" )
    }

    // Return a new service definition object with the methods and types filtered down.
    inputService.copy( methods = methodsForRelease, types = typesForRelease, release = ReleaseProcess.highestOrdinal( releaseLevels ), typeOrdinality = typeOrdinals )

  }

  // This function goes through all methods in the descriptor file, gets their return type, then
  // recurses into it to discover all types involved. Then it looks for another occurance of each type
  // from other known methods and sets the ordinal value of the type to the lowest possible.
  // This allows us to know what the lowest StabilityLevel each type is used for.
  def discoverOrdinality( serviceDefinition: ServiceDefinition ): List[TypeOrdinal] = {

    val ordinalTypes = new ListBuffer[TypeOrdinal]

    // iterate all the methods
    serviceDefinition.methods foreach (method => {
      // find the type of the return object
      val returnType = method.returnInfo.map( _.returnType.typeName ).getOrElse( throw new Exception( "Method has no returnInfo" ) )

      // find the return type in the types list then recurse through it and build a list of all types involved in this method
      val methodSubTypes = getTypeAndSubTypes( serviceDefinition, returnType )

      // iterate through all types and get or create a TypeOrdinal object
      methodSubTypes foreach (oneType => {
        val ordinalType = ordinalTypes.find( ot => ot.name == oneType ).getOrElse( {
          val newOrdType = TypeOrdinal( oneType, method.release.ordinal )
          ordinalTypes += newOrdType
          newOrdType
        } )
        Try( ordinalTypes -= ordinalType )
        // maybe set the ordinal value to a lower value
        val exactOrdinalType = if (method.release.ordinal < ordinalType.lowestOrdinal) {
          ordinalType.copy( lowestOrdinal = method.release.ordinal )
        }
        else {
          ordinalType
        }
        ordinalTypes += exactOrdinalType
      })
    })
    ordinalTypes.result( )
  }

  // Find the specified type in the types list then recurse through it and build a list of all types contained within it
  def getTypeAndSubTypes( serviceDefinition: ServiceDefinition, typeName: String ): List[String] = {
    (List( typeName ) ++ getTypesWithinType( serviceDefinition, typeName )).distinct
  }

  // This is the recursion function that will discover all types within a type.
  private def getTypesWithinType( serviceDefinition: ServiceDefinition, typeName: String ): List[String] = {
    val memberTypes: List[String] =
      (for (typeDef <- serviceDefinition.types.filter( t => t.name == typeName );
            member <- typeDef.members;
            dictionaryType = member.typeUse.dictionaryType
      ) yield dictionaryType.collect { case v: String => v } ++ List( member.typeUse.typeName ))
        .flatten

    val subTypes: List[String] =
      for (typeUse <- memberTypes;
           deeperType <- getTypesWithinType( serviceDefinition, typeUse )
      ) yield deeperType
    memberTypes ++ subTypes
  }

  // Used to build a list of types and their lowest ordinal value as defined by the method they are returned by.
  case class TypeOrdinal( name: String, lowestOrdinal: Int )

}