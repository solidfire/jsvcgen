package com.solidfire.jsvcgen.serialization

import java.lang.Boolean

import com.solidfire.jsvcgen.JavaClasses
import com.solidfire.jsvcgen.JavaClasses._
import com.solidfire.jsvcgen.javautil.Optional
import com.solidfire.jsvcgen.serialization.OptionalAdaptorUtils._
import org.scalatest.{Matchers, WordSpec}

/**
  * Created by Jason Ryan Womack on 5/11/16.
  */
class OptionalAdaptorUtilsTest extends WordSpec with Matchers {

  "initializeAllNullOptionalFieldsAsEmpty" should {
    "return empty array with object with no optional fields" in {
      initializeAllNullOptionalFieldsAsEmpty( new Object ) should not be null
    }

    "return empty array with a custom type with no optional fields" in {
      initializeAllNullOptionalFieldsAsEmpty( new A ) should not be null
    }

    "return one element array with a simple object with one optional field" in {
      initializeAllNullOptionalFieldsAsEmpty( new B( null ) ).optional should be (Optional.empty)
    }

    "return true with complex object with a nested optional field" in {
      initializeAllNullOptionalFieldsAsEmpty( new C( new B( null ) ) ).b.optional should be (Optional.empty)
    }


  }

  "implementsSerializable" should {
    "return false with null object" in {
      implementsSerializable( null ) shouldBe false
    }

    "return false with a non serializable type" in {
      implementsSerializable( new Object ) shouldBe false
    }

    "return false with a custom type that does not implement Serializable" in {
      implementsSerializable( new A ) shouldBe false
    }

    "return true with a primitive type" in {
      implementsSerializable( Boolean.TRUE ) shouldBe true
    }

    "return true with a custom type that implement Serializable" in {
      implementsSerializable( new AA ) shouldBe true
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

    "return true with a simple object with one optional field" in {
      hasOptionalFields( new B( Optional.of( "String" ) ) ) shouldBe true
    }

    "return true with complex object with a nested optional field" in {
      hasOptionalFields( new C( new B( Optional.of( "String" ) ) ) ) shouldBe true
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

    "return one element map with a simple object with one optional field" in {
      getOptionalFields( new B( Optional.of( "String" ) ) ) should have size 1
    }

    "return one element map with complex object with a nested optional field" in {
      getOptionalFields( new C( new B( Optional.of( "String" ) ) ) ) should have size 1
    }

  }

}
