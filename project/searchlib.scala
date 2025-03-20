import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object searchlib extends Module {
  override val moduleName: String      = "search"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = Seq(
    scalaUri,
    catsEffect,
    jsoup
  ) ++ elastic4s
  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
