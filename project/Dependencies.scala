import sbt.Keys._
import sbt._

object Dependencies {

  /** Function to simplify depending on another component in the tests */
  def testWith(dep: Project, withTests: Boolean = false): ClasspathDependency =
    if (withTests) dep % "test->compile;test->test" else dep % "test"

  object versions {
    val ScalaV                = "2.13.13"
    val HikariConnectionPoolV = "5.1.0"
    val ScalaLoggingV         = "3.9.5"
    val ScalaTestV            = "3.2.18"
    val Log4JV                = "2.23.1"
    val AwsSdkV               = "1.12.669"
    val MockitoV              = "1.17.30"
    val Elastic4sV            = "8.11.5"
    val JacksonV              = "2.16.1"
    val CatsEffectV           = "3.5.3"
    val FlywayV               = "10.8.1"
    val PostgresV             = "42.7.2"
    val ScalaTsiV             = "0.8.3"
    val Http4sV               = "0.23.25"
    val TapirV                = "1.9.10"
    val ApiSpecV              = "0.7.4"
    val SttpV                 = "3.9.2"
    val CirceV                = "0.14.6"
    val ScalikeJDBCV          = "4.2.0"
    val TestContainersV       = "1.19.4"
    val JsoupV                = "1.17.2"
    val JavaMelodyV           = "1.92.0"
    val EnumeratumV           = "1.7.3"
    val FlexmarkV             = "0.64.8"

    lazy val scalaUri = ("io.lemonlabs" %% "scala-uri" % "4.0.3").excludeAll("org.typelevel", "cats-parse")

    lazy val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV
    lazy val postgres    = "org.postgresql"   % "postgresql"  % PostgresV
    lazy val hikari      = "com.zaxxer"       % "HikariCP"    % HikariConnectionPoolV

    lazy val sttp = "com.softwaremill.sttp.client3" %% "core" % SttpV

    lazy val mockito = Seq("org.scalatestplus" %% "mockito-5-10" % "3.2.18.0")

    // Maybe remove flexmark when markdown migration is complete
    lazy val flexmark: Seq[ModuleID] = Seq(
      "com.vladsch.flexmark" % "flexmark"                       % FlexmarkV,
      "com.vladsch.flexmark" % "flexmark-util-data"             % FlexmarkV,
      "com.vladsch.flexmark" % "flexmark-ext-gfm-strikethrough" % FlexmarkV,
      "com.vladsch.flexmark" % "flexmark-ext-superscript"       % FlexmarkV
    )

    lazy val enumeratum      = "com.beachape" %% "enumeratum"       % EnumeratumV
    lazy val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % EnumeratumV

    lazy val database: Seq[ModuleID] = Seq(
      scalikejdbc,
      postgres,
      hikari,
      "org.flywaydb" % "flyway-core"                % FlywayV,
      "org.flywaydb" % "flyway-database-postgresql" % FlywayV
    )

    lazy val awsS3: Seq[ModuleID] = Seq(
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkV,
      // NOTE: The s3 client uses JAXB for some performance improvements:^)
      //       A warning is logged when uploading stuff to s3 if not included
      "javax.xml.bind" % "jaxb-api" % "2.3.1"
    )

    lazy val jsoup = "org.jsoup" % "jsoup" % JsoupV

    lazy val melody: Seq[ModuleID] = Seq(
      "net.bull.javamelody" % "javamelody-core" % JavaMelodyV,
      "org.jrobin"          % "jrobin"          % "1.5.9" // This is needed for javamelody graphing
    )

    lazy val scalaTsi = "com.scalatsi" %% "scala-tsi" % ScalaTsiV

    lazy val circe: Seq[ModuleID] = Seq(
      "io.circe" %% "circe-generic" % CirceV,
      "io.circe" %% "circe-literal" % CirceV,
      "io.circe" %% "circe-parser"  % CirceV
    )

    lazy val http4s: Seq[ModuleID] = Seq(
      "org.http4s"    %% "http4s-server"       % Http4sV,
      "org.http4s"    %% "http4s-dsl"          % Http4sV,
      "org.http4s"    %% "http4s-circe"        % Http4sV,
      "org.http4s"    %% "http4s-ember-server" % Http4sV,
      "org.typelevel" %% "cats-parse"          % "1.0.0"
    )

    lazy val tapir: Seq[ModuleID] = Seq(
      "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"      % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle"  % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-enumeratum"         % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"         % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-jdkhttp-server"     % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-prometheus-metrics" % TapirV,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"       % ApiSpecV,
      "com.softwaremill.sttp.tapir"   %% "tapir-testing"            % TapirV % "test"
    ).map {
      // NOTE: tapir-jdkhttp-server includes some logback provider for slf4j
      //       this conflicts with the existing provider, so lets exclude it.
      _.exclude("ch.qos.logback", "logback-classic").exclude("ch.qos.logback", "logback-core")
    }

    lazy val catsEffect: ModuleID                = "org.typelevel" %% "cats-effect" % CatsEffectV
    lazy val tapirHttp4sCirce: Seq[sbt.ModuleID] = circe ++ http4s ++ tapir :+ catsEffect

    lazy val elastic4s: Seq[ModuleID] = Seq(
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % Elastic4sV,
      "com.sksamuel.elastic4s" %% "elastic4s-testkit"       % Elastic4sV % "test"
    )

    lazy val logging: Seq[ModuleID] = Seq(
      "org.apache.logging.log4j"    % "log4j-api"         % Log4JV,
      "org.apache.logging.log4j"    % "log4j-core"        % Log4JV,
      "org.apache.logging.log4j"    % "log4j-slf4j2-impl" % Log4JV,
      "com.typesafe.scala-logging" %% "scala-logging"     % ScalaLoggingV,
      "org.slf4j"                   % "slf4j-api"         % "2.0.12",
      // We need jackson to load `log4j2.yaml`
      "com.fasterxml.jackson.core"       % "jackson-core"            % JacksonV,
      "com.fasterxml.jackson.core"       % "jackson-databind"        % JacksonV,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonV,
      "com.fasterxml.jackson.module"    %% "jackson-module-scala"    % JacksonV
    )

    // Sometimes we override transitive dependencies because of vulnerabilities, we put these here
    lazy val vulnerabilityOverrides: Seq[ModuleID] = Seq(
      "commons-codec"             % "commons-codec" % "1.16.0",
      "org.apache.httpcomponents" % "httpclient"    % "4.5.14",
      "org.yaml"                  % "snakeyaml"     % "2.0"
    )
  }
}
