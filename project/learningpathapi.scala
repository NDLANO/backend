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
      "com.amazonaws"  % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.scalatest" %% "scalatest"               % ScalaTestV % "test"
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
      "no.ndla.common.model.api._",
      "no.ndla.common.model.api.config._"
    ),
    exports = Seq(
      "Author",
      "LearningPathStatus",
      "LearningPathSummaryV2",
      "LearningPathTagsSummary",
      "LearningPathV2",
      "LearningStepContainerSummary",
      "LearningStepSeqNo",
      "LearningStepStatus",
      "LearningStepSummaryV2",
      "LearningStepV2",
      "License",
      "SearchResultV2",
      "ConfigMetaRestricted",
      "config.ConfigMeta"
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
