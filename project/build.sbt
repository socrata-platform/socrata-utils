resolvers ++= Seq(
  "socrata releases" at "http://repo.socrata.com/artifactory/libs-release",
  "DiversIT repo" at "http://repository-diversit.forge.cloudbees.com/release"
)

addSbtPlugin("com.socrata" % "socrata-cloudbees-sbt" % "1.4.1")

libraryDependencies += "com.rojoma" %% "simple-arm" % "1.1.10"
