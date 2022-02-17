import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object scalatestsuitelib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest"     %% "scalatest"               % ScalaTestV,
    "org.mockito"       %% "mockito-scala"           % MockitoV,
    "org.mockito"       %% "mockito-scala-scalatest" % MockitoV,
    "org.testcontainers" % "elasticsearch"           % TestContainersV,
    "org.testcontainers" % "testcontainers"          % TestContainersV,
    "org.testcontainers" % "postgresql"              % TestContainersV,
    jodaTime
  ) ++ database ++ vulnerabilityOverrides

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "scalatestsuite",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
