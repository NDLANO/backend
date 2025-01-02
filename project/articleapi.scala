import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object articleapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.articleapi.Main")
  override val moduleName: String        = "article-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
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

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq(
      "no.ndla.articleapi.model.api._",
      "no.ndla.articleapi.model.api.TSTypes._",
      "no.ndla.common.model.domain.Availability"
    ),
    exports = Seq(
      "ArticleV2DTO",
      "ArticleSearchParamsDTO",
      "ArticleSummaryV2DTO",
      "Availability",
      "SearchResultV2DTO",
      "TagsSearchResultDTO",
      "ArticleDumpDTO",
      "ArticleIdsDTO"
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
