import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object audioapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.audioapi.Main")
  override val moduleName: String        = "audio-api"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
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
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq("no.ndla.audioapi.model.api._"),
    exports = Seq(
      "Audio",
      "AudioSummarySearchResult",
      "NewAudioMetaInformation",
      "NewSeries",
      "SearchParams",
      "Series",
      "SeriesSummary",
      "AudioSummary",
      "TagsSearchResult",
      "AudioMetaInformation",
      "UpdatedAudioMetaInformation",
      "SeriesSummarySearchResult",
      "SeriesSearchParams"
    )
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings() ++
    tsSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    ScalaTsiPlugin
  )
}
