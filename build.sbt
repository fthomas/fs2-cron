import com.typesafe.sbt.SbtGit.GitKeys
import sbtcrossproject.CrossProject
import sbtcrossproject.CrossType
import sbtcrossproject.Platform
import scala.sys.process._

/// variables

val groupId = "eu.timepit"
val projectName = "fs2-cron"
val rootPkg = s"$groupId.${projectName.replace("-", "")}"
val gitHubOwner = "fthomas"

val moduleCrossPlatformMatrix: Map[String, List[Platform]] = Map(
  "core" -> List(JVMPlatform)
)
/// projects

lazy val root = project
  .in(file("."))
  .aggregate(coreJVM)
  .aggregate(readme)
  .settings(commonSettings)
  .settings(noPublishSettings)

lazy val core = myCrossProject("core")
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.cron4s,
      Dependencies.fs2Core,
      Dependencies.scalaTest % Test
    )
  )

lazy val coreJVM = core.jvm

lazy val readme = project
  .in(file("modules/readme"))
  .enablePlugins(BuildInfoPlugin)
  .enablePlugins(TutPlugin)
  .dependsOn(coreJVM)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    buildInfoKeys := Seq[BuildInfoKey](latestVersion),
    fork in Tut := true,
    scalacOptions -= "-Ywarn-unused:imports",
    scalacOptions -= "-Ywarn-unused-import", // the same as above but for 2.11
    tutSourceDirectory := baseDirectory.value,
    tutTargetDirectory := (LocalRootProject / baseDirectory).value
  )

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
  scaladocSettings,
  initialCommands := s"""
    import $rootPkg._
    import cats.effect.{ContextShift, IO, Timer}
    import cron4s.Cron
    import fs2.Stream
    import scala.concurrent.ExecutionContext

    implicit val ioContextShift: ContextShift[IO] = IO.contextShift(ExecutionContext.global)
    implicit val ioTimer: Timer[IO] = IO.timer(ExecutionContext.global)
  """
)

lazy val compileSettings = Def.settings()

lazy val metadataSettings = Def.settings(
  name := projectName,
  organization := groupId,
  homepage := Some(url(s"https://github.com/$gitHubOwner/$projectName")),
  startYear := Some(2018),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  headerLicense := Some(HeaderLicense.ALv2("2018", s"$projectName contributors")),
  developers := List(
    Developer(
      id = "fthomas",
      name = "Frank S. Thomas",
      email = "",
      url("https://github.com/fthomas")
    )
  )
)

lazy val noPublishSettings = Def.settings(
  skip in publish := true
)

lazy val scaladocSettings = Def.settings(
  Compile / doc / scalacOptions ++= {
    val tree =
      if (isSnapshot.value) GitKeys.gitHeadCommit.value
      else GitKeys.gitDescribedVersion.value.map("v" + _)
    Seq(
      "-doc-source-url",
      s"${scmInfo.value.get.browseUrl}/blob/${tree.get}€{FILE_PATH}.scala",
      "-sourcepath",
      (LocalRootProject / baseDirectory).value.getAbsolutePath
    )
  }
)

/// commands

def addCommandsAlias(name: String, cmds: Seq[String]) =
  addCommandAlias(name, cmds.mkString(";", ";", ""))

addCommandsAlias(
  "validate",
  Seq(
    "clean",
    "headerCheck",
    "scalafmtCheck",
    "scalafmtSbtCheck",
    "test:scalafmtCheck",
    "coverage",
    "test",
    "coverageReport",
    "doc",
    "readme/tut",
    "package",
    "packageSrc"
  )
)
