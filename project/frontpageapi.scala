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
      "org.flywaydb"   % "flyway-core"             % FlywayV,
      "org.mockito"   %% "mockito-scala"           % MockitoV   % "test",
      "org.mockito"   %% "mockito-scala-scalatest" % MockitoV   % "test",
      "org.scalatest" %% "scalatest"               % ScalaTestV % "test",
      "javax.servlet"  % "javax.servlet-api"       % JavaxServletV
    ),
    database,
    vulnerabilityOverrides,
    tapirHttp4sCirce
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.frontpageapi.model.api._", "no.ndla.network.tapir._"),
    exports = Seq(
      "FrontPage",
      "MenuData",
      "Menu",
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
