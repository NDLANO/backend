import java.util.Properties

val Scalaversion = "2.12.2"
val Scalatraversion = "2.5.1"
val ScalaLoggingVersion = "3.5.0"
val ScalaTestVersion = "3.0.1"
val Log4JVersion = "2.9.1"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.11.46"
val MockitoVersion = "1.10.19"
val Elastic4sVersion = "6.1.2"
val ElasticsearchVersion = "6.0.1"
val Json4SVersion = "3.5.3"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val draft_api = (project in file("."))
  .settings(
    name := "draft-api",
    organization := appProperties.value.getProperty("NDLAOrganization"),
    version := appProperties.value.getProperty("NDLAComponentVersion"),
    scalaVersion := Scalaversion,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.28",
      "ndla" %% "mapping" % "0.7",
      "ndla" %% "validation" % "0.16",
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.json4s" %% "json4s-native" % Json4SVersion,
      "org.scalatra" %% "scalatra-swagger" % Scalatraversion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.scalikejdbc" %% "scalikejdbc" % "2.5.0",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "mysql" % "mysql-connector-java" % "5.1.36",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "2.3.0",
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-core" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-http" % Elastic4sVersion,
      "com.sksamuel.elastic4s" %% "elastic4s-aws" % Elastic4sVersion,
      "org.elasticsearch" % "elasticsearch" % ElasticsearchVersion,
      "org.apache.lucene" % "lucene-test-framework" % "6.4.1" % "test",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.jsoup" % "jsoup" % "1.10.3",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % "4.0",
      "com.netaporter" %% "scala-uri" % "0.4.16"
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

Test / test := ((Test / test).dependsOn(Test / checkfmt)).value

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

// Don't run Integration tests in default run on Travis as there is no elasticsearch localhost:9200 there yet.
// NB this line will unfortunalty override runs on your local commandline so that
// sbt "test-only -- -n no.ndla.tag.IntegrationTest"
// will not run unless this line gets commented out or you remove the tag over the test class
// This should be solved better!
Test / testOptions += Tests.Argument("-l", "no.ndla.tag.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker := (docker dependsOn assembly).value

docker / dockerfile := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("openjdk:8-jre-alpine")

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
