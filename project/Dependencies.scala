import sbt.Keys._
import sbt._

object Dependencies {

  /** Function to simplify depending on another component in the tests */
  def testWith(dep: Project, withTests: Boolean = false): ClasspathDependency =
    if (withTests) dep % "test->compile;test->test" else dep % "test"

  object versions {
    val ScalaV                = "2.13.8"
    val ScalatraV             = "2.8.2"
    val HikariConnectionPoolV = "4.0.1"
    val ScalaLoggingV         = "3.9.4"
    val ScalaTestV            = "3.2.10"
    val Log4JV                = "2.17.1"
    val JettyV                = "9.4.48.v20220622"
    val AwsSdkV               = "1.12.276"
    val MockitoV              = "1.16.49"
    val Elastic4sV            = "7.16.3"
    val JacksonV              = "2.13.3"
    val CatsEffectV           = "2.1.2"
    val ElasticsearchV        = "7.16.2"
    val Json4SV               = "4.0.3"
    val JavaxServletV         = "4.0.1"
    val FlywayV               = "7.5.3"
    val PostgresV             = "42.4.1"
    val ScalaTsiV             = "0.6.0"
    val Http4sV               = "0.21.33"
    val RhoV                  = "0.21.0"
    val CirceV                = "0.14.2"
    val ScalikeJDBCV          = "4.0.0-RC2"
    val TestContainersV       = "1.15.1"
    val JsoupV                = "1.15.2"

    lazy val scalaUri = "io.lemonlabs" %% "scala-uri" % "3.5.0"

    lazy val scalikejdbc = "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV
    lazy val postgres    = "org.postgresql"   % "postgresql"  % PostgresV
    lazy val hikari      = "com.zaxxer"       % "HikariCP"    % HikariConnectionPoolV

    lazy val enumeratum       = "com.beachape" %% "enumeratum"        % "1.7.0"
    lazy val enumeratumJson4s = "com.beachape" %% "enumeratum-json4s" % "1.7.1"
    lazy val enumeratumCirce  = "com.beachape" %% "enumeratum-circe"  % "1.7.0"

    lazy val database = Seq(
      scalikejdbc,
      postgres,
      hikari
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
      "org.apache.logging.log4j"    % "log4j-api"        % Log4JV,
      "org.apache.logging.log4j"    % "log4j-core"       % Log4JV,
      "org.apache.logging.log4j"    % "log4j-slf4j-impl" % Log4JV,
      "com.typesafe.scala-logging" %% "scala-logging"    % ScalaLoggingV,
      "org.slf4j"                   % "slf4j-api"        % "1.7.32",
      // We need jackson stuff to load `log4j2.yaml`
      "com.fasterxml.jackson.core"       % "jackson-core"            % JacksonV,
      "com.fasterxml.jackson.core"       % "jackson-databind"        % JacksonV,
      "com.fasterxml.jackson.dataformat" % "jackson-dataformat-yaml" % JacksonV
    )

    // Sometimes we override transitive dependencies because of vulnerabilities, we put these here
    lazy val vulnerabilityOverrides = Seq(
      "com.google.guava"          % "guava"         % "30.0-jre",
      "commons-codec"             % "commons-codec" % "1.14",
      "org.apache.httpcomponents" % "httpclient"    % "4.5.13",
      "org.yaml"                  % "snakeyaml"     % "1.26"
    )
  }
}
