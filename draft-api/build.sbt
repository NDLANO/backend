import java.util.Properties

val Scalaversion = "2.13.3"
val Scalatraversion = "2.8.2"
val ScalaLoggingVersion = "3.9.2"
val ScalaTestVersion = "3.2.1"
val Log4JVersion = "2.17.0"
val Jettyversion = "9.4.35.v20201120"
val AwsSdkversion = "1.11.658"
val MockitoVersion = "1.14.8"
val Elastic4sVersion = "6.7.8"
val JacksonVersion = "2.12.1"
val CatsEffectVersion = "2.1.2"
val ElasticsearchVersion = "6.8.13"
val Json4SVersion = "4.0.3"
val FlywayVersion = "7.5.3"
val PostgresVersion = "42.2.18"
val HikariConnectionPoolVersion = "4.0.1"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

import com.itv.scalapact.plugin._
val pactVersion = "2.3.16"

val pactTestFramework = Seq(
  "com.itv" %% "scalapact-circe-0-13" % pactVersion % "test",
  "com.itv" %% "scalapact-http4s-0-21" % pactVersion % "test",
  "com.itv" %% "scalapact-scalatest" % pactVersion % "test"
)

// Sometimes we override transitive dependencies because of vulnerabilities, we put these here
val vulnerabilityOverrides = Seq(
  "com.google.guava" % "guava" % "30.0-jre",
  "commons-codec" % "commons-codec" % "1.14",
  "org.yaml" % "snakeyaml" % "1.26",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13",
  "com.fasterxml.jackson.core" % "jackson-core" % JacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % JacksonVersion,
  "com.fasterxml.jackson.module" %% "jackson-module-scala" % JacksonVersion
)

lazy val PactTest = config("pact") extend Test
lazy val draft_api = (project in file("."))
  .configs(PactTest)
  .settings(
    inConfig(PactTest)(Defaults.testTasks),
    // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
    // To run all tests use pact config ('sbt pact:test')
    Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
    PactTest / testOptions := Seq.empty,
  )
  .settings(
    name := "draft-api",
    organization := appProperties.value.getProperty("NDLAOrganization"),
    version := appProperties.value.getProperty("NDLAComponentVersion"),
    scalaVersion := Scalaversion,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= (pactTestFramework ++ Seq(
      "ndla" %% "language" % "1.0.0",
      "ndla" %% "mapping" % "0.15",
      "ndla" %% "network" % "0.47",
      "ndla" %% "validation" % "0.55",
      "ndla" %% "scalatestsuite" % "0.3" % "test",
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
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22",
      "org.scalikejdbc" %% "scalikejdbc" % "4.0.0-RC2",
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "org.postgresql" % "postgresql" % PostgresVersion,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-embedded" % Elastic4sVersion % "test",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.jsoup" % "jsoup" % "1.11.3",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkversion,
      "org.mockito" %% "mockito-scala" % MockitoVersion % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.typelevel" %% "cats-effect" % CatsEffectVersion,
      "org.slf4j" % "slf4j-api" % "1.7.30",
    ) ++ vulnerabilityOverrides)
    // Excluding slf4j-api (and specifically adding 1.7.30) because of conflict between 1.7.30 and 2.0.0-alpha1
      .map(_.exclude("org.slf4j", "slf4j-api"))
  )
  .enablePlugins(ScalaTsiPlugin)
  .settings(
    // The classes that you want to generate typescript interfaces for
    typescriptGenerationImports := Seq("no.ndla.draftapi.model.api._", "no.ndla.draftapi.model.api.TSTypes._"),
    typescriptExports := Seq(
      "Agreement",
      "Article",
      "NewArticle",
      "UpdatedAgreement",
      "UpdatedArticle",
      "UpdatedUserData",
      "UserData"
    ),
    typescriptOutputFile := baseDirectory.value / "typescript" / "index.ts",
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)
  .enablePlugins(ScalaPactPlugin)

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

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

docker / dockerfile := {
  val artifact = (assembly / assemblyOutputPath).value
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
