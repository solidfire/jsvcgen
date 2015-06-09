package com.solidfire.jsvcgen.model

object Util {
  val namespaceRegEx = "^([a-zA-Z_][a-zA-Z_0-9]*)(\\.([a-zA-Z_][a-zA-Z_0-9]*))*$".r

  def validateNamespace( input: String ) = {
    if (input.isEmpty)
      throw new ValidationException("Namespace cannot be empty")

    if (!isValidNamespace(input))
      throw new ValidationException("Malformed namespace \"" + input + "\"")
  }

  def isValidNamespace( input: String) = {
    namespaceRegEx.findAllIn(input).size == 1
  }
}
