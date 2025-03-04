import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object myndlaapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.myndlaapi.Main")
  override val moduleName: String        = "myndla-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      sttp,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    tapirHttp4sCirce,
    database,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq(
      "no.ndla.common.model.api._",
      "no.ndla.common.model.api.config._",
      "no.ndla.common.model.domain.config._",
      "no.ndla.myndlaapi.model.api._"
    ),
    exports = Seq(
      "ConfigMetaRestrictedDTO",
      "no.ndla.common.model.api.myndla.MyNDLAUserDTO",
      "no.ndla.common.model.api.myndla.UpdatedMyNDLAUserDTO",
      "no.ndla.common.model.domain.myndla.UserRole",
      "no.ndla.common.model.domain.ResourceType",
      "config.ConfigMetaDTO",
      "FolderDTO",
      "FolderDataDTO",
      "NewFolderDTO",
      "UpdatedFolderDTO",
      "NewResourceDTO",
      "UpdatedResourceDTO",
      "StatsDTO",
      "SingleResourceStatsDTO",
      "UserFolderDTO"
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
