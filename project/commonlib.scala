import sbt.Keys._
import sbt._
import com.scalatsi.plugin.ScalaTsiPlugin
import Dependencies.versions._

object commonlib extends Module {
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "common",
    libraryDependencies := Seq(
      "org.scala-lang" % "scala-compiler" % ScalaV
    )
  ) ++
    commonSettings

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
