import Dependencies.versions._
import sbt.Keys._
import sbt._

object validationlib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.jsoup"   % "jsoup"         % JsoupV,
    "org.json4s" %% "json4s-native" % Json4SV,
    "org.json4s" %% "json4s-ext"    % Json4SV,
    scalaUri
  ) ++ scalaTestAndMockito

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "validation",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disableTSI: Boolean = true
}
