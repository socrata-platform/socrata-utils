import com.typesafe.tools.mima.plugin.MimaPlugin.mimaDefaultSettings
import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

mimaDefaultSettings

name := "socrata-utils"

organization := "com.socrata"

version := "0.7.0"

previousArtifact <<= scalaBinaryVersion { sv => Some("com.socrata" % ("socrata-utils_" + sv) % "0.6.1") }

scalaVersion := "2.10.2"

crossScalaVersions := Seq("2.8.1", "2.9.2", "2.10.2")

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "1.6",
  "com.yammer.metrics" % "metrics-core" % "2.0.3",
  "com.rojoma" %% "simple-arm" % "1.1.10",
  "org.slf4j" % "slf4j-simple" % "1.7.5" % "test"
)

libraryDependencies <++= (scalaVersion) {
  case "2.8.1" =>
    Seq("org.scalacheck" % "scalacheck_2.8.1" % "1.8" % "test",
        "org.scalatest" % "scalatest_2.8.1" % "1.8" % "test")
  case _ =>
    Seq("org.scalacheck" %% "scalacheck" % "1.10.0" % "test",
        "org.scalatest" %% "scalatest" % "1.9.1" % "test")
}

testOptions in Test += Tests.Setup { loader =>
  loader.loadClass("org.slf4j.LoggerFactory").getMethod("getILoggerFactory").invoke(null)
}

scalacOptions <++= (scalaVersion) map {
  case s if s.startsWith("2.8.") => Seq("-deprecation")
  case s if s.startsWith("2.9.") => Seq("-deprecation")
  case s if s.startsWith("2.10.") =>
    Seq("-deprecation", "-feature",
      // I would prefer to turn these on per-scope as appropriate
      // but can't do it while keeping 2.[8,9] compatibility.
      "-language:higherKinds", "-language:implicitConversions")
}

javacOptions ++= Seq("-Xlint:unchecked")

sourceGenerators in Compile <+= (sourceManaged in Compile, scalaVersion in Compile) map { (baseDir, scalaVersion) =>
  import java.io._
  import com.rojoma.simplearm.util._
  val targetDir = baseDir / "com" / "socrata" / "util-impl"
  val targetFile = targetDir / "ErrorImpl.scala"
  targetDir.mkdirs()
  for {
    stream <- managed(new FileOutputStream(targetFile))
    writer <- managed(new OutputStreamWriter(stream, "UTF-8"))
    printer <- managed(new PrintWriter(writer))
  } {
    printer.println("package com.socrata.`util-impl`")
    printer.println()
    printer.println("object ErrorImpl {")
    printer.println("  @inline def error(message: String): Nothing = {")
    scalaVersion match {
      case s if s.startsWith("2.8.") => printer.println("    Predef.error(message)")
      case s if s.startsWith("2.9.") => printer.println("    sys.error(message)")
      case s if s.startsWith("2.10.") => printer.println("    sys.error(message)")
    }
    printer.println("  }")
    printer.println("}")
  }
  Seq(targetFile)
}

// bllllleargh -- 2.8's doc process blows up thanks to SI-4284
publishArtifact in (Compile, packageDoc) <<= scalaVersion { sv =>
  !sv.startsWith("2.8.")
}
