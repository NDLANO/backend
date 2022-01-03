import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object validationlib {
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

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "validation",
    libraryDependencies ++= dependencies,
    crossScalaVersions := supportedScalaVersions
  ) ++
    commonSettings ++
    fmtSettings

  lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
