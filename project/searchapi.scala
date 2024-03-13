import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object searchapi extends Module {
  override val moduleName: String        = "search-api"
  override val MainClass: Option[String] = Some("no.ndla.searchapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      jsoup,
      "com.amazonaws"  % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.scalatest" %% "scalatest"               % ScalaTestV % "test"
    ),
    melody,
    elastic4s,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
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
      "ValidationError",
      "DraftSearchParams"
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
