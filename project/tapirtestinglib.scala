import Dependencies.versions.*
import sbt.*
import sbt.Keys.*

object tapirtestinglib extends Module {
  override val moduleName: String      = "tapirtesting"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest"               %% "scalatest"      % ScalaTestV,
    "org.testcontainers"           % "elasticsearch"  % TestContainersV,
    "org.testcontainers"           % "testcontainers" % TestContainersV,
    "org.testcontainers"           % "postgresql"     % TestContainersV,
    "com.softwaremill.sttp.tapir" %% "tapir-testing"  % TapirV
  ) ++ database ++ vulnerabilityOverrides ++ tapirHttp4sCirce

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings
}
