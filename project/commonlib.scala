import sbt.Keys._
import sbt._
import com.scalatsi.plugin.ScalaTsiPlugin

object commonlib extends Module {
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "common"
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
