import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object articleapi {
  lazy val mainClass = "no.ndla.articleapi.JettyLauncher"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaLanguage,
      ndlaNetwork,
      ndlaMapping,
      ndlaValidation,
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
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9", // This is needed for javamelody graphing
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
      "io.lemonlabs" %% "scala-uri" % "1.5.1"
    ) ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies)

  val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    name = "article-api",
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

  val settings: Seq[Def.Setting[_]] = Seq(
    name := "article-api",
    libraryDependencies ++= dependencies
  ) ++
    PactSettings ++
    commonSettings ++
    assemblySettings(mainClass) ++
    dockerSettings() ++
    tsSettings ++
    fmtSettings

  val configs: Seq[sbt.librarymanagement.Configuration] = Seq(
    PactTestConfig
  )

  val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaPactPlugin,
    ScalaTsiPlugin
  )

}
