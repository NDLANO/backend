import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object draftapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.draftapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      enumeratumJson4s,
      "org.eclipse.jetty"   % "jetty-webapp"                    % JettyV     % "container;compile",
      "org.eclipse.jetty"   % "jetty-plus"                      % JettyV     % "container",
      "javax.servlet"       % "javax.servlet-api"               % "4.0.1"    % "container;provided;test",
      "org.json4s"         %% "json4s-native"                   % Json4SV,
      "vc.inreach.aws"      % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalikejdbc"    %% "scalikejdbc"                     % ScalikeJDBCV,
      "com.amazonaws"       % "aws-java-sdk-s3"                 % AwsSdkV,
      "org.scalaj"         %% "scalaj-http"                     % "2.4.2",
      "org.scalatest"      %% "scalatest"                       % ScalaTestV % "test",
      "org.jsoup"           % "jsoup"                           % "1.11.3",
      "net.bull.javamelody" % "javamelody-core"                 % "1.74.0",
      "org.jrobin"          % "jrobin"                          % "1.5.9",
      "com.amazonaws"       % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "org.mockito"        %% "mockito-scala"                   % MockitoV   % "test",
      "org.mockito"        %% "mockito-scala-scalatest"         % MockitoV   % "test",
      "org.flywaydb"        % "flyway-core"                     % FlywayV,
      "org.typelevel"      %% "cats-effect"                     % CatsEffectV
    ) ++ elastic4s ++ database ++ scalatra ++ vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq(
      "no.ndla.draftapi.model.api._",
      "no.ndla.draftapi.model.api.TSTypes._"
    ),
    exports = Seq(
      "Agreement",
      "Article",
      "Availability.type",
      "NewAgreement",
      "NewArticle",
      "SearchResult",
      "GrepCodesSearchResult",
      "TagsSearchResult",
      "AgreementSearchResult",
      "UpdatedAgreement",
      "UpdatedArticle",
      "UpdatedUserData",
      "UploadedFile",
      "UserData"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "draft-api",
    libraryDependencies ++= dependencies
  ) ++ commonSettings ++ assemblySettings() ++ dockerSettings() ++ tsSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin
  )

}
