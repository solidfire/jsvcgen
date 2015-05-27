package com.solidfire.jsvcgen.model

object Util {
  def validateNamespace( input: String ) = {
    if (input.isEmpty)
      throw new ValidationException("Namespace cannot be empty")

    val re = "^([a-zA-Z_][a-zA-Z_0-9]*)(\\.([a-zA-Z_][a-zA-Z_0-9]*))*$".r
    if (re.findAllIn(input).size != 1)
      throw new ValidationException("Malformed namespace \"" + input + "\"")
  }
}
