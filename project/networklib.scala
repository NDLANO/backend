import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object networklib extends Module {
  override val moduleName: String      = "network"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      sttp,
      "org.scalatest"        %% "scalatest" % ScalaTestV % "test",
      "redis.clients"         % "jedis"     % "5.2.0",
      "com.github.jwt-scala" %% "jwt-circe" % "10.0.4"
    ),
    mockito,
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
