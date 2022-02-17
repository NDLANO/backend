import Dependencies.versions._
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._
import sbtdocker.DockerPlugin

object oembedproxy extends Module {
  override val MainClass: Option[String] = Some("no.ndla.oembedproxy.JettyLauncher")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaUri,
      "org.eclipse.jetty" % "jetty-webapp"            % JettyV     % "container;compile",
      "org.eclipse.jetty" % "jetty-plus"              % JettyV     % "container",
      "javax.servlet"     % "javax.servlet-api"       % "3.1.0"    % "container;provided;test",
      "org.json4s"       %% "json4s-native"           % Json4SV,
      "org.scalaj"       %% "scalaj-http"             % "2.4.2",
      "org.jsoup"         % "jsoup"                   % "1.11.3",
      "org.scalatest"    %% "scalatest"               % ScalaTestV % "test",
      "org.mockito"      %% "mockito-scala"           % MockitoV   % "test",
      "org.mockito"      %% "mockito-scala-scalatest" % MockitoV   % "test"
    ) ++ scalatra ++ vulnerabilityOverrides
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "oembed-proxy",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings()

  override lazy val plugins = Seq(
    JettyPlugin,
    DockerPlugin
  )

  override lazy val disablePlugins = Seq(
    ScalaTsiPlugin
  )
}
