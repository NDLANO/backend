import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object validationlib extends Module {
  override val moduleName: String = "validation"
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest"     % ScalaTestV % "test",
    "org.jsoup"      % "jsoup"         % JsoupV,
    "org.json4s"    %% "json4s-native" % Json4SV,
    "org.json4s"    %% "json4s-ext"    % Json4SV,
    scalaUri
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
