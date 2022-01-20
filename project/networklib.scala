import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import Dependencies.versions._
import Dependencies._

object networklib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.json4s" %% "json4s-jackson" % Json4SV,
    "org.json4s" %% "json4s-native" % Json4SV,
    "org.scalaj" %% "scalaj-http" % "2.4.2",
    "org.scalatest" %% "scalatest" % ScalaTestV % "test",
    "org.mockito" %% "mockito-scala" % MockitoV % "test",
    "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
    "javax.servlet" % "javax.servlet-api" % "4.0.1" % "provided;test",
    "com.github.jwt-scala" %% "jwt-json4s-native" % "9.0.2"
  ) ++ vulnerabilityOverrides

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "network",
    libraryDependencies ++= dependencies,
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
