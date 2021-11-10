import Dependencies._
import java.util.Properties

lazy val article_api = (project in file("./article-api/"))
  .settings(
    name := "article-api",
    libraryDependencies := articleApiDependencies
  )
  .configs(PactTest)
  .settings(PactSettings)
  .settings(commonSettings: _*)
  .settings(assemblySettings)
  .settings(dockerSettings)
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)
  .enablePlugins(ScalaPactPlugin)
  .disablePlugins(ScalaTsiPlugin)

lazy val draft_api = (project in file("./draft-api/"))
  .settings(
    name := "draft-api",
    libraryDependencies := draftApiDependencies
  )
  .configs(PactTest)
  .settings(PactSettings)
  .settings(commonSettings: _*)
  .settings(assemblySettings)
  .settings(dockerSettings)
  .enablePlugins(ScalaTsiPlugin)
  .settings(
    // The classes that you want to generate typescript interfaces for
    typescriptGenerationImports := Seq("no.ndla.draftapi.model.api._", "no.ndla.draftapi.model.api.TSTypes._"),
    typescriptExports := Seq(
      "Agreement",
      "Article",
      "NewArticle",
      "UpdatedAgreement",
      "UpdatedArticle",
      "UpdatedUserData",
      "UserData"
    ),
    typescriptOutputFile := baseDirectory.value / "draft-api" / "typescript" / "index.ts",
  )
  .enablePlugins(DockerPlugin)
  .enablePlugins(JettyPlugin)
  .enablePlugins(ScalaPactPlugin)

lazy val commonSettings = Seq(
  organization := "ndla",
  version := "0.0.1",
  scalaVersion := ScalaV,
  javacOptions ++= Seq("-source", "1.8", "-target", "1.8"),
  scalacOptions := Seq("-target:jvm-1.8", "-unchecked", "-deprecation", "-feature"),
  Test / parallelExecution := false,
  resolvers ++= scala.util.Properties
    .envOrNone("NDLA_RELEASES")
    .map(repo => "Release Sonatype Nexus Repository Manager" at repo)
    .toSeq
)



lazy val PactTest = config("pact") extend Test
lazy val PactSettings = {
  inConfig(PactTest)(Defaults.testTasks)
  // Since pactTest gets its options from Test configuration, the 'Test' (default) config won't run PactProviderTests
  // To run all tests use pact config ('sbt pact:test')
  Test / testOptions := Seq(Tests.Argument("-l", "PactProviderTest"))
  (PactTest / testOptions).withRank(KeyRanks.Invisible) := Seq.empty
}

lazy val assemblySettings = Seq(
  assembly / assemblyJarName := name.value + ".jar",
//  assembly / mainClass := Some("no.ndla.draftapi.JettyLauncher"),
  assembly / assemblyMergeStrategy := {
    case "module-info.class"                                           => MergeStrategy.discard
    case x if x.endsWith("/module-info.class")                         => MergeStrategy.discard
    case "mime.types"                                                  => MergeStrategy.filterDistinctLines
    case PathList("org", "joda", "convert", "ToString.class")          => MergeStrategy.first
    case PathList("org", "joda", "convert", "FromString.class")        => MergeStrategy.first
    case PathList("org", "joda", "time", "base", "BaseDateTime.class") => MergeStrategy.first
    case x =>
      val oldStrategy = (assembly / assemblyMergeStrategy).value
      oldStrategy(x)
  }
)

lazy val dockerSettings = Seq(
  docker := (docker dependsOn assembly).value,
  docker / dockerfile := {
    val artifact = (assembly / assemblyOutputPath).value
    val artifactTargetPath = s"/app/${artifact.name}"
    new Dockerfile {
      from("adoptopenjdk/openjdk11:alpine-slim")
      run("apk", "--no-cache", "add", "ttf-dejavu")
      add(artifact, artifactTargetPath)
      entryPoint("java", "-Dorg.scalatra.environment=production", "-jar", artifactTargetPath)
    }
  },
  docker / imageNames := Seq(
  ImageName(namespace = Some(organization.value),
    repository = name.value,
    tag = Some(System.getProperty("docker.tag", "SNAPSHOT")))
  )
)

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


