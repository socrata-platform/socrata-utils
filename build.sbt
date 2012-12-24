import SocrataSbtKeys._
import SocrataUtil._

seq(socrataSettings(): _*)

name := "socrata-utils"

scalaVersion := "2.10.0"

crossScalaVersions := Seq("2.8.1", "2.9.2", "2.10.0")

libraryDependencies <++= (slf4jVersion) { slf4jVersion =>
  Seq(
    "joda-time" % "joda-time" % "1.6",
    "com.yammer.metrics" % "metrics-core" % "2.0.3",
    "com.rojoma" %% "simple-arm" % "1.1.10",
    "org.slf4j" % "slf4j-simple" % slf4jVersion % "test"
  )
}

libraryDependencies <+= (scalaVersion) {
  case "2.8.1" => "org.scalacheck" % "scalacheck_2.8.1" % "1.8" % "test"
  case _ => "org.scalacheck" %% "scalacheck" % "1.10.0" % "test"
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
      case Is28() => printer.println("    Predef.error(message)")
      case Is29() => printer.println("    sys.error(message)")
      case Is210() => printer.println("    sys.error(message)")
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
