import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object integrationtests extends Module {
  override val MainClass: Option[String] = None

  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestV % "test",
    "org.mockito" %% "mockito-scala" % MockitoV % "test",
    "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "integration",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
