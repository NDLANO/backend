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
    val Log4JV = "2.16.0"
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
    val Http4sV = "0.21.21"
    val RhoV = "0.21.0"
    val CirceV = "0.13.0"
    val ScalikeJDBCV = "4.0.0-RC2"
    val TestContainersV = "1.15.1"

    lazy val pactTestFrameworkDependencies = Seq(
      "com.itv" %% "scalapact-circe-0-13" % PactV % "test",
      "com.itv" %% "scalapact-http4s-0-21" % PactV % "test",
      "com.itv" %% "scalapact-scalatest" % PactV % "test"
    )

    lazy val ndlaNetwork = "ndla" %% "network" % "0.47"
    lazy val ndlaMapping = "ndla" %% "mapping" % "0.15"
    lazy val ndlaValidation = "ndla" %% "validation" % "0.53"
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

    def dockerSettings(extraJavaOpts: String*): Seq[Def.Setting[_]] = {
      Seq(
        docker := (docker dependsOn assembly).value,
        docker / dockerfile := {
          val artifact = (assembly / assemblyOutputPath).value
          val artifactTargetPath = s"/app/${artifact.name}"

          val entry = Seq(
            "java",
            "-Dorg.scalatra.environment=production",
          ) ++
            extraJavaOpts ++
            Seq("-jar", artifactTargetPath)

          new Dockerfile {
            from("adoptopenjdk/openjdk11:alpine-slim")
            run("apk", "--no-cache", "add", "ttf-dejavu")
            add(artifact, artifactTargetPath)
            entryPoint(entry:_*)
          }
        },
        docker / imageNames := Seq(
          ImageName(namespace = Some(organization.value),
            repository = name.value,
            tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
        )
      )
    }

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
    ) ++ PactSettings ++ commonSettings ++ assemblySettings ++ dockerSettings() ++ tsSettings

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
    ) ++ commonSettings ++ assemblySettings ++ dockerSettings() ++ tsSettings

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
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.postgresql" % "postgresql" % PostgresV,
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.typelevel" %% "cats-core" % "2.1.1",
      "org.typelevel" %% "cats-effect" % "2.1.1",
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
    ) ++ commonSettings ++ assemblySettings ++ dockerSettings() ++ tsSettings

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
    ) ++ PactSettings ++ commonSettings ++ assemblySettings ++ dockerSettings() ++ tsSettings

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

  object frontpageapi {
    lazy val dependencies: Seq[ModuleID] = Seq(
      ndlaNetwork,
      ndlaMapping,
      ndlaScalatestsuite,
      scalaTsi,
      "org.http4s" %% "http4s-circe" % Http4sV,
      "io.circe" %% "circe-generic" % CirceV,
      "io.circe" %% "circe-generic-extras" % CirceV,
      "io.circe" %% "circe-literal" % CirceV,
      "io.circe" %% "circe-parser" % CirceV,
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "org.postgresql" % "postgresql" % PostgresV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.http4s" %% "rho-swagger" % RhoV,
      "org.http4s" %% "http4s-server" % Http4sV,
      "org.http4s" %% "http4s-dsl" % Http4sV,
      "org.http4s" %% "http4s-blaze-server" % Http4sV,
      "org.flywaydb" % "flyway-core" % FlywayV,
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "javax.servlet" % "javax.servlet-api" % "4.0.1"
    ) ++ logging ++ vulnerabilityOverrides

    lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
      name = "frontpage-api",
      imports = Seq("no.ndla.frontpageapi.model.api._"),
      exports = Seq(
        "FrontPageData",
        "FilmFrontPageData",
        "NewOrUpdatedFilmFrontPageData",
        "SubjectPageData",
        "NewSubjectFrontPageData",
        "UpdatedSubjectFrontPageData",
        "Error"
      )
    )

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "frontpage-api",
      libraryDependencies := dependencies
    ) ++ commonSettings ++ assemblySettings ++ dockerSettings() ++ tsSettings

    lazy val plugins: Seq[sbt.Plugins] = Seq(
      DockerPlugin,
      ScalaTsiPlugin
    )

  }

  object imageapi {
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
    ) ++ scalatra ++ logging ++ vulnerabilityOverrides

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
      libraryDependencies := dependencies
    ) ++ commonSettings ++ dockerSettings("-Xmx4G")

    lazy val plugins: Seq[sbt.Plugins] = Seq(
      DockerPlugin,
      JettyPlugin,
      ScalaTsiPlugin
    )
  }

  object languagelib {
    lazy val dependencies: Seq[ModuleID] = Seq(
      "org.json4s" %% "json4s-native" % Json4SV,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" % "mockito-all" % "1.10.19" % "test"
    )

    private val scala213 = ScalaV
    private val scala212 = "2.12.10"
    private val supportedScalaVersions = List(scala213, scala212)

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "language",
      libraryDependencies := dependencies,
      crossScalaVersions := supportedScalaVersions
    ) ++ commonSettings

    lazy val disablePlugins = Seq(ScalaTsiPlugin)

  }

  object learningpathapi {
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
      "org.json4s" %% "json4s-ast" % Json4SV,
      "org.json4s" %% "json4s-core" % Json4SV,
      "org.json4s" %% "json4s-ext" % Json4SV,
      "org.scalikejdbc" %% "scalikejdbc" % "4.0.0-RC2",
      "org.postgresql" % "postgresql" % PostgresV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.typelevel" %% "cats-effect" % CatsEffectV,
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV
    ) ++ scalatra ++ logging ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies

    lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
      name = "learningpath-api",
      imports = Seq("no.ndla.learningpathapi.model.api._"),
      exports = Seq(
        "Author",
        "Error",
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
        "config.ConfigMeta"
      )
    )

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "learningpath-api",
      libraryDependencies := dependencies
    ) ++ PactSettings ++ commonSettings ++ assemblySettings ++ dockerSettings() ++ tsSettings

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

  object mappinglib {
    lazy val dependencies: Seq[ModuleID] = Seq("org.scalatest" %% "scalatest" % ScalaTestV % "test")

    private val scala213 = ScalaV
    private val scala212 = "2.12.10"
    private val supportedScalaVersions = List(scala213, scala212)

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "mapping",
      libraryDependencies := dependencies,
      crossScalaVersions := supportedScalaVersions
    ) ++ commonSettings

    lazy val disablePlugins = Seq(ScalaTsiPlugin)
  }

  object networklib {
    lazy val dependencies: Seq[ModuleID] = Seq(
      "org.json4s" %% "json4s-jackson" % Json4SV,
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "provided;test",
      "com.github.jwt-scala" %% "jwt-json4s-native" % "9.0.2"
    ) ++ vulnerabilityOverrides

    private val scala213 = ScalaV
    private val scala212 = "2.12.10"
    private val supportedScalaVersions = List(scala213, scala212)

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "network",
      libraryDependencies := dependencies,
      crossScalaVersions := supportedScalaVersions
    ) ++ commonSettings

    lazy val disablePlugins = Seq(ScalaTsiPlugin)
  }

  object oembedproxy {
    lazy val dependencies: Seq[ModuleID] = Seq(
      ndlaNetwork,
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test"
    ) ++ logging ++ scalatra ++ vulnerabilityOverrides

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "oembed-proxy",
      libraryDependencies := dependencies
    ) ++ commonSettings ++ assemblySettings ++ dockerSettings()

    lazy val plugins = Seq(
      JettyPlugin,
      DockerPlugin
    )

    lazy val disablePlugins = Seq(
      ScalaTsiPlugin
    )
  }

  object scalatestsuitelib {
    lazy val dependencies: Seq[ModuleID] = Seq(
      ndlaNetwork,
      "org.scalatest" %% "scalatest" % ScalaTestV,
      "org.mockito" %% "mockito-scala" % MockitoV,
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.postgresql" % "postgresql" % PostgresV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.postgresql" % "postgresql" % PostgresV,
      "org.testcontainers" % "elasticsearch" % TestContainersV,
      "org.testcontainers" % "testcontainers" % TestContainersV,
      "org.testcontainers" % "postgresql" % TestContainersV,
      "joda-time" % "joda-time" % "2.10"
    ) ++ vulnerabilityOverrides

    private val scala213 = ScalaV
    private val scala212 = "2.12.10"
    private val supportedScalaVersions = List(scala213, scala212)

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "scalatestsuite",
      libraryDependencies := dependencies,
      crossScalaVersions := supportedScalaVersions
    ) ++ commonSettings

    lazy val disablePlugins = Seq(ScalaTsiPlugin)
  }

  object searchapi {
    lazy val dependencies: Seq[ModuleID] = Seq(
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
    ) ++ scalatra ++ logging ++ pactTestFrameworkDependencies ++ vulnerabilityOverrides

    lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
      name = "search-api",
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
      libraryDependencies := dependencies
    ) ++ PactSettings ++ commonSettings ++ assemblySettings ++ dockerSettings("-Xmx2G") ++ tsSettings

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

  object validationlib {
    lazy val dependencies: Seq[ModuleID] = Seq(
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.json4s" %% "json4s-ext" % Json4SV,
      "io.lemonlabs" %% "scala-uri" % "1.5.1"
    )

    private val scala213 = ScalaV
    private val scala212 = "2.12.10"
    private val supportedScalaVersions = List(scala213, scala212)

    lazy val settings: Seq[Def.Setting[_]] = Seq(
      name := "validation",
      libraryDependencies := dependencies,
      crossScalaVersions := supportedScalaVersions
    ) ++ commonSettings

    lazy val disablePlugins = Seq(ScalaTsiPlugin)
  }

  private def typescriptSettings(name: String, imports: Seq[String], exports: Seq[String]) = {
    Seq(
      typescriptGenerationImports := imports,
      typescriptExports := exports,
      typescriptOutputFile := baseDirectory.value / name / "typescript" / "index.ts"
    )
  }
}
