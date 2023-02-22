import Dependencies.versions._
import sbt.Keys._
import sbt._

object integrationtests extends Module {
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq() ++ scalaTestAndMockito
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "integration-tests",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
