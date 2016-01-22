package com.solidfire.jsvcgen.codegen

import org.scalatest.{Matchers, WordSpec}

class CliTest extends WordSpec with Matchers {

  "Cli" should {
    "Create default CliConfig" in {

      val args: Seq[String] = List(
        "jsvcgen somefile.json --generator csharp --namespace org.some.namespace"
      )

      val maybeConfig: Option[CliConfig] = Cli.getParser.parse(args, CliConfig())

    }
  }

}
