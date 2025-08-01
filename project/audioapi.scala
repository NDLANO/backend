import Dependencies.versions.*
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object audioapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.audioapi.Main")
  override val moduleName: String        = "audio-api"
  lazy val dependencies: Seq[ModuleID]   = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      sttp,
      catsEffect,
      jsoup,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    flexmark,
    awsS3,
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides,
    jave
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings()

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin
  )
}
