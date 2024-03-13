import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import Dependencies.versions._
import Dependencies._

object networklib extends Module {
  override val moduleName: String      = "network"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      sttp,
      "org.scalatest"        %% "scalatest" % ScalaTestV % "test",
      "redis.clients"         % "jedis"     % "5.1.1",
      "com.github.jwt-scala" %% "jwt-circe" % "10.0.0"
    ),
    mockito,
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
