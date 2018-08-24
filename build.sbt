import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "fs2-cron",
    libraryDependencies ++= Seq(
      "co.fs2" %% "fs2-core" % "0.10.5",
      "com.github.alonsodomin.cron4s" %% "cron4s-core" % "0.4.5",
      scalaTest % Test
    )
  )
