import sbt.Keys._
import sbt._

object Config {
  lazy val compilerOptions = Seq(
    "-deprecation",
    "-encoding", "UTF-8", // yes, this is 2 args
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-unchecked",
    "-Xfatal-warnings",
    "-Xlint",
    "-Yno-adapted-args",
    "-Ywarn-dead-code", // N.B. doesn't work well with the ??? hole
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
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
      Dependencies.scalatest      % "test",
      Dependencies.scalacheck     % "test",
      Dependencies.mockito        % "test"
    )
  )

  lazy val repositories = List(
    "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
    "Maven Central" at "http://repo1.maven.org/maven2/"
  )

  lazy val junitReports = testOptions in Test <+= (target in Test) map { target =>
    val reportTarget = target / "test-reports"
    Tests.Argument( TestFrameworks.ScalaTest, s"""junitxml(directory="$reportTarget")""" )
  }
}

object Version {
  //this project
  val jsvcgen = "0.1.9-SNAPSHOT"

  val gson       = "2.3"
  val json4s     = "3.2.11"
  val scalate    = "1.7.0"
  val scopt      = "3.2.0"
  val logback    = "1.0.13"
  val slf4j      = "1.7.12"
  val junit      = "4.11"
  val scalatest  = "2.2.5"
  val scalacheck = "1.12.2"
  val mockito    = "1.9.5"
  val wiremock   = "1.56"
  val dispatch   = "0.11.3"
}

object Dependencies {
  lazy val gson          = "com.google.code.gson"     % "gson"            % Version.gson
  lazy val json4sCore    = "org.json4s"               %% "json4s-core"    % Version.json4s
  lazy val json4sJackson = "org.json4s"               %% "json4s-jackson" % Version.json4s
  lazy val scalateCore   = "org.scalatra.scalate"     %% "scalate-core"   % Version.scalate
  lazy val scopt         = "com.github.scopt"         %% "scopt"          % Version.scopt
  lazy val logback       = "ch.qos.logback"           % "logback-classic" % Version.logback
  lazy val slf4j_simple  = "org.slf4j"                % "slf4j-simple"    % Version.slf4j
  lazy val junit         = "junit"                    % "junit"           % Version.junit
  lazy val scalatest     = "org.scalatest"            %% "scalatest"      % Version.scalatest
  lazy val scalacheck    = "org.scalacheck"           %% "scalacheck"     % Version.scalacheck
  lazy val mockito       = "org.mockito"              % "mockito-all"     % Version.mockito
  lazy val wiremock      = "com.github.tomakehurst"   % "wiremock"        % Version.wiremock
  lazy val dispatch      = "net.databinder.dispatch"  %% "dispatch-core"  % Version.dispatch
}
