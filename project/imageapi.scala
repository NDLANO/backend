import Dependencies.versions.*
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.*
import sbt.Keys.libraryDependencies
import sbtdocker.DockerPlugin

object imageapi extends Module {
  override val moduleName: String        = "image-api"
  override val MainClass: Option[String] = Some("no.ndla.imageapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      sttp,
      jsoup,
      "org.scalatest" %% "scalatest"    % ScalaTestV % "test",
      "org.imgscalr"   % "imgscalr-lib" % "4.2",
      // These are not strictly needed, for most cases, but offers better handling of loading images with encoding issues
      "com.twelvemonkeys.imageio" % "imageio-core" % "3.10.1",
      "com.twelvemonkeys.imageio" % "imageio-jpeg" % "3.11.0",
      "commons-io"                % "commons-io"   % "2.16.1"
    ),
    awsS3,
    melody,
    elastic4s,
    database,
    tapirHttp4sCirce,
    vulnerabilityOverrides
  )

  lazy val tsSettings: Seq[Def.Setting[?]] = typescriptSettings(
    imports = Seq(
      "no.ndla.imageapi.model.api._",
      "no.ndla.imageapi.model.api.UpdateImageMetaInformation._"
    ),
    exports = Seq(
      "Image",
      "ImageMetaInformationV2",
      "ImageMetaInformationV3",
      "ImageMetaSummary",
      "NewImageMetaInformationV2",
      "SearchParams",
      "SearchResult",
      "SearchResultV3",
      "TagsSearchResult",
      "UpdateImageMetaInformation",
      "ValidationError"
    )
  )

  override lazy val settings: Seq[Def.Setting[?]] = Seq(
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    dockerSettings() ++
    tsSettings ++
    assemblySettings()

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    ScalaTsiPlugin
  )
}
