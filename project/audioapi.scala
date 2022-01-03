import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object audioapi {
  lazy val mainClass = "no.ndla.audioapi.JettyLauncher"
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
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
      "io.lemonlabs" %% "scala-uri" % "3.2.0",
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "org.typelevel" %% "cats-effect" % CatsEffectV,
    ) ++ scalatra ++ vulnerabilityOverrides)

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    name = "audio-api",
    imports = Seq("no.ndla.audioapi.model.api._"),
    exports = Seq(
      "Audio",
      "AudioSummarySearchResult",
      "NewAudioMetaInformation",
      "NewSeries",
      "SearchParams",
      "Series",
      "SeriesSummary",
      "AudioSummary",
      "TagsSearchResult",
      "AudioMetaInformation",
      "UpdatedAudioMetaInformation",
      "SeriesSummarySearchResult",
      "SeriesSearchParams",
      "ValidationError"
    )
  )

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "audio-api",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings(mainClass) ++
    dockerSettings() ++
    tsSettings ++
    fmtSettings

  lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin
  )
}
