import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.{
  typescriptExports,
  typescriptGenerationImports,
  typescriptOutputFile
}
import sbt.*
import sbt.Keys.*

object constantslib extends Module {
  override val moduleName: String      = "constants"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumCirce,
      sttp,
      scalikejdbc,
      scalaTsi
    )
  )
  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    tsSettings

  lazy val tsSettings: Seq[Def.Setting[?]] = Seq(
    typescriptGenerationImports := Seq(
      "no.ndla.common.model.domain.config._",
      "no.ndla.network.tapir.auth._",
      "no.ndla.common.model.domain.draft._",
      "no.ndla.common.model.domain.concept._"
    ),
    typescriptExports := Seq(
      "ConfigKey",
      "DraftStatus",
      "Permission",
      "WordClass"
    ),
    typescriptOutputFile := file("./typescript/constants-backend/index.ts")
  )

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    ScalaTsiPlugin
  )
}
