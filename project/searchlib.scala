import Dependencies.versions._
import sbt.Keys._
import sbt._
import com.scalatsi.plugin.ScalaTsiPlugin

object searchlib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
    elastic4sCore,
    elastic4sHttp,
    elastic4sAWS,
    elastic4sEmbedded,
    scalaUri
  )
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "search",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
