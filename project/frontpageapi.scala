import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.versions._
import Dependencies._

object frontpageapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.frontpageapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      enumeratum,
      enumeratumCirce,
      "io.circe"                      %% "circe-generic"           % CirceV,
      "io.circe"                      %% "circe-generic-extras"    % CirceV cross CrossVersion.for3Use2_13,
      "io.circe"                      %% "circe-literal"           % CirceV,
      "io.circe"                      %% "circe-parser"            % CirceV,
      "org.http4s"                    %% "http4s-server"           % Http4sV,
      "org.http4s"                    %% "http4s-dsl"              % Http4sV,
      "org.http4s"                    %% "http4s-circe"            % Http4sV,
      "org.http4s"                    %% "http4s-ember-server"     % Http4sV,
      "org.flywaydb"                   % "flyway-core"             % FlywayV,
      "javax.servlet"                  % "javax.servlet-api"       % JavaxServletV,
      "com.softwaremill.sttp.tapir"   %% "tapir-http4s-server"     % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-swagger-ui-bundle" % TapirV,
      "com.softwaremill.sttp.tapir"   %% "tapir-json-circe"        % TapirV,
      "com.softwaremill.sttp.apispec" %% "openapi-circe-yaml"      % ApiSpecV,
      "org.typelevel"                 %% "cats-effect"             % CatsEffectV
    ) ++ database ++ vulnerabilityOverrides ++ scalaTestAndMockito
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq("no.ndla.frontpageapi.model.api._"),
    exports = Seq(
      "FrontPageData",
      "FilmFrontPageData",
      "NewOrUpdatedFilmFrontPageData",
      "SubjectPageData",
      "NewSubjectFrontPageData",
      "UpdatedSubjectFrontPageData",
      "Error"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "frontpage-api",
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
