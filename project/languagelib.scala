import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object languagelib extends Module {
  override val moduleName: String      = "language"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.scalatest" %% "scalatest"     % ScalaTestV % "test"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
