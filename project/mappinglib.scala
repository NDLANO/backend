import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object mappinglib extends Module {
  override val moduleName: String      = "mapping"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = Seq("org.scalatest" %% "scalatest" % ScalaTestV % "test")

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
