import sbt.Keys._
import sbt._

object Dependencies {

  object versions {
    val ScalaV = "2.13.2"
    val ScalatraV = "2.8.2"
    val HikariConnectionPoolV = "4.0.1"
    val ScalaLoggingV = "3.9.4"
    val ScalaTestV = "3.2.1"
    val Log4JV = "2.17.1"
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

    lazy val scalaUri = "io.lemonlabs" %% "scala-uri" % "3.5.0"
    lazy val jodaTime = "joda-time" % "joda-time" % "2.10"

    lazy val pactTestFrameworkDependencies = Seq(
      "com.itv" %% "scalapact-circe-0-13" % PactV % "test",
      "com.itv" %% "scalapact-http4s-0-21" % PactV % "test",
      "com.itv" %% "scalapact-scalatest" % PactV % "test"
    )

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
    lazy val elasticsearch = "org.elasticsearch" % "elasticsearch" % ElasticsearchV

    lazy val logging = Seq(
      "org.apache.logging.log4j" % "log4j-api" % Log4JV,
      "org.apache.logging.log4j" % "log4j-core" % Log4JV,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JV,
      "org.slf4j" % "slf4j-api" % "1.7.32",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingV
    )

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
  }
}
