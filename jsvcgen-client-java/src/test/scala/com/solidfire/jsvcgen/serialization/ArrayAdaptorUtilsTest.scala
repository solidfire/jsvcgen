/*
 * Copyright &copy 2014-2016 NetApp, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.solidfire.jsvcgen.serialization

import java.util

import com.solidfire.jsvcgen.JavaClasses._
import com.solidfire.jsvcgen.serialization.ArrayAdaptorUtils._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by Jason Ryan Womack on 5/11/16.
  */
class ArrayAdaptorUtilsTest extends WordSpec with Matchers {

  "convertAllArrayListsToArrays" should {
    "do nothing with object with no optional fields" in {
      convertAllArrayListsToArrays( new Object ) should not be null
    }
    "do nothing with a custom type with no optional fields" in {
      convertAllArrayListsToArrays( new A ) should not be null
    }
    "convert an array list type to an array" in {
      val map = new FooMap( )
      val array = new util.ArrayList[String]( )
      map.put( "array", array )
      convertAllArrayListsToArrays( map ).get( "array" ).getClass.isArray should be( true )
    }
    "convert array list and preserve values" in {
      val map = new FooMap( )
      val array = new util.ArrayList[String]( )
      array.add( "One" )
      map.put( "array", array )
      convertAllArrayListsToArrays( map ).get( "array" ).asInstanceOf[Array[Object]]( 0 ) should be( "One" )
    }
    "convert nested array lists and preserve values" in {
      val map = new FooMap( )
      val map1 = new FooMap( )
      val array = new util.ArrayList[String]( )
      array.add( "One" )
      map1.put( "array", array )
      map.put( "map1", map1 )
      convertAllArrayListsToArrays( map ).get( "map1" ).asInstanceOf[util.HashMap[String, Object]].get( "array" ).asInstanceOf[Array[Object]]( 0 ) should be( "One" )
    }
  }

  "hasMapFields" should {
    "return false with null object" in {
      hasMapFields( null ) shouldBe false
    }
    "return false with object with no fields" in {
      hasMapFields( new Object ) shouldBe false
    }
    "return false with a custom type with no map fields" in {
      hasMapFields( new A ) shouldBe false
    }
    "return false with complex object with an empty array of maps" in {
      hasMapFields( new E( new Array[FooMap]( 0 ) ) ) shouldBe false
    }
    "return true with a map" in {
      hasMapFields( new FooMap( ) ) shouldBe true
    }
    "return true with complex object with an array of maps" in {
      hasMapFields( new E( Array( new FooMap( ) ) ) ) shouldBe true
    }
    "return false with complex object with a null nested maps" in {
      hasMapFields( new ComplexFooMap( null, null, null ) ) shouldBe false
    }
    "return true with complex object with a empty nested map [String -> String]" in {
      hasMapFields( new ComplexFooMap( new util.HashMap[String, String]( ), null, null ) ) shouldBe true
    }
    "return true with complex object with a empty nested map [String -> Array[String]]" in {
      hasMapFields( new ComplexFooMap( null, new util.HashMap[String, Array[String]]( ), null ) ) shouldBe true
    }
    "return true with complex object with a empty nested map [String -> Object]" in {
      hasMapFields( new ComplexFooMap( null, null, new util.HashMap[String, Object]( ) ) ) shouldBe true
    }
  }

  "getAllMaps" should {
    "return 0 with a null object" in {
      getAllMaps( null ) should have size 0
    }
    "return 0 with object with no map fields" in {
      getAllMaps( new Object ) should have size 0
    }
    "return 0 with a custom type with no map fields" in {
      getAllMaps( new A ) should have size 0
    }
    "return 1 with an empty map" in {
      getAllMaps( new FooMap( ) ) should have size 1
    }
    "return 1 with complex object with a empty nested map [String -> String]" in {
      getAllMaps( new ComplexFooMap( new util.HashMap[String, String]( ), null, null ) ) should have size 1
    }
    "return 1 with complex object with a empty nested map [String -> Array[String]]" in {
      getAllMaps( new ComplexFooMap( null, new util.HashMap[String, Array[String]]( ), null ) ) should have size 1
    }
    "return 1 with complex object with a empty nested map [String -> Object]" in {
      getAllMaps( new ComplexFooMap( null, null, new util.HashMap[String, Object]( ) ) ) should have size 1
    }
    "return 3 with complex object with 3 empty nested maps" in {
      getAllMaps( new ComplexFooMap( new util.HashMap[String, String]( ), new util.HashMap[String, Array[String]]( ), new util.HashMap[String, Object]( ) ) ) should have size 3
    }

  }
}
