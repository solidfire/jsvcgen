import sbt.Keys._
import sbt._

object Config {
  lazy val compilerOptions = Seq(
                                 "-Xfatal-warnings",
                                 "-Xlint",
                                 "-feature",
                                 "-language:implicitConversions",
                                 "-deprecation",
                                 "-unchecked"
                                )

  lazy val org = "com.gockelhut"

  lazy val orgName = "Gockel Hut"

  lazy val settings = Defaults.defaultSettings ++ Seq(
    //populate default set of scalac options for each project
    scalacOptions ++= compilerOptions,
    scalaVersion := "2.11.2",
    version := Version.jsvcgen,
    organization := org,
    resolvers := repositories,
    libraryDependencies ++= Seq(
        Dependencies.scalatest % "test"
      )
  )

  lazy val repositories = List(
    "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
    "Maven Central" at "http://repo1.maven.org/maven2/"
  )

  lazy val junitReports = testOptions in Test <+= (target in Test) map { target =>
    val reportTarget = target / "test-reports"
    Tests.Argument(TestFrameworks.ScalaTest, s"""junitxml(directory="$reportTarget")""")
  }
}

object Version {
  //this project
  val jsvcgen = "0.1.1-SNAPSHOT"
  
  val gson      = "2.3"
  val json4s    = "3.2.10"
  val scalatest = "2.2.2"
  val scopt     = "3.2.0"
}

object Dependencies {
  lazy val gson          = "com.google.code.gson" %  "gson"           % Version.gson
  lazy val json4sCore    = "org.json4s"           %% "json4s-core"    % Version.json4s
  lazy val json4sJackson = "org.json4s"           %% "json4s-jackson" % Version.json4s
  lazy val scalatest     = "org.scalatest"        %% "scalatest"      % Version.scalatest
  lazy val scopt         = "com.github.scopt"     %% "scopt"          % Version.scopt
}
