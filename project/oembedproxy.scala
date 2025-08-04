import Dependencies.versions.*
import sbt.*
import sbt.Keys.*
import sbtdocker.DockerPlugin

object oembedproxy extends Module {
  override val moduleName: String        = "oembed-proxy"
  override val MainClass: Option[String] = Some("no.ndla.oembedproxy.Main")
  lazy val dependencies: Seq[ModuleID]   = withLogging(
    Seq(
      scalaUri,
      sttp,
      jsoup,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings()

  override lazy val plugins: Seq[Plugins] = Seq(
    DockerPlugin
  )
}
