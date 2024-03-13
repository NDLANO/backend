import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object myndlalib extends Module {
  override val moduleName: String      = "myndla"
  override val enableReleases: Boolean = false

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      sttp,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    tapirHttp4sCirce,
    database,
    vulnerabilityOverrides
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
