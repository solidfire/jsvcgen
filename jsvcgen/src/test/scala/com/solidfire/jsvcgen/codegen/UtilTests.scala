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
package com.solidfire.jsvcgen.codegen

import com.solidfire.jsvcgen.codegen.Util.{ camelCase, underscores }
import org.scalatest._
import org.scalatest.prop.GeneratorDrivenPropertyChecks

import scala.util.Random._

object UtilTests

class UtilSpec extends FlatSpec with Matchers {

  "camelCase" should "format blank firstUpper=false" in {
    Util.camelCase( "", firstUpper = false ) should be( "" )
  }

  "camelCase" should "format blank firstUpper=true" in {
    Util.camelCase( "", firstUpper = true ) should be( "" )
  }

  "camelCase" should "format underscore firstUpper=false" in {
    Util.camelCase( "_", firstUpper = false ) should be( "" )
  }

  "camelCase" should "format underscore firstUpper=true" in {
    Util.camelCase( "_", firstUpper = true ) should be( "" )
  }

  "camelCase" should "format underscored with firstUpper=false" in {
    Util.camelCase( "some_method", firstUpper = false ) should be( "someMethod" )
  }

  "camelCase" should "format underscored with firstUpper=true" in {
    Util.camelCase( "some_method", firstUpper = true ) should be( "SomeMethod" )
  }

  "camelCase" should "format force first letter lower if firstUpper=false" in {
    Util.camelCase( "SomeMethod", firstUpper = false ) should be( "someMethod" )
  }

  "camelCase" should "leave a CamelCased string unchanged if firstUpper=true" in {
    Util.camelCase( "SomeMethod", firstUpper = true ) should be( "SomeMethod" )
  }

  "underscores" should "format blank" in {
    Util.underscores( "" ) should be( "" )
  }

  "underscores" should "format underscore" in {
    Util.underscores( "_" ) should be( "_" )
  }

  "underscores" should "format underscored from camel case" in {
    Util.underscores( "someMethod" ) should be( "some_method" )
  }

  "underscores" should "format underscored from capitalized" in {
    Util.underscores( "SomeMethod" ) should be( "some_method" )
  }
}

import org.scalacheck.Gen

object AsciiNamespaceGenerator {
  /** Generates an alphanumerical character with underscore */
  def alphaUnderscoreChar = Gen.frequency( (5, Gen.const( '_' )), (9, Gen.alphaChar) )

  val asciiNameGen: Gen[String] = for {
    s0 ← Gen.alphaChar
    s1 ← Gen.listOfN( nextInt( 20 ), alphaUnderscoreChar )
  } yield (s0 :: s1.toList).mkString
}

import com.solidfire.jsvcgen.codegen.AsciiNamespaceGenerator._

class UtilPropertiesTest extends PropSpec with GeneratorDrivenPropertyChecks with ShouldMatchers {


  property( "Test camelCase with first upper is true " ) {
    forAll( asciiNameGen ) { n: String => whenever( n.length > 1 ) {
      camelCase( n, firstUpper = true ).charAt( 0 ) should be( n.charAt( 0 ).toUpper )
    }
    }
  }

  property( "Test camelCase with first upper is false " ) {
    forAll( asciiNameGen ) { n: String => whenever( n.length > 1 ) {
      camelCase( n, firstUpper = false ).charAt( 0 ) should be( n.charAt( 0 ).toLower )
    }
    }
  }
  property( "Test camelCase length with first upper is true " ) {
    forAll( asciiNameGen ) { n: String => whenever( n.trim.length > 1 ) {
      camelCase( n, firstUpper = true ).length should be <= n.length
    }
    }
  }

  property( "Test camelCase length with first upper is false " ) {
    forAll( asciiNameGen ) { n: String => whenever( n.trim.length > 1 ) {
      camelCase( n, firstUpper = false ).length should be <= n.length
    }
    }
  }

  property( "Test underscores" ) {
    forAll( asciiNameGen ) { n: String => whenever( n.trim.length > 1 ) {
      underscores( n ).charAt( 0 ) should be( n.charAt( 0 ).toLower )
    }
    }
  }

  property( "Test underscores length" ) {
    forAll( asciiNameGen ) { n: String => whenever( n.trim.length > 1 ) {
      underscores( n ).length should be >= n.length
    }
    }
  }
}

