import _root_.sbtunidoc.Plugin.UnidocKeys._
import _root_.sbtunidoc.Plugin._
import com.typesafe.sbt.SbtGhPages.ghpages
import com.typesafe.sbt.SbtSite.site
import PgpKeys._
import sbtassembly.Plugin.AssemblyKeys._

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

ivyConfiguration <<= (externalResolvers, ivyPaths, offline, checksums, appConfiguration, target, streams) map { (rs, paths, off, check, app, t, s) =>
  val resCacheDir = t / "resolution-cache"
  new InlineIvyConfiguration(paths, rs, Nil, Nil, off, None, check, Some(resCacheDir), s.log)
}

jacoco.settings

logLevel := Level.Info

wartremoverErrors ++= Warts.allBut(Wart.NoNeedForMonad)

// To sync with Maven central, you need to supply the following information:
pomExtra in Global := Config.pomExtra

credentials += Credentials(Path.userHome / ".ivy2" / ".sonatype.credentials")

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
    publish := { },
    publishLocal := { },
    publishSigned := { },
    publishLocalSigned := { },
    publishArtifact := false,
    Keys.`package` := file( "" ),
    packageBin in Global := file( "" ),
    packageSrc in Global := file( "" ),
    packageDoc in Global := file( "" ),
    packagedArtifacts := Map( ),
    unidocProjectFilter in(ScalaUnidoc, unidoc) := inProjects( jsvcgenCore, jsvcgen, jsvcgenClientJava )
  )
  aggregate(
    jsvcgenCore,
    jsvcgen,
    jsvcgenClientJava,
    jsvcgenAssembly
  )).enablePlugins( CrossPerProjectPlugin, GitBranchPrompt )

lazy val jsvcgenCore = Project(
  id = "jsvcgen-core",
  base = file( "jsvcgen-core" ),
  settings = Config.settings ++ jacoco.settings ++ Seq(
    description := "Core library for jsvcgen.",
    crossPaths := true,
    libraryDependencies ++= Seq(
      Dependencies.json4sJackson
    )
  )
)

lazy val jsvcgen = Project(
  id = "jsvcgen",
  base = file( "jsvcgen" ),
  settings = Config.settings ++ templateSettings ++  jacoco.settings ++ Seq(
    description := "Code generator for JSON-RPC services.",
    crossPaths := true,
    libraryDependencies ++= Seq(
      Dependencies.scalateCore,
      Dependencies.scopt
    ),
    mainClass := Some( "com.solidfire.jsvcgen.codegen.Cli" )
  )

) dependsOn jsvcgenCore

lazy val jsvcgenAssembly = Project(
  id = "jsvcgen-assembly",
  base = file( "jsvcgen-assembly" ),
  settings = Config.settings ++ assemblySettings ++ Seq(
    description := "SBT plugin for easy code generation in an SBT project.",
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq( "2.10.6" ),
    crossPaths := false,
    libraryDependencies += Dependencies.slf4jSimple,
    mainClass := Some( "com.solidfire.jsvcgen.codegen.Cli" ),
    jarName in assembly := s"""jsvcgen-assembly-${version.value}.jar""",
    test in assembly := {},
    assemblyOption in packageDependency ~= { _.copy(appendContentHash = true) },
    assemblyOption in assembly ~= { _.copy(cacheUnzip = false) },
    assemblyOption in assembly ~= { _.copy(cacheOutput = false) }
  )
) settings (
  addArtifact(artifact in (Compile, assembly), assembly).settings: _*
) dependsOn jsvcgen % "compile"

lazy val jsvcgenClientJava = Project(
  id = "jsvcgen-client-java",
  base = file( "jsvcgen-client-java" ),
  settings = Config.settings ++ jacoco.settings ++ Seq(
    description := "Client library for JSON-RPC web services.",
    libraryDependencies ++= Seq(
      Dependencies.base64,
      Dependencies.gson,
      Dependencies.jodaTime,
      Dependencies.jodaConvert,
      Dependencies.junit
    ),
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq( "2.10.6" ),
    crossPaths := false, // do not append _${scalaVersion} to generated JAR
    autoScalaLibrary := false // do not add Scala libraries as a dependency
  )
)

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases" at nexus + "service/local/staging/deploy/maven2")
}

packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
  java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION -> version.value
)