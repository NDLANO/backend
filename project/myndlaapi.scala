import Dependencies.versions.*
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object myndlaapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.myndlaapi.Main")
  override val moduleName: String        = "myndla-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      sttp,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    tapirHttp4sCirce,
    database,
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
