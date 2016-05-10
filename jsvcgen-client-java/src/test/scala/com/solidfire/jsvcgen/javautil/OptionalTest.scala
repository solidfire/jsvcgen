package com.solidfire.jsvcgen.javautil

import org.scalatest.{ Matchers, WordSpec }

/**
 * Created by Jason Ryan Womack on 7/20/15.
 */
object Opt {
  val a = Optional.of("A")
  val b = Optional.of("B")
}

class OptionalTest extends WordSpec with Matchers{

  "Optional[String]" should {
    "be equal to itself" in {
      Opt.a should be (Opt.a)
    }
    "be equal to another of itself" in {
      Opt.a should be (Optional.of("A"))
    }
    "not be equal to another different from itself" in {
      Opt.a should not be Opt.b
    }
    "not be equal to Optional of itself" in {
      Opt.a should not be Optional.of(Opt.a)
    }
    "not be equal to Optional.toString of itself" in {
      Opt.a should not be Optional.of(Opt.a.toString)
    }
  }
}
