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

  lazy val org = "com.solidfire"

  lazy val orgName = "SolidFire"

  lazy val settings = Defaults.coreDefaultSettings ++ Seq(
    //populate default set of scalac options for each project
    scalacOptions ++= compilerOptions,
    crossPaths in ThisBuild := true,
    crossScalaVersions := Seq( "2.10.5", "2.11.5" ),
    version := Version.jsvcgen,
    organization := org,
    resolvers := repositories,
    libraryDependencies ++= Seq(
      Dependencies.slf4j_simple,
      Dependencies.scalatest % "test"
    )
  )

  lazy val repositories = List(
    "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
    "Maven Central" at "http://repo1.maven.org/maven2/"
  )

  lazy val junitReports = testOptions in Test <+= (target in Test) map { target ⇒
    val reportTarget = target / "test-reports"
    Tests.Argument( TestFrameworks.ScalaTest, s"""junitxml(directory="$reportTarget")""" )
  }
}

object Version {
  //this project
  val jsvcgen = "0.1.9-SNAPSHOT"

  val gson      = "2.3"
  val json4s    = "3.2.10"
  val junit     = "4.11"
  val scalate   = "1.7.0"
  val scalatest = "2.2.2"
  val scopt     = "3.2.0"
  val logback   = "1.0.13"
  val slf4j     = "1.7.12"
}

object Dependencies {
  lazy val gson          = "com.google.code.gson" % "gson" % Version.gson
  lazy val json4sCore    = "org.json4s" %% "json4s-core" % Version.json4s
  lazy val json4sJackson = "org.json4s" %% "json4s-jackson" % Version.json4s
  lazy val junit         = "junit" % "junit" % Version.junit
  lazy val scalateCore   = "org.scalatra.scalate" %% "scalate-core" % Version.scalate
  lazy val scalatest     = "org.scalatest" %% "scalatest" % Version.scalatest
  lazy val scopt         = "com.github.scopt" %% "scopt" % Version.scopt
  lazy val logback      = "ch.qos.logback" % "logback-classic" % Version.logback
  lazy val slf4j_simple  = "org.slf4j" % "slf4j-simple" % Version.slf4j
}
