import java.io.FileInputStream
import java.util.Properties

import sbtdocker.Instructions

val Scalaversion = "2.11.8"
val Scalatraversion = "2.3.1"
val SwaggerUIVersion = "2.0.24"
val ScalaLoggingVersion = "3.1.0"
val Log4JVersion = "2.6"
val Jettyversion = "9.2.10.v20150310"
val AwsSdkversion = "1.10.26"
val ScalaTestVersion = "2.2.4"
val MockitoVersion = "1.10.19"
val SlickVersion = "3.0.0"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

lazy val commonSettings = Seq(
  organization := appProperties.value.getProperty("NDLAOrganization"),
  version := appProperties.value.getProperty("NDLAComponentVersion"),
  scalaVersion := Scalaversion
)

lazy val content_api = (project in file(".")).
  settings(commonSettings: _*).
  settings(
    name := "content-api",
    javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
    scalacOptions := Seq("-target:jvm-1.7"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.4",
      "joda-time" % "joda-time" % "2.8.2",
      "org.scalatra" %% "scalatra" % Scalatraversion,
      "org.eclipse.jetty" % "jetty-webapp" % Jettyversion % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % Jettyversion % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.scalatra" %% "scalatra-json" % Scalatraversion,
      "org.scalatra" %% "scalatra-scalatest" % Scalatraversion % "test",
      "org.json4s"   %% "json4s-native" % "3.3.0",
      "org.scalatra" %% "scalatra-swagger"  % Scalatraversion,
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.webjars" % "swagger-ui" % SwaggerUIVersion,
      "org.scalikejdbc" %% "scalikejdbc" % "2.2.8",
      "org.postgresql" % "postgresql" % "9.4-1201-jdbc4",
      "mysql" % "mysql-connector-java" % "5.1.36",
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkversion,
      "org.scalaj" %% "scalaj-http" % "1.1.5",
      "com.sksamuel.elastic4s" %% "elastic4s-core" % "2.3.0",
      "com.sksamuel.elastic4s" %% "elastic4s-testkit" % "2.3.0" % "test",
      "org.scalatest" % "scalatest_2.11" % ScalaTestVersion % "test",
      "org.jsoup" % "jsoup" % "1.7.3",
      "org.mockito" % "mockito-all" % MockitoVersion % "test",
      "org.flywaydb" % "flyway-core" % "4.0",
      "com.netaporter" %% "scala-uri" % "0.4.12"
    )
  ).enablePlugins(DockerPlugin).enablePlugins(GitVersioning).enablePlugins(JettyPlugin)

unmanagedResourceDirectories in Compile <+= (baseDirectory) {_ / "src/main/webapp"}

assemblyJarName in assembly := "content-api.jar"
mainClass in assembly := Some("no.ndla.contentapi.JettyLauncher")
assemblyMergeStrategy in assembly := {
  case "mime.types" => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class")  => MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class")  => MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class")  => MergeStrategy.first
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

// Don't run Integration tests in default run
testOptions in Test += Tests.Argument("-l", "no.ndla.IntegrationTest")

// Make the docker task depend on the assembly task, which generates a fat JAR file
docker <<= (docker dependsOn assembly)

dockerfile in docker := {
  val artifact = (assemblyOutputPath in assembly).value
  val artifactTargetPath = s"/app/${artifact.name}"
  new Dockerfile {
    from("java")

    add(artifact, artifactTargetPath)
    entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
  }
}

val gitHeadCommitSha = settingKey[String]("current git commit SHA")
gitHeadCommitSha in ThisBuild := Process("git log --pretty=format:%h -n 1").lines.head

imageNames in docker := Seq(
  ImageName(
    namespace = Some(organization.value),
    repository = name.value,
    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
)

parallelExecution in Test := false
