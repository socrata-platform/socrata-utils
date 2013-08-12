import eu.diversit.sbt.plugin.WebDavPlugin._

seq(WebDav.scopedSettings : _*)

credentials ++= List(new File("/private/socrata-oss/maven-credentials")).flatMap { f =>
  if(f.exists) Some(Credentials(f)) else None
}

publishTo <<= version { v =>
  val cloudbees = "https://repository-socrata-oss.forge.cloudbees.com/"
  if(v.endsWith("SNAPSHOT")) {
    Some("snapshots" at cloudbees + "snapshot")
  } else {
    Some("releases" at cloudbees + "release")
  }
}
