import Dependencies.versions.*
import sbt.*
import sbt.Keys.*
import sbtdocker.DockerPlugin

object frontpageapi extends Module {
  override val moduleName: String        = "frontpage-api"
  override val MainClass: Option[String] = Some("no.ndla.frontpageapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumCirce,
      catsEffect,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    database,
    vulnerabilityOverrides,
    tapirHttp4sCirce
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
