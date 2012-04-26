import SocrataSbtKeys._

seq(socrataSettings(): _*)

name := "socrata-utils"

scalaVersion := "2.9.2"

crossScalaVersions := Seq("2.8.1", "2.9.2")

libraryDependencies <++= (slf4jVersion) { slf4jVersion =>
  Seq(
    "joda-time" % "joda-time" % "1.6",
    "com.yammer.metrics" % "metrics-core" % "2.0.3",
    "com.rojoma" %% "simple-arm" % "1.1.10",
    "org.slf4j" % "slf4j-simple" % slf4jVersion % "test"
  )
}

libraryDependencies <+= (scalaVersion) {
  case "2.8.1" => "org.scala-tools.testing" % "scalacheck_2.8.1" % "1.8" % "test"
  case "2.9.2" => "org.scala-tools.testing" % "scalacheck_2.9.1" % "1.9" % "test"
}

testOptions in Test += Tests.Setup { loader =>
  loader.loadClass("org.slf4j.LoggerFactory").getMethod("getILoggerFactory").invoke(null)
}

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
      case "2.8.1" => printer.println("    Predef.error(message)")
      case "2.9.2" => printer.println("    sys.error(message)")
    }
    printer.println("  }")
    printer.println("}")
  }
  Seq(targetFile)
}
