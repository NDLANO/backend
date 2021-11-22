import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt.{Def, _}
import sbtassembly.AssemblyKeys._
import sbtassembly._
import sbtdocker.DockerKeys._
import sbtdocker._
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport._

object Dependencies {

  object common {
    lazy val commonSettings = Seq(
      organization := "ndla",
      version := "0.0.1",
      scalaVersion := ScalaV,
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
      Test / parallelExecution := false,
      resolvers ++= scala.util.Properties
        .envOrNone("NDLA_RELEASES")
        .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
        .toSeq
    )

    val ScalaV = "2.13.2"
    val ScalatraV = "2.8.2"
    val HikariConnectionPoolV = "4.0.1"
    val ScalaLoggingV = "3.9.2"
    val ScalaTestV = "3.2.1"
    val Log4JV = "2.13.3"
    val JettyV = "9.4.35.v20201120"
    val AwsSdkV = "1.11.658"
    val MockitoV = "1.14.8"
    val Elastic4sV = "6.7.8"
    val JacksonV = "2.12.1"
    val CatsEffectV = "2.1.2"
    val ElasticsearchV = "6.8.13"
    val Json4SV = "4.0.3"
    val FlywayV = "7.5.3"
    val PostgresV = "42.2.18"
    val PactV = "2.3.16"
    val ScalaTsiV = "0.5.1"

    lazy val pactTestFrameworkDependencies = Seq(
      "com.itv" %% "scalapact-circe-0-13" % PactV % "test",
      "com.itv" %% "scalapact-http4s-0-21" % PactV % "test",
      "com.itv" %% "scalapact-scalatest" % PactV % "test"
    )

    lazy val ndlaNetwork = "ndla" %% "network" % "0.47"
    lazy val ndlaMapping = "ndla" %% "mapping" % "0.15"
    lazy val ndlaValidation = "ndla" %% "validation" % "0.52"
    lazy val ndlaLanguage = "ndla" %% "language" % "1.0.0"
    lazy val ndlaScalatestsuite = "ndla" %% "scalatestsuite" % "0.3" % "test"

    lazy val scalaTsi = "com.scalatsi" %% "scala-tsi" % ScalaTsiV

    lazy val scalatra = Seq(
      "org.scalatra" %% "scalatra" % ScalatraV,
      "org.scalatra" %% "scalatra-json" % ScalatraV,
      "org.scalatra" %% "scalatra-swagger" % ScalatraV,
      "org.scalatra" %% "scalatra-scalatest" % ScalatraV % "test"
    )

    lazy val elastic4sCore = "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sV
    lazy val elastic4sHttp = "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sV
    lazy val elastic4sAWS = "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sV
    lazy val elastic4sEmbedded = "com.sksamuel.elastic4s" %% "elastic4s-embedded" % Elastic4sV

    lazy val log4j = Seq(
      "org.apache.logging.log4j" % "log4j-api" % Log4JV,
      "org.apache.logging.log4j" % "log4j-core" % Log4JV,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JV,
    )

    lazy val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingV

    lazy val logging: Seq[ModuleID] = log4j :+ scalaLogging

    // Sometimes we override transitive dependencies because of vulnerabilities, we put these here
    lazy val vulnerabilityOverrides = Seq(
      "com.fasterxml.jackson.core" % "jackson-core" % JacksonV,
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonV,
      "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonV,
      "com.google.guava" % "guava" % "30.0-jre",
      "commons-codec" % "commons-codec" % "1.14",
      "org.apache.httpcomponents" % "httpclient" % "4.5.13",
      "org.yaml" % "snakeyaml" % "1.26"
    )

    lazy val PactTestConfig = config("PactTest") extend(Test)
    lazy val PactSettings: Seq[Def.Setting[_]] = inConfig(PactTestConfig)(Defaults.testTasks) ++ Seq(
      // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
      // To run all tests use pact config 'sbt PactTest/test' (or 'sbt article_api/PactTest/test' for a single subproject)
      Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
      Test / testOnly / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),

      PactTestConfig / testOptions := Seq.empty,
      PactTestConfig / testOnly / testOptions := Seq.empty
    )

    lazy val assemblySettings = Seq(
      assembly / assemblyJarName := name.value + ".jar",
      assembly / assemblyMergeStrategy := {
        case "module-info.class"                                           => MergeStrategy.discard
        case x if x.endsWith("/module-info.class")                         => MergeStrategy.discard
        case "mime.types"                                                  => MergeStrategy.filterDistinctLines
        case PathList("org", "joda", "convert", "ToString.class")          => MergeStrategy.first
        case PathList("org", "joda", "convert", "FromString.class")        => MergeStrategy.first
        case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
        case x =>
          val oldStrategy = (assembly / assemblyMergeStrategy).value
          oldStrategy(x)
      }
    )

