import sbt._

object LatestVersion extends AutoPlugin {
  object autoImport {
    lazy val latestVersion: SettingKey[String] =
      settingKey[String]("latest released version")
  }
}
