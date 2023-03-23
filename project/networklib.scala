import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import Dependencies.versions._
import Dependencies._

object networklib extends Module {
  override val moduleName: String = "network"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      sttp,
      "org.json4s"           %% "json4s-jackson"          % Json4SV,
      "org.json4s"           %% "json4s-native"           % Json4SV,
      "org.scalatest"        %% "scalatest"               % ScalaTestV % "test",
      "org.mockito"          %% "mockito-scala"           % MockitoV   % "test",
      "org.mockito"          %% "mockito-scala-scalatest" % MockitoV   % "test",
      "javax.servlet"         % "javax.servlet-api"       % "4.0.1"    % "provided;test",
      "com.github.jwt-scala" %% "jwt-json4s-native"       % "9.0.2",
      "redis.clients"         % "jedis"                   % "4.2.3"
    ) ++ vulnerabilityOverrides ++ scalatra
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
