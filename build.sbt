
name := "jsvcgen"

exportJars := true

fork in run := false

parallelExecution in Test := false

crossPaths in ThisBuild := true

ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

logLevel := Level.Info

wartremoverErrors ++= Warts.all

lazy val jsvcgenProject = (project in file(".")
  settings(Config.settings: _*)
  aggregate(
    jsvcgenCore,
    jsvcgen,
    jsvcgenClientJava,
    jsvcgenPluginSbt
  ))


lazy val jsvcgenCore = Project(
  id = "jsvcgen-core",
  base = file("jsvcgen-core"),
  settings = Config.settings ++ jacoco.settings ++ Seq(
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
  settings = Config.settings ++ templateSettings ++ assemblySettings ++ jacoco.settings ++ Seq(
      description := "Code generator for JSON-RPC services.",
      libraryDependencies ++= Seq(
          Dependencies.json4sJackson,
          Dependencies.scalateCore,
          Dependencies.scopt
        ),
      mainClass := Some("com.solidfire.jsvcgen.codegen.Cli")
    )
) dependsOn(
  jsvcgenCore % "compile;test->test"
)

lazy val jsvcgenPluginSbt = Project(
  id = "jsvcgen-plugin-sbt",
  base = file("jsvcgen-plugin-sbt"),
  settings = Config.settings ++ Seq(
      description := "SBT plugin for easy code generation in an SBT project.",
      sbtPlugin := true
    )
) dependsOn(
  jsvcgen % "compile"
)

lazy val jsvcgenClientJava = Project(
  id = "jsvcgen-client-java",
  base = file("jsvcgen-client-java"),
  settings = Config.settings ++ jacoco.settings ++ Seq(
      description := "Client library for JSON-RPC web services.",
      libraryDependencies ++= Seq(
        Dependencies.gson,
        Dependencies.junit    % "test",
        Dependencies.wiremock % "test",
        Dependencies.dispatch % "test"
      ),
      crossPaths := false,      // do not append _${scalaVersion} to generated JAR
      autoScalaLibrary := false // do not add Scala libraries as a dependency
    )
)

packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
  java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION -> version.value
)
