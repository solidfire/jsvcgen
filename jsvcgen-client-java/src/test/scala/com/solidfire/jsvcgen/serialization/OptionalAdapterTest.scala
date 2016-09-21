package com.solidfire.jsvcgen.serialization

import com.google.gson.GsonBuilder
import com.solidfire.jsvcgen.javautil.Optional
import org.scalatest.{Matchers, WordSpec}


class OptionalAdapterTest extends WordSpec with Matchers {

  private val gson = new GsonBuilder( )
    .registerTypeAdapter( OptionalAdapter.serializingClass( ), new OptionalAdapter( ) )
    .create( )

  class TestString {
    var optional: Optional[java.lang.String] = Optional.of( "initial_string" )
  }

  class TestString2 {
    var optional1: Optional[java.lang.String] = Optional.of( "initial_string" )
    var optional2: Optional[java.lang.String] = Optional.of( "initial_string" )
  }

  class TestInt {
    var optional: Optional[java.lang.Integer] = Optional.of( 2 )
  }

  class TestComplex {
    var optional: Optional[DummyObj] = Optional.empty( )
  }

  class TestComplexOptional {
    var optional: Optional[DummyOptionalObj] = Optional.empty( )
  }

  case class DummyObj( str: java.lang.String, int: java.lang.Integer )

  case class DummyOptionalObj(
                               str: Optional[java.lang.String] = Optional.empty( ),
                               int: Optional[java.lang.Integer] = Optional.empty( )
                             )

  "OptionalAdapter" should {
    "decode a properly formatted Optional<String>" in {
      val input = "{'optional':'string'}"
      val output = gson.fromJson( input, classOf[TestString] )
      output.optional.get( ) should be( "string" )
    }

    "decode a properly formatted Optional<String> with empty value" in {
      val input = "{'optional':''}"
      val output = gson.fromJson( input, classOf[TestString] )
      output.optional should be( Optional.of("") )
    }

    "decode a properly formatted Optional<int>" in {
      val input = "{'optional':1}"
      val output = gson.fromJson( input, classOf[TestInt] );
      output.optional.get( ) should be( 1 )
    }

    "decode a properly formatted Optional<[TestComplex]>" in {
      val input = "{'optional':{'str':'string','int':1}}"
      val output = gson.fromJson( input, classOf[TestComplex] )
      output.optional.get( ).str should be( "string" )
      output.optional.get( ).int should be( 1 )
    }

    "decode a properly formatted Optional<[TestComplex]> with empty values" in {
      val input = "{'optional':{'str':'','int':null}}"
      val output = gson.fromJson( input, classOf[TestComplex] )
      output.optional.get( ).str should be( "" )
      output.optional.get( ).int should be( null )
    }

    "decode a properly formatted Optional<[TestComplexOptional]>" in {
      val input = "{'optional':{'str':'string','int':1}}"
      val output = gson.fromJson( input, classOf[TestComplexOptional] )
      output.optional.get( ).str should be( Optional.of( "string" ) )
      output.optional.get( ).int should be( Optional.of( 1 ) )
    }

    "decode a properly formatted Optional<[TestComplexOptional]> with empty values" in {
      val input = "{'optional':{'str':'','int':''}}"
      val output = gson.fromJson( input, classOf[TestComplexOptional] )
      output.optional.get( ).str should be( Optional.of("") )
      output.optional.get( ).int should be( Optional.empty( ) )
    }

    "decode a properly formatted Optional<[TestComplexOptional]> with no values" in {
      val input = "{'optional':{'str':null,'int':null}}"
      val output = gson.fromJson( input, classOf[TestComplexOptional] )
      output.optional.get( ).str should be( Optional.empty() )
      output.optional.get( ).int should be( Optional.empty( ) )
    }

    "decode a properly formatted Optional<[TestComplexOptional]> with no properties" in {
      val input = "{'optional':{ }}"
      val output = gson.fromJson( input, classOf[TestComplexOptional] )
      output.optional.get( ).str should be( Optional.empty() )
      output.optional.get( ).int should be( Optional.empty( ) )
    }

    "decode a properly formatted DummyOptionalObj with empty values" in {
      val input = "{'str':'','int':''}"
      val output = gson.fromJson( input, classOf[DummyOptionalObj] )
      output.str should be( Optional.of("") )
      output.int should be( Optional.empty( ) )
    }

    "decode a properly formatted DummyOptionalObj with no values" in {
      val input = "{'str':null,'int':null}"
      val output = gson.fromJson( input, classOf[DummyOptionalObj] )
      output.str should be( null )
      output.int should be( null )
    }

    "decode a properly formatted DummyOptionalObj with no properties" in {
      val input = "{}"
      val output = gson.fromJson( input, classOf[DummyOptionalObj] )
      output.str should be( null )
      output.int should be( null )
    }

    "encode a properly formatted Optional<String>" in {
      val input = new TestString( )
      val output = gson.toJson( input )
      output should be( """{"optional":"initial_string"}""" )
    }

    "encode an object with an unset Optional" in {
      val input = new TestString2( )
      input.optional2 = Optional.empty( )
      val output = gson.toJson( input )
      output should be( """{"optional1":"initial_string"}""" )
    }

    "encode a valid complex object" in {
      val obj = new DummyObj( "initial_string", 2 )
      val input = new TestComplex( )
      input.optional = Optional.of( obj )
      val output = gson.toJson( input )
      output should be( """{"optional":{"str":"initial_string","int":2}}""" )
    }
  }
}