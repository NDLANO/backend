import java.util.Properties

val Scalaversion = "2.12.7"
val Scalatraversion = "2.6.3"
val ScalaLoggingVersion = "3.9.0"
val ScalaTestVersion = "3.0.5"
val Log4JVersion = "2.11.1"
val Jettyversion = "9.4.12.v20180830"
val AwsSdkversion = "1.11.434"
val MockitoVersion = "2.23.0"
val Elastic4sVersion = "6.3.7"
val JacksonVersion = "2.9.8"
val ElasticsearchVersion = "6.3.2"
val Json4SVersion = "3.5.4"
val CatsEffectVersion = "1.0.0"
val FlywayVersion = "5.2.0"
val PostgresVersion = "42.2.5"
val HikariConnectionPoolVersion = "3.2.0"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

import com.itv.scalapact.plugin._
val pactVersion = "2.3.4"

val pactTestFramework = Seq(
  "com.itv" %% "scalapact-argonaut-6-2" % pactVersion % "test",
  "com.itv" %% "scalapact-http4s-0-16a" % pactVersion % "test",
  "com.itv" %% "scalapact-scalatest" % pactVersion % "test"
)

lazy val PactTest = config("pact") extend Test
lazy val draft_api = (project in file("."))
  .configs(PactTest)
  .settings(
    inConfig(PactTest)(Defaults.testTasks),
    // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
    // To run all tests use pact config ('sbt pact:test')
    Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
    PactTest / testOptions := Seq.empty
  )
  .settings(
    name := "draft-api",
    organization := appProperties.value.getProperty("NDLAOrganization"),
    version := appProperties.value.getProperty("NDLAComponentVersion"),
    scalaVersion := Scalaversion,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= pactTestFramework ++ Seq(
      "ndla" %% "network" % "0.38",
      "ndla" %% "mapping" % "0.11",
      "ndla" %% "validation" % "0.31",
      "joda-time" % "joda-time" % "2.10",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.json4s" %% "json4s-native" % Json4SVersion,
      "org.scalatra" %% "scalatra-swagger" % Scalatraversion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.scalikejdbc" %% "scalikejdbc" % "3.3.1",
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "org.postgresql" % "postgresql" % PostgresVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "2.4.1",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-embedded" % Elastic4sVersion % "test",
      "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion, // Overriding jackson-databind used in dependencies because of https://app.snyk.io/vuln/SNYK-JAVA-COMFASTERXMLJACKSONCORE-72884
      "org.apache.lucene" % "lucene-queryparser" % "7.1.0", // Overriding lucene-queryparser used in elasticsearch because of https://snyk.io/vuln/SNYK-JAVA-ORGAPACHELUCENE-31569
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "log4j" % "log4j" % "1.2.16",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkversion,
      "org.mockito" % "mockito-core" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "io.lemonlabs" %% "scala-uri" % "1.3.1",
      "org.typelevel" %% "cats-effect" % CatsEffectVersion
    ),
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)

val checkfmt = taskKey[Boolean]("Check for code style errors")
checkfmt := {
  val noErrorsInMainFiles = (Compile / scalafmtCheck).value
  val noErrorsInTestFiles = (Test / scalafmtCheck).value
  val noErrorsInSbtConfigFiles = (Compile / scalafmtSbtCheck).value

  noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInSbtConfigFiles
}

Test / test := (Test / test).dependsOn(Test / checkfmt).value

val fmt = taskKey[Unit]("Automatically apply code style fixes")
fmt := {
  (Compile / scalafmt).value
  (Test / scalafmt).value
  (Compile / scalafmtSbt).value
}

assembly / assemblyJarName := "draft-api.jar"
assembly / mainClass := Some("no.ndla.draftapi.JettyLauncher")
assembly / assemblyMergeStrategy := {
  case "mime.types"                                                  => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")          => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")        => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
  case x =>
    val oldStrategy = (assembly / assemblyMergeStrategy).value
    oldStrategy(x)
}

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

docker / dockerfile := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("adoptopenjdk/openjdk11:alpine-slim")
    run("apk", "--no-cache", "add", "ttf-dejavu")
    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
  }
}

docker / imageNames := Seq(
  ImageName(namespace = Some(organization.value),
            repository = name.value,
            tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

Test / parallelExecution := false

resolvers ++= scala.util.Properties
  .envOrNone("NDLA_RELEASES")
  .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
  .toSeq
