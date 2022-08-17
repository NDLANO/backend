import sbt.Keys._
import sbt._
import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin

object commonlib extends Module {
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumJson4s,
      "org.json4s"   %% "json4s-native"     % Json4SV,
      "org.json4s"   %% "json4s-ext"        % Json4SV,
      "javax.servlet" % "javax.servlet-api" % JavaxServletV
    ) ++ scalatra
  )
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "common",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
