import sbt.Keys._
import sbt._

object Dependencies {

  /** Function to simplify depending on another component in the tests */
  def testWith(dep: Project, withTests: Boolean = false): ClasspathDependency =
    if (withTests) dep % "test->compile;test->test" else dep % "test"

  object versions {
    val ScalaV                = "2.13.10"
    val ScalatraV             = "2.8.4"
    val HikariConnectionPoolV = "5.0.1"
    val ScalaLoggingV         = "3.9.5"
    val ScalaTestV            = "3.2.10"
    val Log4JV                = "2.19.0"
    val JettyV                = "9.4.48.v20220622"
    val AwsSdkV               = "1.12.276"
    val MockitoV              = "1.17.22"
    val Elastic4sV            = "8.5.0"
    val JacksonV              = "2.14.1"
    val CatsEffectV           = "3.5.1"
    val ElasticsearchV        = "7.16.2"
    val Json4SV               = "4.0.6"
    val JavaxServletV         = "4.0.1"
    val FlywayV               = "7.5.3"
    val PostgresV             = "42.5.1"
    val ScalaTsiV             = "0.6.0"
    val Http4sV               = "0.23.23"
    val TapirV                = "1.7.3"
    val ApiSpecV              = "0.6.0"
    val SttpV                 = "3.9.0"
    val CirceV                = "0.14.2"
    val ScalikeJDBCV          = "4.0.0"
    val TestContainersV       = "1.17.6"
    val JsoupV                = "1.15.3"
    val JavaMelodyV           = "1.92.0"

    lazy val scalaUri = "io.lemonlabs" %% "scala-uri" % "3.5.0"

    lazy val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV
    lazy val postgres    = "org.postgresql"   % "postgresql"  % PostgresV
    lazy val hikari      = "com.zaxxer"       % "HikariCP"    % HikariConnectionPoolV

    lazy val sttp = "com.softwaremill.sttp.client3" %% "core" % SttpV

    lazy val enumeratum       = "com.beachape" %% "enumeratum"        % "1.7.0"
    lazy val enumeratumJson4s = "com.beachape" %% "enumeratum-json4s" % "1.7.1"
    lazy val enumeratumCirce  = "com.beachape" %% "enumeratum-circe"  % "1.7.0"

    lazy val database: Seq[ModuleID] = Seq(
      scalikejdbc,
      postgres,
      hikari
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
      "io.circe" %% "circe-generic"        % CirceV,
      "io.circe" %% "circe-generic-extras" % CirceV,
      "io.circe" %% "circe-literal"        % CirceV,
      "io.circe" %% "circe-parser"         % CirceV
    )

    lazy val http4s: Seq[ModuleID] = Seq(
      "org.http4s" %% "http4s-server"       % Http4sV,
      "org.http4s" %% "http4s-dsl"          % Http4sV,
      "org.http4s" %% "http4s-circe"        % Http4sV,
      "org.http4s" %% "http4s-ember-server" % Http4sV,
      "org.http4s" %% "http4s-jetty-server" % "0.23.13"
    )

    lazy val tapir: Seq[ModuleID] = Seq(
      "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"     % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle" % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"        % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-jdkhttp-server"    % TapirV,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"      % ApiSpecV
    ).map {
      // NOTE: tapir-jdkhttp-server includes some logback provider for slf4j
      //       this conflicts with the existing provider, so lets exclude it.
      _.exclude("ch.qos.logback", "logback-classic").exclude("ch.qos.logback", "logback-core")
    }

    lazy val catsEffect: ModuleID                = "org.typelevel" %% "cats-effect" % CatsEffectV
    lazy val tapirHttp4sCirce: Seq[sbt.ModuleID] = circe ++ http4s ++ tapir :+ catsEffect

    lazy val scalatra: Seq[ModuleID] = Seq(
      "org.scalatra" %% "scalatra"           % ScalatraV,
      "org.scalatra" %% "scalatra-json"      % ScalatraV,
      "org.scalatra" %% "scalatra-swagger"   % ScalatraV,
      "org.scalatra" %% "scalatra-scalatest" % ScalatraV % "test"
    )

    lazy val elastic4s: Seq[ModuleID] = Seq(
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % Elastic4sV,
      "com.sksamuel.elastic4s" %% "elastic4s-testkit"       % Elastic4sV % "test"
    )

    lazy val logging: Seq[ModuleID] = Seq(
      "org.apache.logging.log4j"    % "log4j-api"         % Log4JV,
      "org.apache.logging.log4j"    % "log4j-core"        % Log4JV,
      "org.apache.logging.log4j"    % "log4j-slf4j2-impl" % Log4JV,
      "com.typesafe.scala-logging" %% "scala-logging"     % ScalaLoggingV,
      "org.slf4j"                   % "slf4j-api"         % "2.0.5",
      // We need jackson to load `log4j2.yaml`
      "com.fasterxml.jackson.core"       % "jackson-core"            % JacksonV,
      "com.fasterxml.jackson.core"       % "jackson-databind"        % JacksonV,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonV
    )

    // Sometimes we override transitive dependencies because of vulnerabilities, we put these here
    lazy val vulnerabilityOverrides: Seq[ModuleID] = Seq(
      "com.google.guava"          % "guava"         % "30.0-jre",
      "commons-codec"             % "commons-codec" % "1.15",
      "org.apache.httpcomponents" % "httpclient"    % "4.5.13",
      "org.yaml"                  % "snakeyaml"     % "1.33"
    )
  }
}
