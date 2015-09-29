package com.solidfire.jsvcgen.model


trait Attribute {
  val name: String
  val since: Option[String]
  val deprecated: Option[Deprecated]
  val documentation: Option[Documentation]
}
