import Dependencies.versions._
import sbt.Keys._
import sbt._
import au.com.onegeek.sbtdotenv.SbtDotenv.parseFile
import sbtassembly._
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.{
  typescriptExports,
  typescriptGenerationImports,
  typescriptOutputFile
}
import com.typesafe.sbt.SbtGit.git
import org.scalafmt.sbt.ScalafmtPlugin.autoImport._
import sbtassembly.AssemblyKeys._
import sbtdocker.DockerKeys._
import sbtdocker._

import scala.language.postfixOps

object Module {

  def setup(
      project: sbt.Project,
      module: Module,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty
  ): sbt.Project = {
    project
      .settings(module.settings: _*)
      .configs(module.configs: _*)
      .enablePlugins(module.plugins: _*)
      .disablePlugins(module.disablePlugins: _*)
      .dependsOn(deps: _*)
  }
}

trait Module {
  val isScala3Yet: Boolean = false
  val sVersion: String     = if (isScala3Yet) Scala3V else ScalaV

  lazy val settings: Seq[Def.Setting[_]]                     = Seq.empty
  lazy val configs: Seq[sbt.librarymanagement.Configuration] = Seq.empty
  lazy val plugins: Seq[sbt.Plugins]                         = Seq.empty
  lazy val disablePlugins: Seq[sbt.AutoPlugin]               = Seq.empty

  protected val MainClass: Option[String] = None

  // NOTE: Intellij has no good way of separating run and test scala configurations from sbt.
  //       This is a workaround to stop having to customize this locally,
  //       while still keeping fatal warnings active on CI.
  val isCI: Boolean          = sys.env.getOrElse("CI", "false").toBoolean
  val CIOptions: Seq[String] = if (isCI) Seq("-Xfatal-warnings") else Seq.empty

  lazy val commonSettings: Seq[Def.Setting[_]] = Seq(
    run / mainClass     := this.MainClass,
    Compile / mainClass := this.MainClass,
    organization        := "ndla",
    version             := "0.0.1",
    scalaVersion        := sVersion,
    javacOptions ++= Seq("-source", "17", "-target", "17"),
    scalacOptions := Seq(
      "-unchecked",
      // "-deprecation",
      "-feature"
    ),
    javaOptions ++= reflectiveAccessOptions,
    scalacOptions ++= CIOptions,
    Test / parallelExecution := false,
    resolvers ++= scala.util.Properties
      .envOrNone("NDLA_RELEASES")
      .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
      .toSeq
  ) ++ loadEnvFile() ++ fmtSettings

  private def loadEnvFile(): Seq[Def.Setting[_]] = {
    if (sys.env.get("DISABLE_SUB_DOTENV").contains("true")) Seq.empty
    else
      Seq(
        fork := true,
        envVars ++= {
          parseFile(baseDirectory.value / ".env").getOrElse(Map.empty)
        }
      )
  }

  def withLogging(libs: Seq[ModuleID]): Seq[ModuleID] = {
    // Many sub-dependencies might pull in slf4j-api, and since there might
    // be compatibility issues we exclude others and include our own when
    // we add logging.
    libs.map(_.exclude("org.slf4j", "slf4j-api")) ++ logging
  }

  def assemblySettings() = Seq(
    assembly / assemblyJarName := name.value + ".jar",
    assembly / mainClass       := this.MainClass,
    assembly / assemblyMergeStrategy := {
      case "module-info.class"                             => MergeStrategy.discard
      case x if x.endsWith("/module-info.class")           => MergeStrategy.discard
      case x if x.endsWith("io.netty.versions.properties") => MergeStrategy.discard
      case "mime.types"                                    => MergeStrategy.filterDistinctLines
      case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
    }
  )

  // Since scalatra uses reflection to generate swagger-doc
  // We need to open some types to reflective access
  // This should match `.jvmopts` file
  lazy val reflectiveAccessOptions: Seq[String] = Seq(
    "--add-opens=java.base/java.lang=ALL-UNNAMED",
    "--add-opens=java.base/java.net=ALL-UNNAMED",
    "--add-opens=java.base/java.security=ALL-UNNAMED",
    "--add-opens=java.base/java.time=ALL-UNNAMED",
    "--add-opens=java.base/java.util=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt.event=ALL-UNNAMED",
    "--add-opens=java.desktop/java.awt=ALL-UNNAMED"
  )

  def dockerSettings(extraJavaOpts: String*): Seq[Def.Setting[_]] = {
    Seq(
      docker := (docker dependsOn assembly).value,
      docker / dockerfile := {
        val artifact           = (assembly / assemblyOutputPath).value
        val artifactTargetPath = s"/app/${artifact.name}"

        val entry = Seq(
          "java",
          "-Dorg.scalatra.environment=production"
        ) ++
          reflectiveAccessOptions ++
          extraJavaOpts ++
          Seq("-jar", artifactTargetPath)

        new Dockerfile {
          from("eclipse-temurin:17-jdk")
          add(artifact, artifactTargetPath)
          entryPoint(entry: _*)
        }
      },
      docker / imageNames := Seq(
        ImageName(
          namespace = Some(organization.value),
          repository = name.value,
          tag = Some(System.getProperty("docker.tag", "SNAPSHOT"))
        )
      )
    )
  }

  val checkfmt = taskKey[Unit]("Check for code style errors")
  val fmt      = taskKey[Unit]("Automatically apply code style fixes")

  val checkfmtSetting = {
    checkfmt := {
      (Compile / scalafmtCheck).value
      (Test / scalafmtCheck).value
      (Compile / scalafmtSbtCheck).value
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
    fmtSetting
  )

  protected def typescriptSettings(imports: Seq[String], exports: Seq[String]) = {
    Seq(
      typescriptGenerationImports := imports,
      typescriptExports           := exports,
      typescriptOutputFile        := baseDirectory.value / "typescript" / "index.ts"
    )
  }
}
