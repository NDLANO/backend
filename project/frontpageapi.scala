import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object frontpageapi extends Module {
  override val moduleName: String        = "frontpage-api"
  override val MainClass: Option[String] = Some("no.ndla.frontpageapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      enumeratum,
      enumeratumCirce,
      catsEffect,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test"
    ),
    database,
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.frontpageapi.model.api._", "no.ndla.network.tapir._"),
    exports = Seq(
      "no.ndla.common.model.api.FrontPage",
      "no.ndla.common.model.api.MenuData",
      "no.ndla.common.model.api.Menu",
      "FilmFrontPageData",
      "NewOrUpdatedFilmFrontPageData",
      "SubjectPageData",
      "NewSubjectFrontPageData",
      "UpdatedSubjectFrontPageData",
      "ErrorBody"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
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
