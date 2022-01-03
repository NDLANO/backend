import Dependencies.common._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtassembly._
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.{
  typescriptExports,
  typescriptGenerationImports,
  typescriptOutputFile
}
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyKeys._
import sbtdocker.DockerKeys._
import sbtdocker._

trait Module {
  protected val MainClass: Option[String] = None
  lazy val commonSettings = Seq(
    run / mainClass := this.MainClass,
    Compile / mainClass := this.MainClass,
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

  def withLogging(libs: Seq[ModuleID]): Seq[ModuleID] = {
    // Many sub-dependencies might pull in slf4j-api, and since there might
    // be compatibility issues we exclude others and include our own when
    // we add logging.
    libs.map(_.exclude("org.slf4j", "slf4j-api")) ++ logging
  }

  def assemblySettings() = Seq(
    assembly / assemblyJarName := name.value + ".jar",
    assembly / mainClass := this.MainClass,
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

  lazy val PactTestConfig = config("PactTest") extend (Test)
  lazy val PactSettings: Seq[Def.Setting[_]] = inConfig(PactTestConfig)(Defaults.testTasks) ++ Seq(
    // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
    // To run all tests use pact config 'sbt PactTest/test' (or 'sbt article_api/PactTest/test' for a single subproject)
    Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
    Test / testOnly / testOptions := Seq(Tests.Argument("-l", "PactProviderTest")),
    PactTestConfig / testOptions := Seq.empty,
    PactTestConfig / testOnly / testOptions := Seq.empty
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

  def typescriptSettings(imports: Seq[String],
                         exports: Seq[String]): Seq[Def.Setting[_ >: Seq[String] with sbt.File <: Object]] = {
    Seq(
      typescriptGenerationImports := imports,
      typescriptExports := exports,
      typescriptOutputFile := baseDirectory.value / "typescript" / "index.ts"
    )

  }
}
