name := "jsvcgen"

exportJars := true

fork in run := true

lazy val jsvcgenProject = project in file(".") aggregate(
                                                         jsvcgenCore,
                                                         jsvcgen
                                                        )

lazy val jsvcgenCore = Project(
  id = "jsvcgen-core",
  base = file("jsvcgen-core"),
  settings = Config.settings ++ Seq(
      description := "Core library for jsvcgen.",
      libraryDependencies ++= Seq(
        Dependencies.json4sCore,
        Dependencies.json4sJackson % "test"
      )
    )
)

lazy val jsvcgen = Project(
  id = "jsvcgen",
  base = file("jsvcgen"),
  settings = Config.settings ++ Seq(
      description := "Code generator for JSON-RPC services.",
      libraryDependencies ++= Seq(
          Dependencies.json4sJackson,
          Dependencies.scopt
        ),
      mainClass := Some("com.gockelhut.jsvcgen.generate.Cli")
    )
) dependsOn(
  jsvcgenCore % "compile;test->test"
)

packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
  java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION -> version.value
)
