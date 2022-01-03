import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object imageapi {
  lazy val mainClass = "no.ndla.imageapi.JettyLauncher"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaLanguage,
      ndlaMapping,
      ndlaNetwork,
      ndlaScalatestsuite,
      elastic4sCore,
      elastic4sHttp,
      scalaTsi,
      "joda-time" % "joda-time" % "2.10",
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "org.postgresql" % "postgresql" % PostgresV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkV,
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
      "org.elasticsearch.client" % "elasticsearch-rest-high-level-client" % ElasticsearchV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9", // This is needed for javamelody graphing
      "org.imgscalr" % "imgscalr-lib" % "4.2",
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      // These are not strictly needed, for most cases, but offers better handling of loading images with encoding issues
      "com.twelvemonkeys.imageio" % "imageio-core" % "3.4.1",
      "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.4.1",
      "commons-io" % "commons-io" % "2.6"
    ) ++ scalatra ++ vulnerabilityOverrides)

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    name = "image-api",
    imports = Seq("no.ndla.imageapi.model.api._"),
    exports = Seq(
      "Image",
      "ImageMetaInformationV2",
      "ImageMetaSummary",
      "NewImageMetaInformationV2",
      "SearchParams",
      "SearchResult",
      "TagsSearchResult",
      "UpdateImageMetaInformation",
      "ValidationError",
    )
  )

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "image-api",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    dockerSettings("-Xmx4G") ++
    assemblySettings(mainClass) ++
    fmtSettings

  lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin
  )
}
