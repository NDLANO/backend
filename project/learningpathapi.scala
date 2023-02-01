import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object learningpathapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.learningpathapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      enumeratumJson4s,
      sttp,
      "org.eclipse.jetty"   % "jetty-webapp"                    % JettyV     % "container;compile",
      "org.eclipse.jetty"   % "jetty-plus"                      % JettyV     % "container",
      "javax.servlet"       % "javax.servlet-api"               % "4.0.1"    % "container;provided;test",
      "org.json4s"         %% "json4s-native"                   % Json4SV,
      "org.json4s"         %% "json4s-ast"                      % Json4SV,
      "org.json4s"         %% "json4s-core"                     % Json4SV,
      "org.json4s"         %% "json4s-ext"                      % Json4SV,
      "vc.inreach.aws"      % "aws-signing-request-interceptor" % "0.0.22",
      "org.typelevel"      %% "cats-effect"                     % CatsEffectV,
      "org.jsoup"           % "jsoup"                           % JsoupV,
      "net.bull.javamelody" % "javamelody-core"                 % JavaMelodyV,
      "org.jrobin"          % "jrobin"                          % "1.5.9",
      "com.amazonaws"       % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "org.scalatest"      %% "scalatest"                       % ScalaTestV % "test",
      "org.mockito"        %% "mockito-scala"                   % MockitoV   % "test",
      "org.mockito"        %% "mockito-scala-scalatest"         % MockitoV   % "test",
      "org.flywaydb"        % "flyway-core"                     % FlywayV
    ) ++ elastic4s ++ database ++ scalatra ++ vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.learningpathapi.model.api._", "no.ndla.learningpathapi.model.api.config._"),
    exports = Seq(
      "Author",
      "Error",
      "ConfigMetaRestricted",
      "LearningPathStatus",
      "LearningPathSummaryV2",
      "LearningPathTagsSummary",
      "LearningPathV2",
      "LearningStepContainerSummary",
      "LearningStepSeqNo",
      "LearningStepStatus",
      "LearningStepSummaryV2",
      "LearningStepV2",
      "License",
      "SearchResultV2",
      "MyNDLAUser",
      "config.ConfigMeta",
      "Folder",
      "FolderData",
      "NewFolder",
      "UpdatedFolder",
      "NewResource",
      "UpdatedResource"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "learningpath-api",
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
