import Dependencies.versions._
import sbt.Keys._
import sbt._

object scalatestsuitelib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.testcontainers" % "elasticsearch"  % TestContainersV,
    "org.testcontainers" % "testcontainers" % TestContainersV,
    "org.testcontainers" % "postgresql"     % TestContainersV
  ) ++ database ++ vulnerabilityOverrides ++ scalaTestAndMockitoInMain

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "scalatestsuite",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disableTSI: Boolean = true
}
