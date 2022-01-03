import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object frontpageapi {
  lazy val mainClass = "no.ndla.frontpageapi.Main"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaNetwork,
      ndlaMapping,
      ndlaScalatestsuite,
      scalaTsi,
      "org.http4s" %% "http4s-circe" % Http4sV,
      "io.circe" %% "circe-generic" % CirceV,
      "io.circe" %% "circe-generic-extras" % CirceV,
      "io.circe" %% "circe-literal" % CirceV,
      "io.circe" %% "circe-parser" % CirceV,
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "org.postgresql" % "postgresql" % PostgresV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.http4s" %% "rho-swagger" % RhoV,
      "org.http4s" %% "http4s-server" % Http4sV,
      "org.http4s" %% "http4s-dsl" % Http4sV,
      "org.http4s" %% "http4s-blaze-server" % Http4sV,
      "org.flywaydb" % "flyway-core" % FlywayV,
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "javax.servlet" % "javax.servlet-api" % "4.0.1"
    ) ++ vulnerabilityOverrides)

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    name = "frontpage-api",
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

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "frontpage-api",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings(mainClass) ++
    dockerSettings() ++
    tsSettings ++
    fmtSettings

  lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    ScalaTsiPlugin
  )

}
