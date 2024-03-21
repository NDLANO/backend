import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin
import Dependencies.versions._

object draftapi extends Module {
  override val moduleName: String        = "draft-api"
  override val MainClass: Option[String] = Some("no.ndla.draftapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      sttp,
      catsEffect,
      jsoup,
      "org.scalikejdbc" %% "scalikejdbc"             % ScalikeJDBCV,
      "org.scalatest"   %% "scalatest"               % ScalaTestV % "test",
      "com.amazonaws"    % "aws-java-sdk-cloudwatch" % AwsSdkV
    ),
    flexmark,
    awsS3,
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq(
      "no.ndla.draftapi.model.api._",
      "no.ndla.draftapi.model.api.TSTypes._",
      "no.ndla.common.model.domain.Availability"
    ),
    exports = Seq(
      "Article",
      "Availability",
      "NewArticle",
      "SearchResult",
      "GrepCodesSearchResult",
      "TagsSearchResult",
      "UpdatedArticle",
      "UpdatedUserData",
      "UploadedFile",
      "UserData"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies
  ) ++ commonSettings ++ assemblySettings() ++ dockerSettings() ++ tsSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    ScalaTsiPlugin
  )
}
