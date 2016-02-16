package com.solidfire.jsvcgen.model

/**
  * Created by Jason Ryan Womack on 2/12/16.
  */
trait Typed {
  val name:           String
  val typeUse:        TypeUse
  val documentation:  Option[Documentation]
}
