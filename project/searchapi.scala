import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.*
import sbtdocker.DockerPlugin

object searchapi extends Module {
  override val moduleName: String        = "search-api"
  override val MainClass: Option[String] = Some("no.ndla.searchapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
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

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq(
      "no.ndla.searchapi.model.api._",
      "no.ndla.searchapi.controller.parameters._"
    ),
    exports = Seq(
      "ApiTaxonomyContext",
      "ArticleResult",
      "AudioResult",
      "GroupSearchResult",
      "ImageResult",
      "LearningpathResult",
      "MultiSearchResult",
      "ArticleResults",
      "AudioResults",
      "ImageResults",
      "LearningpathResults",
      "SearchError",
      "SearchParams",
      "DraftSearchParams",
      "SubjectAggregations",
      "SubjectAggsInput",
      "GrepSearchInput"
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
