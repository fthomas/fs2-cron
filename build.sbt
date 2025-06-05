import sbtcrossproject.{CrossProject, CrossType, Platform}

/// variables

val groupId = "eu.timepit"
val projectName = "fs2-cron"
val rootPkg = s"$groupId.${projectName.replace("-", "")}"

val Scala_2_12 = "2.12.20"
val Scala_2_13 = "2.13.16"
val Scala_3 = "3.3.5"

val moduleCrossPlatformMatrix: Map[String, List[Platform]] = Map(
  "calev" -> List(JVMPlatform, JSPlatform),
  "core" -> List(JVMPlatform, JSPlatform),
  "cron4s" -> List(JVMPlatform),
  "cron-utils" -> List(JVMPlatform)
)

/// global settings

ThisBuild / organization := groupId
ThisBuild / tlBaseVersion := "0.10"
ThisBuild / startYear := Some(2018)
ThisBuild / licenses := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("fthomas", "Frank S. Thomas")
)
ThisBuild / scalaVersion := Scala_2_13
ThisBuild / crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3)
ThisBuild / tlCiReleaseBranches := Seq("master")
ThisBuild / githubWorkflowBuild ++= Seq(
  WorkflowStep.Sbt(
    commands = List("readme/mdoc"),
    name = Some("Check README"),
    cond = Some(s"matrix.scala != '3' && matrix.project == 'rootJVM'")
  ),
  WorkflowStep.Sbt(
    commands = List("coverage", "test", "coverageReport"),
    name = Some("Generate coverage report"),
    cond = Some(s"matrix.scala != '3'")
  ),
  WorkflowStep.Use(
    ref = UseRef.Public("codecov", "codecov-action", "v4"),
    name = Some("Codecov"),
    env = Map("CODECOV_TOKEN" -> "${{ secrets.CODECOV_TOKEN }}")
  )
)
ThisBuild / mergifyPrRules := {
  val authorCondition = MergifyCondition.Or(
    List(
      MergifyCondition.Custom("author=scala-steward"),
      MergifyCondition.Custom("author=scala-steward[bot]")
    )
  )
  Seq(
    MergifyPrRule(
      "label scala-steward's PRs",
      List(authorCondition),
      List(MergifyAction.Label(List("dependency-update")))
    ),
    MergifyPrRule(
      "merge scala-steward's PRs",
      List(authorCondition) ++ mergifySuccessConditions.value,
      List(MergifyAction.Merge(Some("merge")))
    )
  )
}

/// projects

lazy val root = tlCrossRootProject
  .aggregate(calev, core, cron4s, cronUtils, readme)

lazy val core = myCrossProject("core")
  .settings(
    libraryDependencies ++= Seq(
      "co.fs2" %%% "fs2-core" % "3.12.0",
      "org.typelevel" %%% "munit-cats-effect" % "2.1.0" % Test
    )
  )
  .jsSettings(
    tlVersionIntroduced := Map(
      "2.12" -> "0.8.3",
      "2.13" -> "0.8.3",
      "3" -> "0.8.3"
    )
  )

lazy val calev = myCrossProject("calev")
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    libraryDependencies += "com.github.eikek" %%% "calev-core" % "0.7.4",
    initialCommands += s"""
      import $rootPkg.calev._
      import com.github.eikek.calev._
    """
  )
  .jsSettings(
    libraryDependencies += "io.github.cquiroz" %%% "scala-java-time-tzdb" % "2.6.0" % Test,
    tlVersionIntroduced := Map(
      "2.12" -> "0.8.3",
      "2.13" -> "0.8.3",
      "3" -> "0.8.3"
    )
  )

lazy val cron4s = myCrossProject("cron4s")
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    libraryDependencies += "com.github.alonsodomin.cron4s" %% "cron4s-core" % "0.8.2",
    libraryDependencies ++= {
      if (scalaVersion.value.startsWith("2.12."))
        List(scalaOrganization.value % "scala-reflect" % scalaVersion.value)
      else
        List()
    },
    initialCommands += s"""
      import $rootPkg.cron4s._
      import _root_.cron4s.Cron
      import _root_.cron4s.expr.CronExpr
    """
  )

lazy val cronUtils = myCrossProject("cron-utils")
  .dependsOn(core % "compile->compile;test->test")
  .settings(
    libraryDependencies += "com.cronutils" % "cron-utils" % "9.2.1",
    initialCommands += s"""
      import $rootPkg.cronutils._
      import com.cronutils.model.CronType
      import com.cronutils.model.definition.CronDefinitionBuilder
      import com.cronutils.model.time.ExecutionTime
      import com.cronutils.parser.CronParser
    """,
    tlVersionIntroduced := Map(
      "2.12" -> "0.8.2",
      "2.13" -> "0.8.2",
      "3" -> "0.8.2"
    )
  )

lazy val readme = project
  .in(file("modules/readme"))
  .enablePlugins(MdocPlugin, NoPublishPlugin)
  .dependsOn(calev.jvm, cron4s.jvm)
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
  metadataSettings,
  Test / parallelExecution := false
)

lazy val compileSettings = Def.settings(
  scalaVersion := Scala_2_13,
  crossScalaVersions := List(Scala_2_12, Scala_2_13, Scala_3),
  initialCommands := s"""
    import $rootPkg._
    import cats.effect.IO
    import cats.effect.unsafe.implicits.global
    import fs2.Stream
  """
)

lazy val metadataSettings = Def.settings(
  headerLicense := Some(HeaderLicense.ALv2("2018-2023", s"$projectName contributors"))
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
