logLevel:= Level.Warn

addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "2.5.0")

addSbtPlugin("com.eed3si9n" % "sbt-assembly" % "0.11.2")

addSbtPlugin("de.johoop" % "jacoco4sbt" % "2.1.6")

addSbtPlugin("org.brianmckenna" % "sbt-wartremover" % "0.13")

addSbtPlugin("com.mojolly.scalate" % "xsbt-scalate-generator" % "0.5.0")

addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.0")

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.1.3"

