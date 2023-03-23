import Dependencies.versions._
import sbt.Keys._
import sbt._
import com.scalatsi.plugin.ScalaTsiPlugin

object searchlib extends Module {
  override val moduleName: String = "search"
  lazy val dependencies: Seq[ModuleID] = Seq(
    scalaUri,
    "org.json4s" %% "json4s-native" % Json4SV,
    "org.json4s" %% "json4s-ext"    % Json4SV
  ) ++ elastic4s
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
