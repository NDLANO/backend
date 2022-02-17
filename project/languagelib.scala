import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object languagelib extends Module {
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.json4s"    %% "json4s-native" % Json4SV,
      "org.scalatest" %% "scalatest"     % ScalaTestV % "test",
      "org.mockito"    % "mockito-all"   % "1.10.19"  % "test"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "language",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)

}
