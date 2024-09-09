import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object integrationtests extends Module {
  override val moduleName: String      = "integration-tests"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    )
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
