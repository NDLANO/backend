import Dependencies.versions.*
import sbt.*
import sbt.Keys.*
import sbtdocker.DockerPlugin

object searchapi extends Module {
  override val moduleName: String        = "search-api"
  override val MainClass: Option[String] = Some("no.ndla.searchapi.Main")
  lazy val dependencies: Seq[ModuleID]   = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      jsoup,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    melody,
    elastic4s,
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
