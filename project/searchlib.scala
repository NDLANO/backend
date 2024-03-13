import Dependencies.versions._
import sbt.Keys._
import sbt._
import com.scalatsi.plugin.ScalaTsiPlugin

object searchlib extends Module {
  override val moduleName: String      = "search"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = Seq(
    scalaUri,
    catsEffect,
    jsoup,
  ) ++ elastic4s
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
