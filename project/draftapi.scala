import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object draftapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.draftapi.JettyLauncher")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaLanguage,
      ndlaNetwork,
      ndlaMapping,
      ndlaValidation,
      ndlaScalatestsuite,
      elastic4sCore,
      elastic4sHttp,
      elastic4sAWS,
      elastic4sEmbedded % "test",
      scalaTsi,
      "joda-time" % "joda-time" % "2.10",
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.postgresql" % "postgresql" % PostgresV,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkV,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.typelevel" %% "cats-effect" % CatsEffectV,
    ) ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies)

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.draftapi.model.api._", "no.ndla.draftapi.model.api.TSTypes._"),
    exports = Seq(
      "Agreement",
      "Article",
      "NewArticle",
      "UpdatedAgreement",
      "UpdatedArticle",
      "UpdatedUserData",
      "UserData"
    )
  )

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "draft-api",
    libraryDependencies ++= dependencies
  ) ++ PactSettings ++ commonSettings ++ assemblySettings() ++ dockerSettings() ++ tsSettings

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
