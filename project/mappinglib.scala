import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import Dependencies.versions._
import Dependencies._

object mappinglib extends Module {
  override val moduleName: String      = "mapping"
  lazy val dependencies: Seq[ModuleID] = Seq("org.scalatest" %% "scalatest" % ScalaTestV % "test")

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
