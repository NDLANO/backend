import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object articleapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.articleapi.Main")
  override val moduleName: String        = "article-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      sttp,
      jsoup,
      "org.eclipse.jetty" % "jetty-webapp"                    % JettyV     % "container;compile",
      "org.eclipse.jetty" % "jetty-plus"                      % JettyV     % "container",
      "javax.servlet"     % "javax.servlet-api"               % "4.0.1"    % "container;provided;test",
      "org.json4s"       %% "json4s-native"                   % Json4SV,
      "com.amazonaws"     % "aws-java-sdk-s3"                 % AwsSdkV,
      "com.amazonaws"     % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "vc.inreach.aws"    % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalatest"    %% "scalatest"                       % ScalaTestV % "test",
      "org.mockito"      %% "mockito-scala"                   % MockitoV   % "test",
      "org.mockito"      %% "mockito-scala-scalatest"         % MockitoV   % "test",
      "org.flywaydb"      % "flyway-core"                     % FlywayV
    ),
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq(
      "no.ndla.articleapi.model.api._",
      "no.ndla.articleapi.model.api.TSTypes._",
      "no.ndla.common.model.domain.Availability"
    ),
    exports = Seq(
      "ArticleV2",
      "ArticleSearchParams",
      "ArticleSummaryV2",
      "Availability.type",
      "SearchResultV2",
      "TagsSearchResult",
      "ArticleDump",
      "ValidationError",
      "ArticleIds"
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
