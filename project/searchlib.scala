import Dependencies.versions._
import sbt.Keys._
import sbt._
import com.scalatsi.plugin.ScalaTsiPlugin

object searchlib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    scalaUri
  ) ++ elastic4s
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "search",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
