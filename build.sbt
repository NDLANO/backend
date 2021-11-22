import Dependencies._
import java.util.Properties
ThisBuild / scalaVersion := common.ScalaV

lazy val article_api = (project in file("./article-api/"))
  .settings(articleapi.settings: _*)
  .configs(articleapi.configs: _*)
  .enablePlugins(articleapi.plugins: _*)

lazy val draft_api = (project in file("./draft-api/"))
  .settings(draftapi.settings: _*)
  .configs(draftapi.configs: _*)
  .enablePlugins(draftapi.plugins: _*)

lazy val audio_api = (project in file("./audio-api/"))
  .settings(audioapi.settings:_*)
  .enablePlugins(audioapi.plugins: _*)

lazy val concept_api = (project in file("./concept-api/"))
  .settings(conceptapi.settings:_*)
  .enablePlugins(conceptapi.plugins:_*)

lazy val frontpage_api = (project in file("./frontpage-api/"))
  .settings(frontpageapi.settings:_*)
  .enablePlugins(frontpageapi.plugins:_*)

// TODO: fmt for all projects in repo
//val checkfmt = taskKey[Boolean]("Check for code style errors")
//checkfmt := {
//  val noErrorsInMainFiles = (Compile / scalafmtCheck).value
//  val noErrorsInTestFiles = (Test / scalafmtCheck).value
//  val noErrorsInSbtConfigFiles = (Compile / scalafmtSbtCheck).value
//
//  noErrorsInMainFiles && noErrorsInTestFiles && noErrorsInSbtConfigFiles
//}
//
//Test / test := (Test / test).dependsOn(Test / checkfmt).value
//
//val fmt = taskKey[Unit]("Automatically apply code style fixes")
//fmt := {
//  (Compile / scalafmt).value
//  (Test / scalafmt).value
//  (Compile / scalafmtSbt).value
//}
