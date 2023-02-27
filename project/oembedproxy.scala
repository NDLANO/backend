import Dependencies.versions._
import com.earldouglas.xwp.JettyPlugin
import sbt.Keys._
import sbt._
import sbtdocker.DockerPlugin

object oembedproxy extends Module {
  override val MainClass: Option[String] = Some("no.ndla.oembedproxy.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaUri,
      sttp,
      "com.amazonaws"     % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.eclipse.jetty" % "jetty-webapp"            % JettyV  % "container;compile",
      "org.eclipse.jetty" % "jetty-plus"              % JettyV  % "container",
      "javax.servlet"     % "javax.servlet-api"       % "3.1.0" % "container;provided;test",
      "org.json4s"       %% "json4s-native"           % Json4SV,
      "org.jsoup"         % "jsoup"                   % JsoupV
    ) ++ scalatra ++ vulnerabilityOverrides ++ scalaTestAndMockito
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

  override lazy val disableTSI: Boolean = true
}
