import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType

/// variables

val groupId = "eu.timepit"
val projectName = "fs2-cron"
val gitHubOwner = "fthomas"

val moduleCrossPlatformMatrix = Map(
  "core" -> List(JVMPlatform)
)

/// projects

lazy val root = project
  .in(file("."))
  .aggregate(coreJVM)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    inThisBuild(
      List(
        organization := "com.example",
        version := "0.1.0-SNAPSHOT"
      )),
    name := "fs2-cron",
    libraryDependencies ++= Seq(
      Dependencies.cron4s,
      Dependencies.fs2Core
    )
  )

lazy val core = myCrossProject("core")
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.cron4s,
      Dependencies.fs2Core
    )
  )

lazy val coreJVM = core.jvm

/// settings

def myCrossProject(name: String): CrossProject =
  CrossProject(name, file(name))(moduleCrossPlatformMatrix(name): _*)
    .crossType(CrossType.Pure)
    .withoutSuffixFor(JVMPlatform)
    .in(file(s"modules/$name"))
    .settings(moduleName := s"$projectName-$name")
    .settings(commonSettings)

lazy val commonSettings = Def.settings(
  compileSettings,
  metadataSettings,
  scaladocSettings
)

lazy val compileSettings = Def.settings()

lazy val metadataSettings = Def.settings()

lazy val noPublishSettings = Def.settings(
  skip in publish := true
)

lazy val scaladocSettings = Def.settings()

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
