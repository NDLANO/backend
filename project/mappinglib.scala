import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import Dependencies.versions._
import Dependencies._

object mappinglib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq("org.scalatest" %% "scalatest" % ScalaTestV % "test")

  private val scala213 = ScalaV
  private val scala212 = "2.12.10"
  private val supportedScalaVersions = List(scala213, scala212)

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "mapping",
    libraryDependencies ++= dependencies,
    crossScalaVersions := supportedScalaVersions
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
