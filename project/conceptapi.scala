import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.*
import sbtassembly.AssemblyPlugin
import sbtdocker.DockerPlugin

object conceptapi extends Module {
  override val moduleName: String        = "concept-api"
  override val MainClass: Option[String] = Some("no.ndla.conceptapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      catsEffect,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.typelevel" %% "cats-core" % "2.13.0"
    ),
    melody,
    elastic4s,
    melody,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq("no.ndla.conceptapi.model.api._", "no.ndla.conceptapi.model.api.TSTypes._"),
    exports = Seq(
      "ConceptDTO",
      "ConceptSearchParamsDTO",
      "ConceptSearchResultDTO",
      "ConceptSummaryDTO",
      "DraftConceptSearchParamsDTO",
      "NewConceptDTO",
      "TagsSearchResultDTO",
      "UpdatedConceptDTO"
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
    ScalaTsiPlugin,
    AssemblyPlugin
  )
}
