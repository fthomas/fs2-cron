lazy val root = (project in file(".")).settings(
  inThisBuild(
    List(
      organization := "com.example",
      version := "0.1.0-SNAPSHOT"
    )),
  name := "fs2-cron",
  libraryDependencies ++= Seq(
    Dependencies.cron4s,
    Dependencies.fs2Core,
    Dependencies.scalaTest % Test
  )
)

/// commands

def addCommandsAlias(name: String, cmds: Seq[String]) =
  addCommandAlias(name, cmds.mkString(";", ";", ""))

addCommandsAlias(
  "validate",
  Seq(
    "clean",
    "scalafmtCheck",
    "scalafmtSbtCheck",
    "test:scalafmtCheck",
    "coverage",
    "test",
    "coverageReport",
    "doc",
    "package",
    "packageSrc"
  )
)
