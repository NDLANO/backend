import sbt.Keys._
import sbt._
import Dependencies.versions._
import com.scalatsi.plugin.ScalaTsiPlugin
import _root_.io.github.davidgregory084.TpolecatPlugin.autoImport.*
import _root_.io.github.davidgregory084.ScalaVersion.*
import _root_.io.github.davidgregory084.ScalacOption

object commonlib extends Module {
  override val moduleName: String      = "common"
  override val enableReleases: Boolean = false
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumCirce,
      sttp,
      scalikejdbc,
      scalaTsi,
      "org.json4s"   %% "json4s-native"   % Json4SV,
      "org.json4s"   %% "json4s-ext"      % Json4SV,
      "com.amazonaws" % "aws-java-sdk-s3" % AwsSdkV
    ),
    melody,
    tapirHttp4sCirce
  )
  val commonTestExcludeOptions = Set(ScalacOptions.warnUnusedPatVars)
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies,
    tpolecatExcludeOptions ++= commonTestExcludeOptions ++ excludeOptions
  ) ++
    commonSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
  )

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
