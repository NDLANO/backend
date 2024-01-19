import Dependencies.versions.*
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys.*
import sbt.*
import sbtdocker.DockerPlugin

object oembedproxy extends Module {
  override val moduleName: String        = "oembed-proxy"
  override val MainClass: Option[String] = Some("no.ndla.oembedproxy.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaUri,
      sttp,
      jsoup,
      "com.amazonaws"     % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.eclipse.jetty" % "jetty-webapp"            % JettyV     % "container;compile",
      "org.eclipse.jetty" % "jetty-plus"              % JettyV     % "container",
      "javax.servlet"     % "javax.servlet-api"       % "4.0.1"    % "container;provided;test",
      "org.json4s"       %% "json4s-native"           % Json4SV,
      "org.scalatest"    %% "scalatest"               % ScalaTestV % "test",
      "org.mockito"      %% "mockito-scala"           % MockitoV   % "test",
      "org.mockito"      %% "mockito-scala-scalatest" % MockitoV   % "test"
    ),
    scalatra,
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings()

  override lazy val plugins: Seq[Plugins] = Seq(
    JettyPlugin,
    DockerPlugin
  )

  override lazy val disablePlugins: Seq[AutoPlugin] = Seq(
    ScalaTsiPlugin
  )
}
