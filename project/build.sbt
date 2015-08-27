resolvers ++= Seq(
  "socrata releases" at "http://repository-socrata-oss.forge.cloudbees.com/release",
  "DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release"
)

addSbtPlugin("com.socrata" % "socrata-cloudbees-sbt" % "1.4.0")

libraryDependencies += "com.rojoma" %% "simple-arm" % "1.1.10"
