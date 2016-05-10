package com.solidfire.jsvcgen.serialization

import com.google.gson.GsonBuilder
import com.solidfire.jsvcgen.javautil.Optional
import org.scalatest.{ Matchers, WordSpec }

class OptionalAdapterTest extends WordSpec with Matchers {

  private val gson = new GsonBuilder( )
    .registerTypeAdapter( OptionalAdapter.serializingClass( ), new OptionalAdapter( ) )
    .create( )

  class TestString {
    var optional: Optional[String] = Optional.of( "initial_string" )
  }

  class TestString2 {
    var optional1: Optional[String] = Optional.of( "initial_string" )
    var optional2: Optional[String] = Optional.of( "initial_string" )
  }

  class TestInt {
    var optional: Optional[Int] = Optional.of( 2 )
  }

  class TestComplex {
    var optional: Optional[DummyObj] = Optional.empty( )
  }

  class DummyObj {
    var str: String = "initial_string"
    var int: Int    = 2
  }

  "OptionalAdapter" should {
    "decode a properly formatted Optional<String>" in {
      val input = "{\"optional\":\"string\"}"
      val output = gson.fromJson( input, classOf[TestString] )
      output.optional.get( ) should be( "string" )
    }


    "decode a properly formatted Optional<String> with empty value" in {
      val input = "{\"optional\":\"\"}"
      val output = gson.fromJson( input, classOf[TestString] )
      output.optional.isPresent should be( false )
    }

    "decode a properly formatted Optional<int>" in {
      val input = "{\"optional\":1}"
      val output = gson.fromJson( input, classOf[TestInt] );
      output.optional.get( ) should be( 1 )
    }

    "decode a properly formatted Optional<[ComplexType]>" in {
      val input = "{\"optional\":{\"str\":\"string\",\"int\":1}}"
      val output = gson.fromJson( input, classOf[TestComplex] )
      output.optional.get( ).str should be( "string" )
      output.optional.get( ).int should be( 1 )
    }

    "encode a properly formatted Optional<String>" in {
      val input = new TestString( )
      val output = gson.toJson( input )
      output should be( "{\"optional\":\"initial_string\"}" )
    }

    "encode an object with an unset Optional" in {
      val input = new TestString2( )
      input.optional2 = Optional.empty( )
      val output = gson.toJson( input )
      output should be( "{\"optional1\":\"initial_string\"}" )
    }

    "encode a valid complex object" in {
      val obj = new DummyObj( )
      val input = new TestComplex( )
      input.optional = Optional.of( obj )
      val output = gson.toJson( input )
      output should be( "{\"optional\":{\"str\":\"initial_string\",\"int\":2}}" )
    }
  }
}