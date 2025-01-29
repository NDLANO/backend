import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.*

object scalatestsuitelib extends Module {
  override val moduleName: String      = "scalatestsuite"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      "org.scalatest"     %% "scalatest"      % ScalaTestV,
      "org.testcontainers" % "elasticsearch"  % TestContainersV,
      "org.testcontainers" % "testcontainers" % TestContainersV,
      "org.testcontainers" % "postgresql"     % TestContainersV
    ),
    database,
    vulnerabilityOverrides,
    mockito
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
