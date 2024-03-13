import Dependencies.versions._
import sbt.Keys._
import sbt._

object integrationtests extends Module {
  override val moduleName: String      = "integration-tests"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
