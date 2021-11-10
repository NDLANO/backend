import sbt.Keys.libraryDependencies
import sbt._

object Dependencies {
  val ScalaV                = "2.13.2"
  val ScalatraV             = "2.8.2"
  val HikariConnectionPoolV = "4.0.1"
  val ScalaLoggingV         = "3.9.2"
  val ScalaTestV            = "3.2.1"
  val Log4JV                = "2.13.3"
  val JettyV                = "9.4.35.v20201120"
  val AwsSdkV               = "1.11.658"
  val MockitoV              = "1.14.8"
  val Elastic4sV            = "6.7.8"
  val JacksonV              = "2.12.1"
  val CatsEffectV           = "2.1.2"
  val ElasticsearchV        = "6.8.13"
  val Json4SV               = "4.0.3"
  val FlywayV               = "7.5.3"
  val PostgresV             = "42.2.18"
  val PactV                 = "2.3.16"

  lazy val pactTestFrameworkDependencies = Seq(
    "com.itv" %% "scalapact-circe-0-13"  % PactV % "test",
    "com.itv" %% "scalapact-http4s-0-21" % PactV % "test",
    "com.itv" %% "scalapact-scalatest"   % PactV % "test"
  )

  lazy val ndlaNetwork        = "ndla" %% "network"        % "0.47"
  lazy val ndlaMapping        = "ndla" %% "mapping"        % "0.15"
  lazy val ndlaValidation     = "ndla" %% "validation"     % "0.52"
  lazy val ndlaScalatestsuite = "ndla" %% "scalatestsuite" % "0.3" % "test"

  lazy val scalatra = Seq(
    "org.scalatra" %% "scalatra"           % ScalatraV,
    "org.scalatra" %% "scalatra-json"      % ScalatraV,
    "org.scalatra" %% "scalatra-swagger"   % ScalatraV,
    "org.scalatra" %% "scalatra-scalatest" % ScalatraV % "test"
  )

  lazy val elastic4sCore     = "com.sksamuel.elastic4s" %% "elastic4s-core"     % Elastic4sV
  lazy val elastic4sHttp     = "com.sksamuel.elastic4s" %% "elastic4s-http"     % Elastic4sV
  lazy val elastic4sAWS      = "com.sksamuel.elastic4s" %% "elastic4s-aws"      % Elastic4sV
  lazy val elastic4sEmbedded = "com.sksamuel.elastic4s" %% "elastic4s-embedded" % Elastic4sV

  lazy val articleApiDependencies: Seq[ModuleID] = Seq(
    ndlaNetwork,
    ndlaMapping,
    ndlaValidation,
    ndlaScalatestsuite,
    elastic4sCore,
    elastic4sHttp,
    "joda-time"                  % "joda-time"                       % "2.10",
    "org.eclipse.jetty"          % "jetty-webapp"                    % JettyV % "container;compile",
    "org.eclipse.jetty"          % "jetty-plus"                      % JettyV % "container",
    "javax.servlet"              % "javax.servlet-api"               % "4.0.1" % "container;provided;test",
    "org.json4s"                 %% "json4s-native"                  % Json4SV,
    "com.typesafe.scala-logging" %% "scala-logging"                  % ScalaLoggingV,
    "org.apache.logging.log4j"   % "log4j-api"                       % Log4JV,
    "org.apache.logging.log4j"   % "log4j-core"                      % Log4JV,
    "org.apache.logging.log4j"   % "log4j-slf4j-impl"                % Log4JV,
    "org.scalikejdbc"            %% "scalikejdbc"                    % "4.0.0-RC2",
    "org.postgresql"             % "postgresql"                      % PostgresV,
    "com.zaxxer"                 % "HikariCP"                        % HikariConnectionPoolV,
    "com.amazonaws"              % "aws-java-sdk-s3"                 % AwsSdkV,
    "com.amazonaws"              % "aws-java-sdk-cloudwatch"         % AwsSdkV,
    "org.scalaj"                 %% "scalaj-http"                    % "2.4.2",
    "org.elasticsearch"          % "elasticsearch"                   % ElasticsearchV,
    "vc.inreach.aws"             % "aws-signing-request-interceptor" % "0.0.22",
    "org.scalatest"              %% "scalatest"                      % ScalaTestV % "test",
    "org.jsoup"                  % "jsoup"                           % "1.11.3",
    "net.bull.javamelody"        % "javamelody-core"                 % "1.74.0",
    "org.jrobin"                 % "jrobin"                          % "1.5.9", // This is needed for javamelody graphing
    "org.mockito"                %% "mockito-scala"                  % MockitoV % "test",
    "org.mockito"                %% "mockito-scala-scalatest"        % MockitoV % "test",
    "org.flywaydb"               % "flyway-core"                     % FlywayV,
    "io.lemonlabs"               %% "scala-uri"                      % "1.5.1"
  ) ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies

