import Dependencies.versions.*
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object draftapi extends Module {
  override val moduleName: String        = "draft-api"
  override val MainClass: Option[String] = Some("no.ndla.draftapi.Main")
  lazy val dependencies: Seq[ModuleID]   = withLogging(
    Seq(
      scalaUri,
      enumeratum,
      sttp,
      catsEffect,
      jsoup,
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "org.scalatest"   %% "scalatest"   % ScalaTestV % "test"
    ),
    flexmark,
    awsS3,
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++ commonSettings ++ assemblySettings() ++ dockerSettings()

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin
  )
}
