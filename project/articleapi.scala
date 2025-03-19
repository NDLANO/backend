import Dependencies.versions.*
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object articleapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.articleapi.Main")
  override val moduleName: String        = "article-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      sttp,
      jsoup,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    flexmark,
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

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin
  )
}
