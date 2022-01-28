import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object articleapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.articleapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      jodaTime,
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkV,
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9", // This is needed for javamelody graphing
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
    ) ++ elastic4s ++ database ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies)

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.articleapi.model.api._",
                  "no.ndla.articleapi.model.api.TSTypes._",
                  "no.ndla.articleapi.model.domain.Availability"),
    exports = Seq(
      "ArticleV2",
      "ArticleSearchParams",
      "ArticleSummaryV2",
      "PartialPublishArticle",
      "Availability.type",
      "SearchResultV2",
      "TagsSearchResult",
      "ArticleDump",
      "ValidationError"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "article-api",
    libraryDependencies ++= dependencies
  ) ++
    PactSettings ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings() ++
    tsSettings ++
    fmtSettings

  override lazy val configs: Seq[sbt.librarymanagement.Configuration] = Seq(
    PactTestConfig
  )

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaPactPlugin,
    ScalaTsiPlugin
  )

}
