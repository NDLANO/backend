import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object scalatestsuitelib {
  lazy val dependencies: Seq[ModuleID] = Seq(
    ndlaNetwork,
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

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "scalatestsuite",
    libraryDependencies ++= dependencies,
    crossScalaVersions := supportedScalaVersions
  ) ++
    commonSettings ++
    fmtSettings

  lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
