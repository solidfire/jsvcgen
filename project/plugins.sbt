logLevel:= Level.Warn

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.6")

addSbtPlugin("org.scoverage" % "sbt-scoverage" % "1.3.3")

libraryDependencies += "org.jacoco" % "org.jacoco.core" % "0.7.5.201505241946"

libraryDependencies += "org.jacoco" % "org.jacoco.agent" % "0.7.5.201505241946"

libraryDependencies += "org.jacoco" % "org.jacoco.report" % "0.7.5.201505241946"

addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.13")

addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.4")

addSbtPlugin("com.eed3si9n" % "sbt-unidoc" % "0.3.3")

addSbtPlugin("com.typesafe.sbt" % "sbt-site" % "0.8.1")

addSbtPlugin("com.typesafe.sbt" % "sbt-ghpages" % "0.5.4")

addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.0.0")

addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "1.0")

addSbtPlugin("com.typesafe.sbt" % "sbt-git" % "0.8.5")

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

