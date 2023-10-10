import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object draftapi extends Module {
  override val moduleName: String        = "draft-api"
  override val MainClass: Option[String] = Some("no.ndla.draftapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      enumeratumJson4s,
      sttp,
      catsEffect,
      jsoup,
      "org.eclipse.jetty" % "jetty-webapp"                    % JettyV        % "container;compile",
      "org.eclipse.jetty" % "jetty-plus"                      % JettyV        % "container",
      "javax.servlet"     % "javax.servlet-api"               % JavaxServletV % "container;provided;test",
      "org.json4s"       %% "json4s-native"                   % Json4SV,
      "vc.inreach.aws"    % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalikejdbc"  %% "scalikejdbc"                     % ScalikeJDBCV,
      "org.scalatest"    %% "scalatest"                       % ScalaTestV    % "test",
      "com.amazonaws"     % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "org.mockito"      %% "mockito-scala"                   % MockitoV      % "test",
      "org.mockito"      %% "mockito-scala-scalatest"         % MockitoV      % "test",
      "org.flywaydb"      % "flyway-core"                     % FlywayV
    ),
    awsS3,
    melody,
    elastic4s,
    database,
    scalatra,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq(
      "no.ndla.draftapi.model.api._",
      "no.ndla.draftapi.model.api.TSTypes._",
      "no.ndla.common.model.domain.Availability"
    ),
    exports = Seq(
      "Article",
      "Availability.type",
      "NewArticle",
      "SearchResult",
      "GrepCodesSearchResult",
      "TagsSearchResult",
      "UpdatedArticle",
      "UpdatedUserData",
      "UploadedFile",
      "UserData"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++ commonSettings ++ assemblySettings() ++ dockerSettings() ++ tsSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin
  )
}
