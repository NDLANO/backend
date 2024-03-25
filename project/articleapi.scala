import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.{libraryDependencies, name}
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

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
      "com.amazonaws"  % "aws-java-sdk-s3"         % AwsSdkV,
      "com.amazonaws"  % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.scalatest" %% "scalatest"               % ScalaTestV % "test"
    ),
    flexmark,
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq(
      "no.ndla.articleapi.model.api._",
      "no.ndla.articleapi.model.api.TSTypes._",
      "no.ndla.common.model.domain.Availability"
    ),
    exports = Seq(
      "ArticleV2",
      "ArticleSearchParams",
      "ArticleSummaryV2",
      "Availability.type",
      "SearchResultV2",
      "TagsSearchResult",
      "ArticleDump",
      "ValidationError",
      "ArticleIds"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
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
