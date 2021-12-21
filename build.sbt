import Dependencies._
import java.util.Properties
ThisBuild / scalaVersion := common.ScalaV

lazy val `article-api` = (project in file("./article-api/"))
  .settings(articleapi.settings: _*)
  .configs(articleapi.configs: _*)
  .enablePlugins(articleapi.plugins: _*)

lazy val `draft-api` = (project in file("./draft-api/"))
  .settings(draftapi.settings: _*)
  .configs(draftapi.configs: _*)
  .enablePlugins(draftapi.plugins: _*)

lazy val `audio-api` = (project in file("./audio-api/"))
  .settings(audioapi.settings: _*)
  .enablePlugins(audioapi.plugins: _*)

lazy val `concept-api` = (project in file("./concept-api/"))
  .settings(conceptapi.settings: _*)
  .enablePlugins(conceptapi.plugins: _*)

lazy val `frontpage-api` = (project in file("./frontpage-api/"))
  .settings(frontpageapi.settings: _*)
  .enablePlugins(frontpageapi.plugins: _*)

lazy val `image-api` = (project in file("./image-api/"))
  .settings(imageapi.settings: _*)
  .enablePlugins(imageapi.plugins: _*)

lazy val language = (project in file("./language/"))
  .settings(languagelib.settings: _*)
  .disablePlugins(languagelib.disablePlugins: _*)

lazy val `learningpath-api` = (project in file("./learningpath-api/"))
  .settings(learningpathapi.settings: _*)
  .configs(learningpathapi.configs: _*)
  .enablePlugins(learningpathapi.plugins: _*)

lazy val mapping = (project in file("./mapping/"))
  .settings(mappinglib.settings: _*)
  .disablePlugins(mappinglib.disablePlugins: _*)

lazy val network = (project in file("./network/"))
  .settings(networklib.settings: _*)
  .disablePlugins(networklib.disablePlugins: _*)

lazy val `oembed-proxy` = (project in file("./oembed-proxy/"))
  .settings(oembedproxy.settings: _*)
  .enablePlugins(oembedproxy.plugins: _*)
  .disablePlugins(oembedproxy.disablePlugins: _*)

lazy val scalatestsuite = (project in file("./scalatestsuite/"))
  .settings(scalatestsuitelib.settings: _*)
  .disablePlugins(scalatestsuitelib.disablePlugins: _*)

lazy val `search-api` = (project in file("./search-api/"))
  .settings(searchapi.settings: _*)
  .configs(searchapi.configs: _*)
  .enablePlugins(searchapi.plugins: _*)

lazy val validation = (project in file("./validation/"))
  .settings(validationlib.settings: _*)
  .disablePlugins(validationlib.disablePlugins: _*)
