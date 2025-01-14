import Dependencies.versions.*
import GithubWorkflowPlugin.autoImport.*
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.{
  typescriptExports,
  typescriptGenerationImports,
  typescriptOutputFile,
  typescriptTaggedUnionDiscriminator
}
import org.scalafmt.sbt.ScalafmtPlugin.autoImport.*
import org.typelevel.sbt.tpolecat.TpolecatPlugin.autoImport.*
import org.typelevel.scalacoptions.*
import sbt.*
import sbt.Keys.*
import sbtassembly.*
import sbtassembly.AssemblyKeys.*
import sbtdocker.*
import sbtdocker.DockerKeys.*

import scala.language.postfixOps

object Module {

  def setup(
      project: sbt.Project,
      module: Module,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty
  ): sbt.Project = {
    project
      .settings(module.settings*)
      .configs(module.configs*)
      .enablePlugins(module.plugins*)
      .disablePlugins(module.disablePlugins*)
      .dependsOn(deps*)
  }
}

trait Module {
  lazy val settings: Seq[Def.Setting[?]]                     = Seq.empty
  lazy val configs: Seq[sbt.librarymanagement.Configuration] = Seq.empty
  lazy val plugins: Seq[sbt.Plugins]                         = Seq.empty
  lazy val disablePlugins: Seq[sbt.AutoPlugin]               = Seq.empty
  val enableReleases: Boolean                                = true
  val moduleName: String

  protected val MainClass: Option[String] = None

  lazy val commonSettings: Seq[Def.Setting[?]] = Seq(
    name                := this.moduleName,
    run / mainClass     := this.MainClass,
    Compile / mainClass := this.MainClass,
    organization        := "ndla",
    version             := "0.0.1",
    scalaVersion        := ScalaV,
    javacOptions ++= Seq("-source", "21", "-target", "21"),
    ghGenerateEnable        := true,
    ghGenerateEnableRelease := this.enableReleases,
    javaOptions ++= reflectiveAccessOptions,
    tpolecatScalacOptions ++= scalacOptions,
    tpolecatExcludeOptions ++= excludeOptions,
    Compile / unmanagedResources += file("log4j2.yaml"),
    Test / unmanagedResources += file("log4j2-test.yaml"),
    Test / tpolecatExcludeOptions ++= testExcludeOptions,
    Test / parallelExecution := false,
    resolvers ++= scala.util.Properties
      .envOrNone("NDLA_RELEASES")
      .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
      .toSeq
  ) ++ fmtSettings

  val excludeOptions: Set[ScalacOption] = {
    // NOTE: Intellij has no good way of separating run and test scala configurations from sbt.
    //       This is a workaround to make sure that fatal warnings does not break intellij
    //       while keeping fatal warnings on CI.
    val isCI: Boolean                        = sys.env.getOrElse("CI", "false").toBoolean
    val CICompilerOptions: Set[ScalacOption] = if (isCI) Set.empty else Set(ScalacOptions.fatalWarnings)

    CICompilerOptions ++ Set(
      ScalacOption("-Wmacros:after", _ => true),
      ScalacOption("-Wmacros:none", _ => true),
      ScalacOption("-Wmacros:before", _ => true)
    )
  }

  val testExcludeOptions: Set[ScalacOption] = {
    Set(
      ScalacOptions.warnNonUnitStatement,
      ScalacOptions.warnValueDiscard,
      ScalacOptions.warnDeadCode
    )
  }

  val scalacOptions: Set[ScalacOption] = {
    // TODO: We need this because of some circe encoder/decoder deriving.
    //       May not be needed in the future, lets explore it when scala 3 is in place.
    val maxInlines = ScalacOption("-Xmax-inlines", List("50"), _.major >= 3)

    // TODO: Scala 3 does not have this option yet. See: https://github.com/scala/scala3/issues/18782
    // NOTE: scala-tsi leaves some unused imports and such in src_managed, lets not care about linting scala-tsi code.
    val silentSrcManaged: ScalacOption = ScalacOption("-Wconf:src=src_managed/.*:silent", _.major == 2)

    // TODO: Remove this when on scala 3
    val xsource: ScalacOption = ScalacOption("-Xsource:3", _.major == 2)

    Set(
      maxInlines,
      silentSrcManaged,
      xsource
    )
  }

  def withLogging(libs: Seq[ModuleID], extraLibs: Seq[ModuleID]*): Seq[ModuleID] = {
    // Many sub-dependencies might pull in slf4j-api, and since there might
    // be compatibility issues we exclude others and include our own when
    // we add logging.
    libs.map(_.exclude("org.slf4j", "slf4j-api")) ++ logging ++ extraLibs.flatten
  }

  def assemblySettings() = Seq(
    assembly / assemblyJarName := name.value + ".jar",
    assembly / mainClass       := this.MainClass,
    assembly / assemblyMergeStrategy := {
      case "module-info.class"                             => MergeStrategy.discard
      case "META-INF/FastDoubleParser-NOTICE"              => MergeStrategy.first
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

  def dockerSettings(): Seq[Def.Setting[?]] = {
    Seq(
      docker := (docker dependsOn assembly).value,
      docker / dockerfile := {
        val artifact           = (assembly / assemblyOutputPath).value
        val artifactTargetPath = s"/app/${artifact.name}"

        val entry =
          s"java $$JAVA_OPTS ${reflectiveAccessOptions.mkString(" ")} -jar $artifactTargetPath"

        new Dockerfile {
          from("eclipse-temurin:21-jdk")
          env("LOG_APPENDER", "Docker")
          add(artifact, artifactTargetPath)
          entryPointRaw(entry)
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

  val fmtSettings: Seq[Def.Setting[Task[Unit]]] = Seq(checkfmtSetting, fmtSetting)

  protected def typescriptSettings(imports: Seq[String], exports: Seq[String]) = {
    Seq(
      typescriptGenerationImports        := imports,
      typescriptExports                  := exports,
      typescriptOutputFile               := file("./typescript/types-backend") / s"${this.moduleName}.ts",
      typescriptTaggedUnionDiscriminator := Some("typename")
    )
  }
}
