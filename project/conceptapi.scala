import Dependencies.versions.*
import sbt.*
import sbt.Keys.*
import sbtassembly.AssemblyPlugin
import sbtdocker.DockerPlugin

object conceptapi extends Module {
  override val moduleName: String        = "concept-api"
  override val MainClass: Option[String] = Some("no.ndla.conceptapi.Main")
  lazy val dependencies: Seq[ModuleID]   = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      catsEffect,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.typelevel" %% "cats-core" % "2.13.0"
    ),
    melody,
    elastic4s,
    melody,
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

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    AssemblyPlugin
  )
}
