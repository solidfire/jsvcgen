import _root_.sbtunidoc.Plugin.UnidocKeys._
import _root_.sbtunidoc.Plugin._
import com.typesafe.sbt.SbtGhPages.ghpages
import com.typesafe.sbt.SbtSite.site

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
name := "jsvcgen"

exportJars := true

fork in run := false

parallelExecution in Test := false

crossPaths in ThisBuild := true

ivyScala := ivyScala.value map {_.copy( overrideScalaVersion = true )}

logLevel := Level.Info

wartremoverErrors ++= Warts.all

lazy val jsvcgenProject = (project in file( "." )
  settings (Config.settings: _*)
  settings (unidocSettings: _*)
  settings (site.settings ++ ghpages.settings: _*)
  settings(
    name := "jsvcgen",
    site.addMappingsToSiteDir( mappings in(ScalaUnidoc, packageDoc), "latest/api" ),
    git.remoteRepo := "git@github.com:solidfire/jsvcgen.git"
  )
  settings(
    test := {},
    publishArtifact := false,
    Keys.`package` := file( "" ),
    packageBin in Global := file( "" ),
    packagedArtifacts := Map( ),
    unidocProjectFilter in(ScalaUnidoc, unidoc) := inProjects( jsvcgenCore, jsvcgen, jsvcgenClientJava )
  )
  aggregate(
    jsvcgenCore,
    jsvcgen,
    jsvcgenClientJava,
    jsvcgenPluginSbt
  )).enablePlugins( GitVersioning, GitBranchPrompt, SbtNativePackager )


lazy val jsvcgenCore = Project(
  id = "jsvcgen-core",
  base = file( "jsvcgen-core" ),
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
  base = file( "jsvcgen" ),
  settings = Config.settings ++ templateSettings ++ assemblySettings ++ jacoco.settings ++ Seq(
    description := "Code generator for JSON-RPC services.",
    libraryDependencies ++= Seq(
      Dependencies.json4sJackson,
      Dependencies.scalateCore,
      Dependencies.scopt
    ),
    mainClass := Some( "com.solidfire.jsvcgen.codegen.Cli" )
  )
) dependsOn (
            jsvcgenCore % "compile;test->test"
            )

lazy val jsvcgenPluginSbt = Project(
  id = "jsvcgen-plugin-sbt",
  base = file( "jsvcgen-plugin-sbt" ),
  settings = Config.settings ++ Seq(
    description := "SBT plugin for easy code generation in an SBT project.",
    sbtPlugin := true
  )
) dependsOn (
            jsvcgen % "compile"
            )

lazy val jsvcgenClientJava = Project(
  id = "jsvcgen-client-java",
  base = file( "jsvcgen-client-java" ),
  settings = Config.settings ++ jacoco.settings ++ Seq(
    description := "Client library for JSON-RPC web services.",
    libraryDependencies ++= Seq(
      Dependencies.gson,
      Dependencies.junit % "test",
      Dependencies.wiremock % "test",
      Dependencies.dispatch % "test"
    ),
    crossPaths := false, // do not append _${scalaVersion} to generated JAR
    autoScalaLibrary := false // do not add Scala libraries as a dependency
  )
)

packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
  java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION -> version.value
)

