import Dependencies.common._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object languagelib {
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" % "mockito-all" % "1.10.19" % "test"
    ))

  private val scala213 = ScalaV
  private val scala212 = "2.12.10"
  private val supportedScalaVersions = List(scala213, scala212)

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "language",
    libraryDependencies ++= dependencies,
    crossScalaVersions := supportedScalaVersions
  ) ++
    commonSettings ++
    fmtSettings

  lazy val disablePlugins = Seq(ScalaTsiPlugin)

}
