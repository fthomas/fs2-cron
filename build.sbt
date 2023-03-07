import sbtcrossproject.{CrossProject, CrossType, Platform}
import org.typelevel.sbt.gha.JavaSpec.Distribution.Temurin

/// variables

val groupId = "eu.timepit"
val projectName = "fs2-cron"
val rootPkg = s"$groupId.${projectName.replace("-", "")}"
val gitHubOwner = "fthomas"

val Scala_2_12 = "2.12.17"
val Scala_2_13 = "2.13.10"
val Scala_3 = "3.2.2"

val moduleCrossPlatformMatrix: Map[String, List[Platform]] = Map(
  "core" -> List(JVMPlatform),
  "cron4s" -> List(JVMPlatform),
  "calev" -> List(JVMPlatform)
)

/// global settings

ThisBuild / organization := groupId
ThisBuild / tlBaseVersion := "0.8"
ThisBuild / startYear := Some(2018)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("fthomas", "Frank S. Thomas")
)
ThisBuild / tlSkipIrrelevantScalas := true
ThisBuild / scalaVersion := Scala_2_13
ThisBuild / crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3)
ThisBuild / tlCiReleaseBranches := Seq("master")
ThisBuild / githubWorkflowBuild ++= Seq(
  WorkflowStep.Sbt(
    commands = List("readme/mdoc"),
    name = Some("Check README"),
    cond = Some(s"matrix.scala != '$Scala_3'")
  ),
  WorkflowStep.Sbt(
    commands = List("coverage", "test", "coverageReport"),
    name = Some("Generate coverage report"),
    cond = Some(s"matrix.scala != '$Scala_3'")
  ),
  WorkflowStep.Use(
    ref = UseRef.Public("codecov", "codecov-action", "v3"),
    name = Some("Codecov")
  )
)
ThisBuild / mergifyPrRules := {
  val authorCondition = MergifyCondition.Custom("author=scala-steward")
  Seq(
    MergifyPrRule(
      "label scala-steward's PRs",
      List(authorCondition),
      List(MergifyAction.Label(List("dependency-update")))
    ),
    MergifyPrRule(
      "merge scala-steward's PRs",
      List(authorCondition) ++ mergifySuccessConditions.value,
      List(MergifyAction.Merge(Some("squash")))
    )
  )
}

/// projects

lazy val root = tlCrossRootProject
  .aggregate(calev, core, cron4s, readme)

lazy val core = myCrossProject("core")
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.fs2Core
    )
  )

lazy val coreJVM = core.jvm

lazy val cron4s = myCrossProject("cron4s")
  .dependsOn(core)
  .settings(
    crossScalaVersions := List(Scala_2_12, Scala_2_13),
    libraryDependencies ++= Seq(
      Dependencies.cron4s,
      Dependencies.fs2Core,
      Dependencies.scalaTest % Test
    ),
    initialCommands := s"""
      import $rootPkg._
      import cats.effect.unsafe.implicits.global
      import cats.effect.IO
      import _root_.cron4s.Cron
      import fs2.Stream
      import scala.concurrent.ExecutionContext
    """
  )

lazy val cron4sJVM = cron4s.jvm

lazy val calev = myCrossProject("calev")
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      Dependencies.calevCore,
      Dependencies.scalaTest % Test
    ),
    initialCommands := s"""
      import $rootPkg._
      import $rootPkg.calev._
      import cats.effect.IO
      import cats.effect.unsafe.implicits.global
      import com.github.eikek.calev._
      import fs2.Stream
      import scala.concurrent.ExecutionContext
    """
  )

lazy val calevJVM = calev.jvm

lazy val readme = project
  .in(file("modules/readme"))
  .enablePlugins(MdocPlugin, NoPublishPlugin)
  .dependsOn(calevJVM, cron4sJVM)
  .settings(commonSettings)
  .settings(
    crossScalaVersions := List(Scala_2_12, Scala_2_13),
    scalacOptions -= "-Xfatal-warnings",
    mdocIn := baseDirectory.value / "README.md",
    mdocOut := (LocalRootProject / baseDirectory).value / "README.md",
    mdocVariables := Map(
      "VERSION" -> tlLatestVersion.value.get
    )
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
  metadataSettings
)

lazy val compileSettings = Def.settings(
  scalaVersion := Scala_2_13,
  crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3)
)

lazy val metadataSettings = Def.settings(
  headerLicense := Some(HeaderLicense.ALv2("2018-2021", s"$projectName contributors"))
)

/// commands

def addCommandsAlias(name: String, cmds: Seq[String]) =
  addCommandAlias(name, cmds.mkString(";", ";", ""))

addCommandsAlias(
  "fmt",
  Seq(
    "headerCreateAll",
    "scalafmtAll",
    "scalafmtSbt"
  )
)
