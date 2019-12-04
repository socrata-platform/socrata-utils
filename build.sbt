name := "socrata-utils"
organization := "com.socrata"

mimaPreviousArtifacts := Set()

scalaVersion := "2.12.8"

crossScalaVersions := Seq("2.10.4", "2.11.7", scalaVersion.value)

libraryDependencies ++= Seq(
  "com.rojoma"        %% "simple-arm-v2" % "[2.1.0,3.0.0)",
  "com.yammer.metrics" % "metrics-core"  % "2.0.3",
  "joda-time"          % "joda-time"     % "2.6",
  "org.joda"           % "joda-convert"  % "1.2",
  "org.slf4j"          % "slf4j-simple"  % "1.7.5" % "test"
)

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.14.0" % "test",
  "org.scalatest" %% "scalatest" % "3.0.8" % "test"
)

scalacOptions ++= Seq("-deprecation", "-feature")
