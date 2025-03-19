import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object databaselib extends Module {
  override val moduleName: String      = "database"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      jsoup,
      scalaUri
    ),
    database
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
