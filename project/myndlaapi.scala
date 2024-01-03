import Dependencies.versions.*
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object myndlaapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.myndlaapi.Main")
  override val moduleName: String        = "myndla-api"

  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      sttp,
      "org.scalatest" %% "scalatest"               % ScalaTestV % "test",
      "org.mockito"   %% "mockito-scala"           % MockitoV   % "test",
      "org.mockito"   %% "mockito-scala-scalatest" % MockitoV   % "test",
      "org.flywaydb"   % "flyway-core"             % FlywayV
    ),
    tapirHttp4sCirce,
    database,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    imports = Seq(
      "no.ndla.common.model.api._",
      "no.ndla.myndla.model.api.config._",
      "no.ndla.myndla.model.api._",
      "no.ndla.myndlaapi.model.arena.api._"
    ),
    exports = Seq(
      "ConfigMetaRestricted",
      "MyNDLAUser",
      "config.ConfigMeta",
      "Folder",
      "FolderData",
      "NewFolder",
      "UpdatedFolder",
      "NewResource",
      "UpdatedResource",
      "ArenaOwner",
      "Category",
      "CategoryWithTopics",
      "Flag",
      "NewCategory",
      "NewPost",
      "NewPostNotification",
      "NewFlag",
      "NewTopic",
      "Topic",
      "TopicWithPosts",
      "PaginatedTopics",
      "PaginatedPosts",
      "PaginatedNewPostNotifications",
      "Post"
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
    JettyPlugin,
    ScalaTsiPlugin
  )
}
