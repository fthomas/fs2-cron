import com.github.sbt.git.SbtGit.GitKeys
import sbtcrossproject.{CrossProject, CrossType, Platform}
import org.typelevel.sbt.gha.JavaSpec.Distribution.Temurin

/// variables

val groupId = "eu.timepit"
val projectName = "fs2-cron"
val rootPkg = s"$groupId.${projectName.replace("-", "")}"
val gitHubOwner = "fthomas"

val Scala_2_12 = "2.12.17"
val Scala_2_13 = "2.13.10"
val Scala_3 = "3.2.1"

val moduleCrossPlatformMatrix: Map[String, List[Platform]] = Map(
  "core" -> List(JVMPlatform),
  "cron4s" -> List(JVMPlatform),
  "calev" -> List(JVMPlatform)
)

/// sbt-github-actions configuration
ThisBuild / crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3)
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
ThisBuild / githubWorkflowPublishTargetBranches := Seq(
  RefPredicate.Equals(Ref.Branch("master")),
  RefPredicate.StartsWith(Ref.Tag("v"))
)
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Run(
    List("sbt ci-release"),
    name = Some("Publish JARs"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec(Temurin, "8"))
ThisBuild / githubWorkflowBuild :=
  Seq(
    WorkflowStep.Sbt(List("validate"), name = Some("Build project")),
    WorkflowStep.Use(UseRef.Public("codecov", "codecov-action", "v1"), name = Some("Codecov"))
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

/// global settings

ThisBuild / versionScheme := Some("early-semver")

/// projects

lazy val root = project
  .in(file("."))
  .aggregate(coreJVM)
  .aggregate(cron4sJVM)
  .aggregate(calevJVM)
  .aggregate(readme)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := Nil
  )

lazy val core = myCrossProject("core")
  .settings(
    crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3),
    libraryDependencies ++= Seq(
      Dependencies.fs2Core
    )
  )

lazy val coreJVM = core.jvm

lazy val cron4s = myCrossProject("cron4s")
  .dependsOn(core)
  .settings(
    crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3),
    libraryDependencies ++= Seq(
      Dependencies.cron4s
        .cross(CrossVersion.for3Use2_13)
        .excludeAll(ExclusionRule("org.typelevel")),
      Dependencies.fs2Core,
      Dependencies.scalaTest % Test
    ),
    publish / skip := scalaBinaryVersion.value == "3",
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
    crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3),
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
val runMdoc2 = taskKey[Unit]("Run mdoc only for scala 2.x")
lazy val readme = project
  .in(file("modules/readme"))
  .enablePlugins(MdocPlugin)
  .dependsOn(cron4sJVM, calevJVM)
  .settings(commonSettings)
  .settings(noPublishSettings)
  .settings(
    crossScalaVersions := List(Scala_2_12, Scala_2_13),
    runMdoc2 := Def.taskDyn {
      val t = mdoc.inputTaskValue
      if ((coreJVM / scalaBinaryVersion).value == "3")
        Def.task(streams.value.log("readme").info("Skip readme generation"))
      else Def.inputTask(t.evaluated).toTask("")
    }.value,
    scalacOptions -= "-Xfatal-warnings",
    mdocIn := baseDirectory.value / "README.md",
    mdocOut := (LocalRootProject / baseDirectory).value / "README.md",
    mdocVariables := Map(
      "VERSION" -> latestVersion.value
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
  metadataSettings,
  scaladocSettings
)

lazy val compileSettings = Def.settings(
  scalaVersion := Scala_2_13,
  coverageEnabled := false
)

lazy val metadataSettings = Def.settings(
  name := projectName,
  organization := groupId,
  homepage := Some(url(s"https://github.com/$gitHubOwner/$projectName")),
  startYear := Some(2018),
  licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  headerLicense := Some(HeaderLicense.ALv2("2018-2021", s"$projectName contributors")),
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
  publish / skip := true
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
    "scalafmtCheckAll",
    "scalafmtSbtCheck",
    "coverage",
    "test",
    "coverageReport",
    "readme/runMdoc2",
    "doc",
    "package",
    "packageSrc"
  )
)

addCommandsAlias(
  "fmt",
  Seq(
    "headerCreate",
    "scalafmtAll",
    "scalafmtSbt"
  )
)
