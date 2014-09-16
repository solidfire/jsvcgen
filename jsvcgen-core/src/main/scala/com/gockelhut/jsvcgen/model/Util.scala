package com.gockelhut.jsvcgen.model

import scala.util.matching.Regex

object Util {
  def validateNamespace(input: String) = {
    if (input.size == 0)
      throw new ValidationException("Namespace cannot be empty")
    
    val re = "^([a-zA-Z_][a-zA-Z_0-9]*)(\\.([a-zA-Z_][a-zA-Z_0-9]*))*$".r
    if (re.findAllIn(input).size != 1)
      throw new ValidationException("Malformed namespace \"" + input + "\"")
  }
}
