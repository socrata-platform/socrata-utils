import com.typesafe.tools.mima.plugin.MimaKeys.previousArtifact

com.socrata.cloudbeessbt.SocrataCloudbeesSbt.socrataSettings()

name := "socrata-utils"

previousArtifact <<= scalaBinaryVersion { sv => None }

scalaVersion := "2.11.7"

crossScalaVersions := Seq("2.10.4", scalaVersion.value)

libraryDependencies ++= Seq(
  "com.rojoma"        %% "simple-arm"    % "[1.1.10,2.0.0)",
  "com.rojoma"        %% "simple-arm-v2" % "[2.1.0,3.0.0)",
  "com.yammer.metrics" % "metrics-core"  % "2.0.3",
  "joda-time"          % "joda-time"     % "2.6",
  "org.joda"           % "joda-convert"  % "1.2",
  "org.slf4j"          % "slf4j-simple"  % "1.7.5" % "test"
)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.12.4" % "test",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
