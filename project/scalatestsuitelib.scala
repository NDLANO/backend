import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._

object scalatestsuitelib extends Module {
  lazy val dependencies: Seq[ModuleID] = Seq(
    "org.scalatest" %% "scalatest" % ScalaTestV,
    "org.mockito" %% "mockito-scala" % MockitoV,
    "org.mockito" %% "mockito-scala-scalatest" % MockitoV,
    "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
    "org.postgresql" % "postgresql" % PostgresV,
    "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
    "org.postgresql" % "postgresql" % PostgresV,
    "org.testcontainers" % "elasticsearch" % TestContainersV,
    "org.testcontainers" % "testcontainers" % TestContainersV,
    "org.testcontainers" % "postgresql" % TestContainersV,
    "joda-time" % "joda-time" % "2.10"
  ) ++ vulnerabilityOverrides

  private val scala213 = ScalaV
  private val scala212 = "2.12.10"
  private val supportedScalaVersions = List(scala213, scala212)

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "scalatestsuite",
    libraryDependencies ++= dependencies,
    crossScalaVersions := supportedScalaVersions
  ) ++
    commonSettings ++
    fmtSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