  lazy val draftApiDependencies: Seq[ModuleID] = Seq(
    ndlaNetwork,
    ndlaMapping,
    ndlaValidation,
    ndlaScalatestsuite,
    elastic4sCore,
    elastic4sHttp,
    elastic4sAWS,
    elastic4sEmbedded % "test",
    "joda-time"                  % "joda-time"                       % "2.10",
    "org.eclipse.jetty"          % "jetty-webapp"                    % JettyV % "container;compile",
    "org.eclipse.jetty"          % "jetty-plus"                      % JettyV % "container",
    "javax.servlet"              % "javax.servlet-api"               % "4.0.1" % "container;provided;test",
    "org.json4s"                 %% "json4s-native"                  % Json4SV,
    "com.typesafe.scala-logging" %% "scala-logging"                  % ScalaLoggingV,
    "org.apache.logging.log4j"   % "log4j-api"                       % Log4JV,
    "org.apache.logging.log4j"   % "log4j-core"                      % Log4JV,
    "org.apache.logging.log4j"   % "log4j-slf4j-impl"                % Log4JV,
    "vc.inreach.aws"             % "aws-signing-request-interceptor" % "0.0.22",
    "org.scalikejdbc"            %% "scalikejdbc"                    % "4.0.0-RC2",
    "com.zaxxer"                 % "HikariCP"                        % HikariConnectionPoolV,
    "org.postgresql"             % "postgresql"                      % PostgresV,
    "com.amazonaws"              % "aws-java-sdk-s3"                 % AwsSdkV,
    "org.scalaj"                 %% "scalaj-http"                    % "2.4.2",
    "org.elasticsearch"          % "elasticsearch"                   % ElasticsearchV,
    "org.scalatest"              %% "scalatest"                      % ScalaTestV % "test",
    "org.jsoup"                  % "jsoup"                           % "1.11.3",
    "net.bull.javamelody"        % "javamelody-core"                 % "1.74.0",
    "org.jrobin"                 % "jrobin"                          % "1.5.9",
    "com.amazonaws"              % "aws-java-sdk-cloudwatch"         % AwsSdkV,
    "org.mockito"                %% "mockito-scala"                  % MockitoV % "test",
    "org.mockito"                %% "mockito-scala-scalatest"        % MockitoV % "test",
    "org.flywaydb"               % "flyway-core"                     % FlywayV,
    "io.lemonlabs"               %% "scala-uri"                      % "1.5.1",
    "org.typelevel"              %% "cats-effect"                    % CatsEffectV,
    "org.slf4j"                  % "slf4j-api"                       % "1.7.30"
  ) ++ scalatra ++ vulnerabilityOverrides ++ pactTestFrameworkDependencies
    // Excluding slf4j-api (and specifically adding 1.7.30) because of conflict between 1.7.30 and 2.0.0-alpha1
    .map(_.exclude("org.slf4j", "slf4j-api"))

  // Sometimes we override transitive dependencies because of vulnerabilities, we put these here
  lazy val vulnerabilityOverrides = Seq(
    "com.fasterxml.jackson.core"   % "jackson-core"          % JacksonV,
    "com.fasterxml.jackson.core"   % "jackson-databind"      % JacksonV,
    "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonV,
    "com.google.guava"             % "guava"                 % "30.0-jre",
    "commons-codec"                % "commons-codec"         % "1.14",
    "org.apache.httpcomponents"    % "httpclient"            % "4.5.13",
    "org.yaml"                     % "snakeyaml"             % "1.26"
  )
}
