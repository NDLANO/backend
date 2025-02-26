import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.*
import sbtdocker.DockerPlugin

object learningpathapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.learningpathapi.Main")
  override val moduleName: String        = "learningpath-api"

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
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq(
      "no.ndla.learningpathapi.model.api._",
      "no.ndla.common.model.api._"
    ),
    exports = Seq(
      "AuthorDTO",
      "LearningPathStatusDTO",
      "LearningPathSummaryV2DTO",
      "LearningPathTagsSummaryDTO",
      "LearningPathV2DTO",
      "LearningStepContainerSummaryDTO",
      "LearningStepSeqNoDTO",
      "LearningStepStatusDTO",
      "LearningStepSummaryV2DTO",
      "LearningStepV2DTO",
      "LicenseDTO",
      "SearchResultV2DTO",
      "config.ConfigMetaRestrictedDTO",
      "config.ConfigMetaDTO"
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
