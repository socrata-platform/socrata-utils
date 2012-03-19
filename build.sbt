import com.socrata.sbtcommon.SbtCommon._

seq(socrataSettings(): _*)

name := "socrata-utils"

version := "1.0.0"

scalaVersion := "2.9.1-1"

crossScalaVersions := Seq("2.8.1", "2.9.1-1")

libraryDependencies ++= Seq(
  "joda-time" % "joda-time" % "1.6",
  "com.yammer.metrics" % "metrics-core" % "2.0.3",
  "org.slf4j" % "slf4j-simple" % slf4jVersion % "test"
)

libraryDependencies <+= (scalaVersion) {
  case "2.8.1" => "org.scala-tools.testing" % "scalacheck_2.8.1" % "1.8" % "test"
  case "2.9.1-1" => "org.scala-tools.testing" % "scalacheck_2.9.1" % "1.9" % "test"
}
