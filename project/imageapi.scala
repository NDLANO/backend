import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object imageapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.imageapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      "org.eclipse.jetty"   % "jetty-webapp"                    % JettyV     % "container;compile",
      "org.eclipse.jetty"   % "jetty-plus"                      % JettyV     % "container",
      "javax.servlet"       % "javax.servlet-api"               % "4.0.1"    % "container;provided;test",
      "org.json4s"         %% "json4s-native"                   % Json4SV,
      "com.amazonaws"       % "aws-java-sdk-s3"                 % AwsSdkV,
      "com.amazonaws"       % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "org.scalaj"         %% "scalaj-http"                     % "2.4.2",
      "org.scalatest"      %% "scalatest"                       % ScalaTestV % "test",
      "org.mockito"        %% "mockito-scala"                   % MockitoV   % "test",
      "org.mockito"        %% "mockito-scala-scalatest"         % MockitoV   % "test",
      "org.flywaydb"        % "flyway-core"                     % FlywayV,
      "vc.inreach.aws"      % "aws-signing-request-interceptor" % "0.0.22",
      "org.jsoup"           % "jsoup"                           % JsoupV,
      "net.bull.javamelody" % "javamelody-core"                 % "1.74.0",
      "org.jrobin"          % "jrobin"                          % "1.5.9", // This is needed for javamelody graphing
      "org.imgscalr"        % "imgscalr-lib"                    % "4.2",
      // These are not strictly needed, for most cases, but offers better handling of loading images with encoding issues
      "com.twelvemonkeys.imageio" % "imageio-core" % "3.8.2",
      "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.8.2",
      "commons-io"                % "commons-io"   % "2.11.0"
    ) ++ elastic4s ++ database ++ scalatra ++ vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.imageapi.model.api._"),
    exports = Seq(
      "Image",
      "ImageMetaInformationV2",
      "ImageMetaInformationV3",
      "ImageMetaSummary",
      "NewImageMetaInformationV2",
      "SearchParams",
      "SearchResult",
      "SearchResultV3",
      "TagsSearchResult",
      "UpdateImageMetaInformation",
      "ValidationError"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "image-api",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    dockerSettings("-Xmx6G") ++
    tsSettings ++
    assemblySettings()

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin
  )
}
