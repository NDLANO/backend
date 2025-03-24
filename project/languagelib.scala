import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object languagelib extends Module {
  override val moduleName: String      = "language"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    tapirHttp4sCirce
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
