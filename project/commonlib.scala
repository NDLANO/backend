import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import org.typelevel.sbt.tpolecat.TpolecatPlugin.autoImport.*
import org.typelevel.scalacoptions.*
import sbt.*
import sbt.Keys.*

object commonlib extends Module {
  override val moduleName: String      = "common"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumCirce,
      sttp,
      scalikejdbc,
      scalaTsi
    ),
    awsS3,
    awsTranscribe,
    melody,
    tapirHttp4sCirce
  )
  val commonTestExcludeOptions = Set(ScalacOptions.warnUnusedPatVars)
  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies,
    tpolecatExcludeOptions ++= commonTestExcludeOptions ++ excludeOptions
  ) ++
    commonSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
  )

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
