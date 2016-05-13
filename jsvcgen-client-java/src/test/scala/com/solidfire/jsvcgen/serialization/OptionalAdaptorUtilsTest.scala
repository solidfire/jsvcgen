package com.solidfire.jsvcgen.serialization

import com.solidfire.jsvcgen.JavaClasses._
import com.solidfire.jsvcgen.javautil.Optional
import com.solidfire.jsvcgen.serialization.OptionalAdaptorUtils._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by Jason Ryan Womack on 5/11/16.
  */
class OptionalAdaptorUtilsTest extends WordSpec with Matchers {

  "initializeAllNullOptionalFieldsAsEmpty" should {
    "do nothing with object with no optional fields" in {
      initializeAllNullOptionalFieldsAsEmpty( new Object ) should not be null
    }
    "do nothing with a custom type with no optional fields" in {
      initializeAllNullOptionalFieldsAsEmpty( new A ) should not be null
    }
    "do nothing with an array custom type with no optional fields" in {
      initializeAllNullOptionalFieldsAsEmpty( Array( new A ) ) should not be null
    }
    "set optional field with a simple object with one optional field" in {
      initializeAllNullOptionalFieldsAsEmpty( new B( null ) ).getOptional should be( Optional.empty )
    }
    "set optional field with an array of simple objects with one optional field" in {
      initializeAllNullOptionalFieldsAsEmpty( Array( new B( null ) ) )( 0 ).getOptional should be( Optional.empty )
    }
    "return true with complex object with a nested optional field" in {
      initializeAllNullOptionalFieldsAsEmpty( new C( new B( null ) ) ).getB.getOptional should be( Optional.empty )
    }
  }

  "hasOptionalFields" should {
    "return false with null object" in {
      hasOptionalFields( null ) shouldBe false
    }
    "return false with object with no optional fields" in {
      hasOptionalFields( new Object ) shouldBe false
    }
    "return false with a custom type with no optional fields" in {
      hasOptionalFields( new A ) shouldBe false
    }
    "return false with complex object with an empty array of types with optionals" in {
      hasOptionalFields( new D( Array( ) ) ) shouldBe false
    }
    "return true with a simple object with one optional field" in {
      hasOptionalFields( new B( Optional.of( "String" ) ) ) shouldBe true
    }
    "return true with complex object with a nested optional field" in {
      hasOptionalFields( new C( new B( Optional.of( "String" ) ) ) ) shouldBe true
    }
    "return true with complex object with an array of optional fields" in {
      hasOptionalFields( new D( Array( new B( null ) ) ) ) shouldBe true
    }

  }

  "getOptionalFields" should {
    "return empty map with null object" in {
      getOptionalFields( null ) should have size 0
    }
    "return empty map with object with no optional fields" in {
      getOptionalFields( new Object ) should have size 0
    }
    "return empty map with a custom type with no optional fields" in {
      getOptionalFields( new A ) should have size 0
    }
    "return empty map with complex object with a an empty array with nested optional field" in {
      getOptionalFields( new D( Array( ) ) ) should have size 0
    }
    "return one element map with a simple object with one optional field" in {
      getOptionalFields( new B( null ) ) should have size 1
    }
    "return one element map with complex object with a nested optional field" in {
      getOptionalFields( new C( new B( null ) ) ) should have size 1
    }
    "return one element map with complex object with a an array with nested optional field" in {
      getOptionalFields( new D( Array( new B( null ) ) ) ) should have size 1
    }
  }
}
