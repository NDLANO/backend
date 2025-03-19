import Dependencies.versions.*
import sbt.*
import sbt.Keys.*
import sbtdocker.DockerPlugin

object learningpathapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.learningpathapi.Main")
  override val moduleName: String        = "learningpath-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      sttp,
      catsEffect,
      jsoup,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
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
    assemblySettings() ++
    dockerSettings()

  override lazy val plugins: Seq[sbt.Plugins] = Seq(DockerPlugin)
}
