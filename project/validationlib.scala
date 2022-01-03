import Dependencies.common._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object validationlib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestV % "test",
    "org.jsoup" % "jsoup" % "1.11.3",
    "org.json4s" %% "json4s-native" % Json4SV,
    "org.json4s" %% "json4s-ext" % Json4SV,
    "io.lemonlabs" %% "scala-uri" % "1.5.1"
  )

  private val scala213 = ScalaV
  private val scala212 = "2.12.10"
  private val supportedScalaVersions = List(scala213, scala212)

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "validation",
    libraryDependencies ++= dependencies,
    crossScalaVersions := supportedScalaVersions
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
