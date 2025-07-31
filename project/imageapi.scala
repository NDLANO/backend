import Dependencies.versions.*
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object imageapi extends Module {
  override val moduleName: String        = "image-api"
  override val MainClass: Option[String] = Some("no.ndla.imageapi.Main")
  lazy val dependencies: Seq[ModuleID]   = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      sttp,
      jsoup,
      "org.scalatest" %% "scalatest"    % ScalaTestV % "test",
      "org.imgscalr"   % "imgscalr-lib" % "4.2",
      // These are not strictly needed, for most cases, but offers better handling of loading images with encoding issues
      "com.twelvemonkeys.imageio" % "imageio-core" % "3.12.0",
      "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.12.0",
      "commons-io"                % "commons-io"   % "2.19.0"
    ),
    awsS3,
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    dockerSettings() ++
    assemblySettings()

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin
  )
}