    lazy val dockerSettings = Seq(
      docker := (docker dependsOn assembly).value,
      docker / dockerfile := {
        val artifact = (assembly / assemblyOutputPath).value
        val artifactTargetPath = s"/app/${artifact.name}"
        new Dockerfile {
          from("adoptopenjdk/openjdk11:alpine-slim")
          run("apk", "--no-cache", "add", "ttf-dejavu")
          add(artifact, artifactTargetPath)
          entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
        }
      },
      docker / imageNames := Seq(
        ImageName(namespace = Some(organization.value),
                  repository = name.value,
                  tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
      )
    )

  }

  import common._

  object articleapi {
    lazy val dependencies: Seq[ModuleID] = Seq(
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
      "org.scalikejdbc" %% "scalikejdbc" % "4.0.0-RC2",
      "org.postgresql" % "postgresql" % PostgresV,
      "com.zaxxer" % "HikariCP" % "3.4.5",
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
    ) ++ logging ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies

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
      libraryDependencies := dependencies
    ) ++ PactSettings ++ commonSettings ++ assemblySettings ++ dockerSettings ++ tsSettings

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

  object audioapi {
    lazy val dependencies: Seq[ModuleID] = Seq(
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
    "org.scalikejdbc" %% "scalikejdbc" % "4.0.0-RC2",
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
      ) ++ scalatra ++ logging ++ vulnerabilityOverrides

    lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
      name = "audio-api",
      imports = Seq("no.ndla.audioapi.model.api._" ),
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
      libraryDependencies := dependencies
    ) ++ commonSettings ++ assemblySettings ++ dockerSettings ++ tsSettings

    lazy val plugins: Seq[sbt.Plugins] = Seq(
      DockerPlugin,
      JettyPlugin,
      ScalaTsiPlugin
    )
  }

  object conceptapi {
    lazy val dependencies: Seq[ModuleID] = Seq(
      ndlaLanguage,
      ndlaNetwork,
      ndlaMapping,
      ndlaValidation,
      ndlaScalatestsuite,
      elastic4sCore,
      elastic4sHttp,
      scalaTsi,
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
      "org.scalikejdbc" %% "scalikejdbc" % "4.0.0-RC2",
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.postgresql" % "postgresql" % PostgresV,
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.typelevel" %% "cats-core" % CatsEffectV,
      "org.typelevel" %% "cats-effect" % CatsEffectV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22"
    ) ++ scalatra ++ logging ++ vulnerabilityOverrides

    lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
      name = "concept-api",
      imports = Seq("no.ndla.conceptapi.model.api._", "no.ndla.conceptapi.model.api.TSTypes._"),
      exports = Seq(
        "Concept",
        "ConceptSearchParams",
        "ConceptSearchResult",
        "ConceptSummary",
        "DraftConceptSearchParams",
        "NewConcept",
        "SubjectTags",
        "TagsSearchResult",
        "UpdatedConcept",
        "ValidationError",
      )
    )

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "concept-api",
      libraryDependencies := dependencies
    ) ++ commonSettings ++ assemblySettings ++ dockerSettings ++ tsSettings

    lazy val plugins: Seq[sbt.Plugins] = Seq(
      DockerPlugin,
      JettyPlugin,
      ScalaTsiPlugin
    )

  }

  object draftapi {
    lazy val dependencies: Seq[ModuleID] = Seq(
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
      "org.apache.logging.log4j" % "log4j-api" % Log4JV,
      "org.apache.logging.log4j" % "log4j-core" % Log4JV,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalikejdbc" %% "scalikejdbc" % "4.0.0-RC2",
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
      "org.slf4j" % "slf4j-api" % "1.7.30"
    ) ++ logging ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies
    // Excluding slf4j-api (and specifically adding 1.7.30) because of conflict between 1.7.30 and 2.0.0-alpha1
      .map(_.exclude("org.slf4j", "slf4j-api"))

    lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
      name = "draft-api",
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
      libraryDependencies := dependencies
    ) ++ PactSettings ++ commonSettings ++ assemblySettings ++ dockerSettings ++ tsSettings

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

  private def typescriptSettings(name: String, imports: Seq[String], exports: Seq[String]) = {
    Seq(
      typescriptGenerationImports := imports,
      typescriptExports := exports,
      typescriptOutputFile := baseDirectory.value / name / "typescript" / "index.ts"
    )
  }
}
