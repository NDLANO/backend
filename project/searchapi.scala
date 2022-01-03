import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object searchapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.searchapi.JettyLauncher")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaLanguage,
      ndlaMapping,
      ndlaNetwork,
      ndlaScalatestsuite,
      scalaTsi,
      "joda-time" % "joda-time" % "2.10",
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sV,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.json4s" %% "json4s-ext" % Json4SV,
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test"
    ) ++ scalatra ++ pactTestFrameworkDependencies ++ vulnerabilityOverrides)

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

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "search-api",
    libraryDependencies ++= dependencies
  ) ++
    PactSettings ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings("-Xmx2G") ++
    tsSettings ++
    fmtSettings

  lazy val configs: Seq[sbt.librarymanagement.Configuration] = Seq(
    PactTestConfig
  )

  lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaPactPlugin,
    ScalaTsiPlugin
  )

}
