import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.{
  typescriptExports,
  typescriptGenerationImports,
  typescriptOutputFile
}
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._
import sbtdocker.DockerKeys._
import sbtdocker._
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport._
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._

object Dependencies {

  object common {
    lazy val commonSettings = Seq(
      organization := "ndla",
      version := "0.0.1",
      scalaVersion := ScalaV,
      javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
      scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
      Test / parallelExecution := false,
      resolvers ++= scala.util.Properties
        .envOrNone("NDLA_RELEASES")
        .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
        .toSeq
    )

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

    lazy val pactTestFrameworkDependencies = Seq(
      "com.itv" %% "scalapact-circe-0-13" % PactV % "test",
      "com.itv" %% "scalapact-http4s-0-21" % PactV % "test",
      "com.itv" %% "scalapact-scalatest" % PactV % "test"
    )

    lazy val ndlaNetwork = "ndla" %% "network" % "0.47"
    lazy val ndlaMapping = "ndla" %% "mapping" % "0.15"
    lazy val ndlaValidation = "ndla" %% "validation" % "0.53"
    lazy val ndlaLanguage = "ndla" %% "language" % "1.0.0"
    lazy val ndlaScalatestsuite = "ndla" %% "scalatestsuite" % "0.3" % "test"

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

    lazy val logging = Seq(
      "org.apache.logging.log4j" % "log4j-api" % Log4JV,
      "org.apache.logging.log4j" % "log4j-core" % Log4JV,
      "org.apache.logging.log4j" % "log4j-slf4j-impl" % Log4JV,
      "org.slf4j" % "slf4j-api" % "1.7.32",
      "com.typesafe.scala-logging" %% "scala-logging" % ScalaLoggingV
    )

    def withLogging(libs: Seq[ModuleID]): Seq[ModuleID] = {
      libs.map(_.exclude("org.slf4j", "slf4j-api")) ++ logging
    }

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

    lazy val PactTestConfig = config("PactTest") extend (Test)
    lazy val PactSettings: Seq[Def.Setting[_]] = inConfig(PactTestConfig)(Defaults.testTasks) ++ Seq(
      // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
      // To run all tests use pact config 'sbt PactTest/test' (or 'sbt article_api/PactTest/test' for a single subproject)
      Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
      Test / testOnly / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
      PactTestConfig / testOptions := Seq.empty,
      PactTestConfig / testOnly / testOptions := Seq.empty
    )

    def assemblySettings(assemblyMainClass: String) = Seq(
      assembly / assemblyJarName := name.value + ".jar",
      assembly / mainClass := Some(assemblyMainClass),
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
    )

    def dockerSettings(extraJavaOpts: String*): Seq[Def.Setting[_]] = {
      Seq(
        docker := (docker dependsOn assembly).value,
        docker / dockerfile := {
          val artifact = (assembly / assemblyOutputPath).value
          val artifactTargetPath = s"/app/${artifact.name}"

          val entry = Seq(
            "java",
            "-Dorg.scalatra.environment=production",
          ) ++
            extraJavaOpts ++
            Seq("-jar", artifactTargetPath)

          new Dockerfile {
            from("adoptopenjdk/openjdk11:alpine-slim")
            run("apk", "--no-cache", "add", "ttf-dejavu")
            add(artifact, artifactTargetPath)
            entryPoint(entry: _*)
          }
        },
        docker / imageNames := Seq(
          ImageName(namespace = Some(organization.value),
                    repository = name.value,
                    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
        )
      )
    }

    val checkfmt = taskKey[Boolean]("Check for code style errors")
    val fmt = taskKey[Unit]("Automatically apply code style fixes")

    val checkfmtSetting = {
      checkfmt := {
        val noErrorsInMainFiles = (Compile / scalafmtCheck).value
        val noErrorsInTestFiles = (Test / scalafmtCheck).value
        val noErrorsInSbtConfigFiles = (Compile / scalafmtSbtCheck).value

        noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInSbtConfigFiles
      }
    }

    val fmtSetting = {
      fmt := {
        (Compile / scalafmt).value
        (Test / scalafmt).value
        (Compile / scalafmtSbt).value
      }
    }

    val fmtSettings = Seq(
      checkfmtSetting,
      fmtSetting,
      Test / test := (Test / test).dependsOn(Test / checkfmt).value
    )

  }

  def typescriptSettings(name: String,
                         imports: Seq[String],
                         exports: Seq[String]): Seq[Def.Setting[_ >: Seq[String] with sbt.File <: Object]] = {
    Seq(
      typescriptGenerationImports := imports,
      typescriptExports := exports,
      typescriptOutputFile := baseDirectory.value / "typescript" / "index.ts"
    )
  }
}
