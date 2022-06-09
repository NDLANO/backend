import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object scalatralib extends Module {
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.json4s"   %% "json4s-native"     % Json4SV,
      "org.json4s"   %% "json4s-ext"        % Json4SV,
      "javax.servlet" % "javax.servlet-api" % JavaxServletV
    ) ++ scalatra
  )
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "scalatra",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
