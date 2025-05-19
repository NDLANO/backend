import sbt.{Project, *}

object Dependencies {

  /** Function to simplify depending on another component in the tests */
  def testWith(dep: Project, withTests: Boolean = false): ClasspathDependency =
    if (withTests) dep % "test->compile;test->test" else dep % "test"

  object versions {
    val ScalaV                = "2.13.16"
    val HikariConnectionPoolV = "6.2.1"
    val ScalaLoggingV         = "3.9.5"
    val ScalaTestV            = "3.2.19"
    val Log4JV                = "2.24.3"
    val AwsSdkV               = "2.31.2"
    val MockitoV              = "1.17.30"
    val Elastic4sV            = "8.11.5"
    val JacksonV              = "2.18.3"
    val CatsEffectV           = "3.5.4"
    val FlywayV               = "11.4.0"
    val PostgresV             = "42.7.5"
    val Http4sV               = "0.23.30"
    val TapirV                = "1.11.23"
    val ApiSpecV              = "0.11.7"
    val SttpV                 = "3.9.7"
    val CirceV                = "0.14.12"
    val ScalikeJDBCV          = "4.3.2"
    val TestContainersV       = "1.20.6"
    val JsoupV                = "1.19.1"
    val JavaMelodyV           = "2.5.0"
    val EnumeratumV           = "1.7.5"
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
      "software.amazon.awssdk" % "s3" % AwsSdkV
    )

    lazy val awsTranscribe: Seq[ModuleID] = Seq(
      "software.amazon.awssdk" % "transcribe" % AwsSdkV
    )

    lazy val awsCloudwatch: Seq[ModuleID] = Seq(
      "software.amazon.awssdk" % "cloudwatch" % AwsSdkV
    )

    lazy val jsoup = "org.jsoup" % "jsoup" % JsoupV

    lazy val melody: Seq[ModuleID] = Seq(
      "net.bull.javamelody" % "javamelody-core" % JavaMelodyV,
      "org.jrobin"          % "jrobin"          % "1.5.9" // This is needed for javamelody graphing
    )

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
      "org.apache.logging.log4j"    % "log4j-jul"         % Log4JV,
      "org.apache.logging.log4j"    % "log4j-slf4j2-impl" % Log4JV,
      "com.typesafe.scala-logging" %% "scala-logging"     % ScalaLoggingV,
      "org.slf4j"                   % "slf4j-api"         % "2.0.17",
      // We need jackson to load `log4j2.yaml`
      "com.fasterxml.jackson.core"       % "jackson-core"            % JacksonV,
      "com.fasterxml.jackson.core"       % "jackson-databind"        % JacksonV,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonV,
      "com.fasterxml.jackson.module"    %% "jackson-module-scala"    % JacksonV
    )

    // Sometimes we override transitive dependencies because of vulnerabilities, we put these here
    lazy val vulnerabilityOverrides: Seq[ModuleID] = Seq(
      "commons-codec"             % "commons-codec" % "1.18.0",
      "org.apache.httpcomponents" % "httpclient"    % "4.5.14",
      "org.yaml"                  % "snakeyaml"     % "2.4"
    )
    lazy val jave: Seq[ModuleID] = Seq(
      "ws.schild" % "jave-core"     % "3.5.0",
      "ws.schild" % "jave-all-deps" % "3.5.0"
    )
  }
}
