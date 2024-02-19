import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object searchapi extends Module {
  override val moduleName: String        = "search-api"
  override val MainClass: Option[String] = Some("no.ndla.searchapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      jsoup,
      "vc.inreach.aws"    % "aws-signing-request-interceptor" % "0.0.22",
      "org.eclipse.jetty" % "jetty-webapp"                    % JettyV     % "container;compile",
      "org.eclipse.jetty" % "jetty-plus"                      % JettyV     % "container",
      "org.json4s"       %% "json4s-native"                   % Json4SV,
      "org.json4s"       %% "json4s-ext"                      % Json4SV,
      "com.amazonaws"     % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "org.scalatest"    %% "scalatest"                       % ScalaTestV % "test",
      "org.mockito"      %% "mockito-scala"                   % MockitoV   % "test",
      "org.mockito"      %% "mockito-scala-scalatest"         % MockitoV   % "test"
    ),
    melody,
    elastic4s,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.searchapi.model.api._"),
    exports = Seq(
      "ApiTaxonomyContext",
      "ArticleResult",
      "AudioResult",
      "GroupSearchResult",
      "ImageResult",
      "LearningpathResult",
      "MultiSearchResult",
      "ArticleResults",
      "AudioResults",
      "ImageResults",
      "LearningpathResults",
      "SearchError",
      "ValidationError"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings() ++
    tsSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin
  )
}
