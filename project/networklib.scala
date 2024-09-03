import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.*

object networklib extends Module {
  override val moduleName: String      = "network"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      sttp,
      "org.scalatest"        %% "scalatest" % ScalaTestV % "test",
      "redis.clients"         % "jedis"     % "5.1.5",
      "com.github.jwt-scala" %% "jwt-circe" % "10.0.1"
    ),
    mockito,
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
