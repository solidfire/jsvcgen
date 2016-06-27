import _root_.sbtunidoc.Plugin.UnidocKeys._
import _root_.sbtunidoc.Plugin._
import aQute.bnd.osgi.Constants
import com.typesafe.sbt.SbtGhPages.ghpages
import com.typesafe.sbt.SbtPgp.autoImportImpl.PgpKeys._
import com.typesafe.sbt.SbtSite.site
import com.typesafe.sbt.osgi.OsgiKeys._
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

fork in run := true

parallelExecution in Test := true

crossPaths in ThisBuild := true

ivyScala := ivyScala.value map {_.copy( overrideScalaVersion = true )}

ivyConfiguration <<= (externalResolvers, ivyPaths, offline, checksums, appConfiguration, target, streams) map { ( rs, paths, off, check, app, t, s ) =>
  val resCacheDir = t / "resolution-cache"
  new InlineIvyConfiguration( paths, rs, Nil, Nil, off, None, check, Some( resCacheDir ), s.log )
}

logLevel := Level.Info

wartremoverErrors ++= Warts.allBut( Wart.NoNeedForMonad )

credentials += Credentials( Path.userHome / ".ivy2" / ".sonatype.credentials" )

sonatypeProfileName := "com.solidfire"

pomExtra in Global := {
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

lazy val jsvcgenProject = (project in file( "." )
  settings (Config.projectSettings: _*)
  settings (unidocSettings: _*)
  settings (site.settings ++ ghpages.settings: _*)
  settings(
  name := "jsvcgen",
  site.addMappingsToSiteDir( mappings in(ScalaUnidoc, packageDoc), "latest/api" ),
  git.remoteRepo := "git@github.com:solidfire/jsvcgen.git"
  )
  settings(
  test := {},
  publish := {},
  publishLocal := {},
  publishSigned := {},
  publishLocalSigned := {},
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
  settings = Config.projectSettings ++ jacocoSettings ++ Seq(
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
  settings = Config.projectSettings ++ templateSettings ++ jacocoSettings ++ Seq(
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
  settings = Config.projectSettings ++ assemblySettings ++ Seq(
    description := "SBT plugin for easy code generation in an SBT project.",
    scalaVersion := "2.10.6",
    crossScalaVersions := Seq( "2.10.6" ),
    crossPaths := false,
    libraryDependencies += Dependencies.slf4jSimple,
    mainClass := Some( "com.solidfire.jsvcgen.codegen.Cli" ),
    jarName in assembly := s"""jsvcgen-assembly-${version.value}.jar""",
    test in assembly := {},
    assemblyOption in packageDependency ~= {_.copy( appendContentHash = true )},
    assemblyOption in assembly ~= {_.copy( cacheUnzip = false )},
    assemblyOption in assembly ~= {_.copy( cacheOutput = false )},
    excludedJars in assembly <<= (fullClasspath in assembly) map { cp =>
      cp.filter(jar => List( "slf4j-simple" ).map(jarName =>
        jar.data.getName.startsWith(jarName)).foldLeft(false)( _ || _ ) )
    }

  )
) settings (
           addArtifact( artifact in(Compile, assembly), assembly ).settings: _*
           ) dependsOn jsvcgen % "compile"

lazy val jsvcgenClientJava = Project(
  id = "jsvcgen-client-java",
  base = file( "jsvcgen-client-java" ),
  settings = Config.projectSettings ++ jacocoSettings ++ Seq(
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
    autoScalaLibrary := false, // do not add Scala libraries as a dependency
    description := "OSGi bundle for Jsvcgen Java Client.",
    OsgiKeys.bundleSymbolicName := "com.solidfire.jsvcgen.client",
    OsgiKeys.exportPackage := Seq( "com.solidfire.jsvcgen", "com.solidfire.jsvcgen.annotation", "com.solidfire.jsvcgen.client", "com.solidfire.jsvcgen.javautil", "com.solidfire.jsvcgen.serialization" ),
    OsgiKeys.additionalHeaders := Map(Constants.NOEE -> "true", Constants.REQUIRE_CAPABILITY -> ""),
    // Here we redefine the "package" task to generate the OSGi Bundle.
    Keys.`package` in Compile <<= OsgiKeys.bundle
  )
).settings(
  addArtifact(artifact in (Compile, OsgiKeys.bundle), OsgiKeys.bundle).settings: _*
).enablePlugins( SbtOsgi )

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some( "snapshots" at nexus + "content/repositories/snapshots" )
  else
    Some( "releases" at nexus + "service/local/staging/deploy/maven2" )
}

packageOptions in(Compile, packageBin) += Package.ManifestAttributes(
  java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION -> version.value
)