import java.util.Properties

val Scalaversion = "2.13.3"
val Log4JVersion = "2.17.0"
val ScalaTestVersion = "3.2.1"
val MockitoVersion = "1.14.8"
val Http4sVersion = "0.21.21"
val JacksonVersion = "2.12.1"
val RhoVersion = "0.21.0"
val CirceVersion = "0.13.0"
val ScalikeJDBCVersion = "4.0.0-RC2"
val HikariConnectionPoolVersion = "3.4.5"
val PostgresVersion = "42.2.14"
val FlywayVersion = "7.1.1"

val appProperties = settingKey[Properties]("The application properties")

appProperties := {
  val prop = new Properties()
  IO.load(prop, new File("build.properties"))
  prop
}

// Sometimes we override transitive dependencies because of vulnerabilities, we put these here
val vulnerabilityOverrides = Seq(
  "org.yaml" % "snakeyaml" % "1.26",
  "com.google.guava" % "guava" % "30.0-jre",
  "org.apache.httpcomponents" % "httpclient" % "4.5.13"
)

lazy val frontpage_api = (project in file("."))
  .settings(
    name := "frontpage-api",
    organization := appProperties.value.getProperty("NDLAOrganization"),
    version := appProperties.value.getProperty("NDLAComponentVersion"),
    scalaVersion := Scalaversion,
    javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
    scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
    libraryDependencies ++= Seq(
      "ndla" %% "network" % "0.47",
      "ndla" %% "mapping" % "0.15",
      "ndla" %% "scalatestsuite" % "0.3" % "test",
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.circe" %% "circe-generic-extras" % CirceVersion,
      "io.circe" %% "circe-literal" % CirceVersion,
      "io.circe" %% "circe-parser" % CirceVersion,
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCVersion,
      "org.postgresql" % "postgresql" % PostgresVersion,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolVersion,
      "org.http4s" %% "rho-swagger" % RhoVersion,
      "org.http4s" %% "http4s-server" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "org.http4s" %% "http4s-blaze-server" % Http4sVersion,
      "org.apache.logging.log4j" % "log4j-api" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-core" % Log4JVersion,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JVersion,
      "org.flywaydb" % "flyway-core" % FlywayVersion,
      "org.mockito" %% "mockito-scala" % MockitoVersion % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoVersion % "test",
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "javax.servlet" % "javax.servlet-api" % "4.0.1"
    ) ++ vulnerabilityOverrides
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(ScalaTsiPlugin)
  .settings(
    typescriptGenerationImports := Seq("no.ndla.frontpageapi.model.api._"),
    typescriptExports := Seq(
      "FrontPageData",
      "FilmFrontPageData",
      "NewOrUpdatedFilmFrontPageData",
      "SubjectPageData",
      "NewSubjectFrontPageData",
      "UpdatedSubjectFrontPageData",
      "Error"
    ),
    typescriptOutputFile := baseDirectory.value / "typescript" / "index.ts"
  )

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

assembly / assemblyJarName := "frontpage-api.jar"
assembly / mainClass := Some("no.ndla.frontpageapi.Main")
assembly / assemblyMergeStrategy := {
  case "module-info.class"                   => MergeStrategy.discard
  case x if x.endsWith("/module-info.class") => MergeStrategy.discard
  case "mime.types"                          => MergeStrategy.filterDistinctLines
  case PathList("org", "joda", "convert", "ToString.class") =>
    MergeStrategy.first
  case PathList("org", "joda", "convert", "FromString.class") =>
    MergeStrategy.first
  case PathList("org", "joda", "time", "base", "BaseDateTime.class") =>
    MergeStrategy.first
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
