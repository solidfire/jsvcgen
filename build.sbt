name := "jsvcgen"

exportJars := true

fork in run := true

lazy val jsvcgen = project in file(".") aggregate(
                                                  jsvcgenCore,
                                                  jsvcgenGenerate
                                                 )

lazy val jsvcgenCore = Project(
  id = "jsvcgen-core",
  base = file("jsvcgen-core"),
  settings = Config.settings ++ Seq(
      description := "Core library for jsvcgen.",
      libraryDependencies ++= Seq(Dependencies.json4sCore)
    )
)

lazy val jsvcgenGenerate = Project(
  id = "jsvcgen-generate",
  base = file("jsvcgen-generate"),
  settings = Config.settings ++ Seq(
      description := "Source code generator for jsvcgen.",
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
