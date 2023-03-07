import sbt.Keys._
import sbt._

object Dependencies {

  /** Function to simplify depending on another component in the tests */
  def testWith(dep: Project, withTests: Boolean = false): ClasspathDependency =
    if (withTests) dep % "test->compile;test->test" else dep % "test"

  object versions {
    val ScalaV                = "3.2.2"
    val ScalatraV             = "3.0.0-M3"
    val HikariConnectionPoolV = "5.0.1"
    val ScalaLoggingV         = "3.9.5"
    val ScalaTestV            = "3.2.15"
    val Log4JV                = "2.19.0"
    val JettyV                = "9.4.48.v20220622"
    val AwsSdkV               = "1.12.276"
    val MockitoV              = "1.17.12"
    val Elastic4sV            = "8.5.3"
    val JacksonV              = "2.14.1"
    val CatsEffectV           = "3.4.8"
    val ElasticsearchV        = "7.16.2"
    val Json4SV               = "4.0.6"
    val JavaxServletV         = "4.0.1"
    val FlywayV               = "7.5.3"
    val PostgresV             = "42.5.4"
    val ScalaTsiV             = "0.8.2"
    val Http4sV               = "0.23.18"
    val TapirV                = "1.2.7"
    val ApiSpecV              = "0.3.2"
    val SttpV                 = "3.8.11"
    val CirceV                = "0.14.4"
    val ScalikeJDBCV          = "4.0.0"
    val TestContainersV       = "1.15.1"
    val JsoupV                = "1.15.3"
    val JavaMelodyV           = "1.91.0"
    val EnumeratumV           = "1.7.2"

    lazy val scalaUri = "io.lemonlabs" %% "scala-uri" % "4.0.3"

    lazy val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV
    lazy val postgres    = "org.postgresql"   % "postgresql"  % PostgresV
    lazy val hikari      = "com.zaxxer"       % "HikariCP"    % HikariConnectionPoolV

    lazy val sttp = "com.softwaremill.sttp.client3" %% "core" % SttpV

    lazy val enumeratum      = "com.beachape" %% "enumeratum"       % EnumeratumV
    lazy val enumeratumCirce = "com.beachape" %% "enumeratum-circe" % EnumeratumV

    lazy val database = Seq(
      scalikejdbc,
      postgres,
      hikari
    )

    /** This should only be used if we want to include scalatest and mockito in the main build (not only tests).
      * Otherwise use [[scalaTestAndMockito]]
      */
    lazy val scalaTestAndMockitoInMain = Seq(
      "org.scalatest"     %% "scalatest"   % ScalaTestV,
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0"
    )

    lazy val scalaTestAndMockito = Seq(
      "org.scalatest"     %% "scalatest"   % ScalaTestV % "test",
      "org.scalatestplus" %% "mockito-4-6" % "3.2.15.0" % "test"
    )

    lazy val scalaTsi = "com.scalatsi" %% "scala-tsi" % ScalaTsiV

    lazy val scalatra = Seq(
      "org.scalatra" %% "scalatra"           % ScalatraV,
      "org.scalatra" %% "scalatra-json"      % ScalatraV,
      "org.scalatra" %% "scalatra-swagger"   % ScalatraV,
      "org.scalatra" %% "scalatra-scalatest" % ScalatraV % "test"
    )

    lazy val elastic4s = Seq(
      "com.sksamuel.elastic4s" %% "elastic4s-client-esjava" % Elastic4sV,
      "com.sksamuel.elastic4s" %% "elastic4s-testkit"       % Elastic4sV % "test"
    )

    lazy val logging = Seq(
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
    lazy val vulnerabilityOverrides = Seq(
      "com.google.guava"          % "guava"         % "30.0-jre",
      "commons-codec"             % "commons-codec" % "1.15",
      "org.apache.httpcomponents" % "httpclient"    % "4.5.13",
      "org.yaml"                  % "snakeyaml"     % "1.33"
    )
  }
}
