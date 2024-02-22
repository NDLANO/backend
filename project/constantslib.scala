import Dependencies.versions.*
import _root_.io.github.davidgregory084.TpolecatPlugin.autoImport.*
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.{
  typescriptExports,
  typescriptGenerationImports,
  typescriptOutputFile
}
import com.scalatsi.plugin.ScalaTsiPlugin.autoImport.typescriptGenerationImports
import sbt.*
import sbt.Keys.*

object constantslib extends Module {
  override val moduleName: String      = "constants"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumJson4s,
      enumeratumCirce,
      sttp,
      scalikejdbc,
      scalaTsi
    )
  )
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    tsSettings

  lazy val tsSettings: Seq[Def.Setting[_]] = Seq(
    typescriptGenerationImports := Seq(
      "no.ndla.myndla.model.domain.config._",
      "no.ndla.network.tapir.auth._"
    ),
    typescriptExports    := Seq("ConfigKey", "Permission"),
    typescriptOutputFile := file("./typescript/constants-backend/index.ts")
  )

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    ScalaTsiPlugin
  )
}
