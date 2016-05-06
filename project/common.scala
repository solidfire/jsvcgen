/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 **/

import de.johoop.jacoco4sbt.JacocoPlugin.jacoco
import sbt.Keys._
import sbt._
import com.typesafe.sbt.pgp._
import sbtrelease.ReleaseStateTransformations._
import sbtrelease.ReleasePlugin.autoImport._
import sbtrelease.{Version => SbtVersion}

object Config {
  lazy val javaCompilerOptions = Seq(
    "-Xlint"
  )

  val isJdk8 = System.getProperty( "java.version" ).startsWith( "1.8" )

  lazy val javadocOptions = if (isJdk8) Seq(
    "-Xdoclint:none"
  )
  else Seq( )

  lazy val allJavadocOptions = javadocOptions ++ Seq(
    "-noqualifier",
    "all",
    "-stylesheetfile",
    "jsvcgen/src/main/resources/javadoc.css",
    "-header",
    s"""<img><br/><b>jsvcgen</b><br/>v${Version.jsvcgen}"""
  )

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
    "-Ywarn-numeric-widen",
    "-Ywarn-value-discard",
    "-Xfuture"
  )

  lazy val pomExtra = {
    <url>https://github.com/solidfire/solidfire-sdk-java</url>
      <licenses>
        <license>
          <name>Apache 2</name>
          <url>http://www.apache.org/licenses/LICENSE-2.0.txt</url>
        </license>
      </licenses>
      <scm>
        <connection>scm:git:github.com/solidfire/jsvcgen</connection>
        <developerConnection>scm:git:git@github.com:solidfire/jsvcgen</developerConnection>
        <url>github.com/solidfire/jsvcgen</url>
      </scm>
      <developers>
        <developer>
          <id>jason-womack</id>
          <name>Jason Ryan Womack</name>
          <url>https://github.com/jason-womack</url>
        </developer>
      </developers>
  }

  lazy val org = "com.solidfire"

  lazy val orgName = "SolidFire, Inc."

  // create beautiful scala test report
  lazy val unitTestOptions = Seq(
    Tests.Argument(TestFrameworks.ScalaTest,"-h","target/html-unit-test-report"),
    Tests.Argument(TestFrameworks.ScalaTest,"-u","target/unit-test-reports"),
    Tests.Argument(TestFrameworks.ScalaTest,"-oD")
  )

  lazy val jacocoTestOptions = Seq(
    Tests.Argument(TestFrameworks.ScalaTest,"-h","target/html-unit-test-report"),
    Tests.Argument(TestFrameworks.ScalaTest,"-u","target/unit-test-reports"),
    Tests.Argument(TestFrameworks.ScalaTest,"-oD")
  )

  lazy val settings = Defaults.coreDefaultSettings ++ Seq(
    //populate default set of scalac options for each project
    javacOptions ++= javaCompilerOptions,
    javacOptions in doc := javadocOptions,
    scalacOptions ++= compilerOptions,
    testOptions in (Test, test) ++= unitTestOptions,
    testOptions in jacoco.Config ++= jacocoTestOptions,
    crossPaths := false,
    version := Version.jsvcgen,
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq( "2.10.6", "2.11.8" ),
    isSnapshot := version.value.trim.endsWith( "-SNAPSHOT" ),
    organization := org,
    resolvers := repositories,
    updateOptions := updateOptions.value.withCachedResolution(true),
    releaseVersionFile := file("project/version.sbt"),
    releaseVersionBump := SbtVersion.Bump.Next,
    releasePublishArtifactsAction := PgpKeys.publishSigned.value,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      tagRelease,
      setNextVersion,
      commitNextVersion,
      pushChanges
    ),
    libraryDependencies ++= Seq(
      Dependencies.slf4jApi,
      Dependencies.slf4jSimple,
      Dependencies.scalatest,
      Dependencies.pegdown,
      Dependencies.scalacheck,
      Dependencies.mockito
    )
  )

  lazy val repositories = List(
    "Typesafe" at "http://repo.typesafe.com/typesafe/releases/",
    "Maven Central" at "http://repo1.maven.org/maven2/"
  )
}

object Version {
  //this project
  val jsvcgen = "0.2.8-SNAPSHOT"

  val base64        = "2.3.9"
  val gson          = "2.6.2"
  val jodaConvert   = "1.8.1"
  val jodaTime      = "2.9.3"
  val json4s        = "3.3.0"
  val scalate       = "1.7.1"
  val scopt         = "3.4.0"
  val slf4j         = "1.6.6"
  val junit         = "4.12"
  val scalatest     = "2.2.6"
  val scalacheck    = "1.12.5"
  val pegdown       = "1.6.0"
  val mockito       = "1.10.19"
  val wiremock      = "1.58"
  val dispatch      = "0.11.3"
}

object Dependencies {
  lazy val base64        = "net.iharder"              %  "base64"               % Version.base64
  lazy val gson          = "com.google.code.gson"     %  "gson"                 % Version.gson
  lazy val jodaTime      = "joda-time"                %  "joda-time"            % Version.jodaTime
  lazy val jodaConvert   = "org.joda"                 %  "joda-convert"         % Version.jodaConvert
  lazy val json4sJackson = "org.json4s"               %% "json4s-jackson"       % Version.json4s force()
  lazy val scalateCore   = "org.scalatra.scalate"     %% "scalate-core"         % Version.scalate
  lazy val scopt         = "com.github.scopt"         %% "scopt"                % Version.scopt
  lazy val slf4jApi      = "org.slf4j"                %  "slf4j-api"            % Version.slf4j
  lazy val slf4jSimple   = "org.slf4j"                %  "slf4j-simple"         % Version.slf4j
  lazy val junit         = "junit"                    %  "junit"                % Version.junit       % "test"
  lazy val scalatest     = "org.scalatest"            %% "scalatest"            % Version.scalatest   % "test"
  lazy val pegdown       = "org.pegdown"              %  "pegdown"              % Version.pegdown     % "test"
  lazy val scalacheck    = "org.scalacheck"           %% "scalacheck"           % Version.scalacheck  % "test"
  lazy val mockito       = "org.mockito"              %  "mockito-all"          % Version.mockito     % "test"
  lazy val wiremock      = "com.github.tomakehurst"   %  "wiremock"             % Version.wiremock    % "test"
  lazy val dispatch      = "net.databinder.dispatch"  %% "dispatch-core"        % Version.dispatch    % "test"
}

import com.mojolly.scalate.ScalatePlugin._
import ScalateKeys._

object build extends Build {
  val templateSettings = scalateSettings ++ Seq(
    /**
     * Sets the behavior of recompiling template files.
     * Always template files are recompiled when this setting is true.
     * When you set it to false, they are recompiled only when the modified time of
     * a template file is newer than that of a scala file generated by compilation
     * or a compiled scala file corresponding to a template file doesn't exist yet.
     */
    scalateOverwrite := true,
    scalateTemplateConfig in Compile <<= baseDirectory { base =>
      Seq(
        /**
         * A minimal template configuration example.
         * "scalate" is used as a package prefix(the 4th argument of TemplateConfig.apply)
         * if not specified.
         *
         * An example of a scalate usage is as bellow if you have templates/index.ssp.
         *
         * val engine = new TemplateEngine
         * engine.layout("/scalate/index.ssp")
         */
        TemplateConfig(
          base / "src/main/resources/codegen",
          Seq(
            "import com.solidfire.jsvcgen._",
            "import com.solidfire.jsvcgen.codegen._"
          ),
          Nil,
          None
        )
      )
    }
  )

  lazy val root = Project("root", file(".")).settings(templateSettings:_*)
}
