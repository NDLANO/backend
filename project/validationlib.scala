import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object validationlib extends Module {
  override val moduleName: String      = "validation"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestV % "test",
    jsoup,
    scalaUri
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
