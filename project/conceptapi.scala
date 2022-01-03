import Dependencies._
import Dependencies.common._
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin
import sbtdocker.DockerPlugin

object conceptapi {
  val mainClass = "no.ndla.conceptapi.JettyLauncher"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaLanguage,
      ndlaNetwork,
      ndlaMapping,
      ndlaValidation,
      ndlaScalatestsuite,
      elastic4sCore,
      elastic4sHttp,
      scalaTsi,
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "net.bull.javamelody" % "javamelody-core" % "1.74.0",
      "org.jrobin" % "jrobin" % "1.5.9",
      "com.amazonaws" % "aws-java-sdk-cloudwatch" % AwsSdkV,
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test",
      "org.flywaydb" % "flyway-core" % FlywayV,
      "org.scalikejdbc" %% "scalikejdbc" % ScalikeJDBCV,
      "com.zaxxer" % "HikariCP" % HikariConnectionPoolV,
      "org.postgresql" % "postgresql" % PostgresV,
      "org.elasticsearch" % "elasticsearch" % ElasticsearchV,
      "org.typelevel" %% "cats-core" % "2.1.1",
      "org.typelevel" %% "cats-effect" % "2.1.1",
      "vc.inreach.aws" % "aws-signing-request-interceptor" % "0.0.22"
    ) ++ scalatra ++ vulnerabilityOverrides)

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
    name = "concept-api",
    imports = Seq("no.ndla.conceptapi.model.api._", "no.ndla.conceptapi.model.api.TSTypes._"),
    exports = Seq(
      "Concept",
      "ConceptSearchParams",
      "ConceptSearchResult",
      "ConceptSummary",
      "DraftConceptSearchParams",
      "NewConcept",
      "SubjectTags",
      "TagsSearchResult",
      "UpdatedConcept",
      "ValidationError",
    )
  )

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "concept-api",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings(mainClass) ++
    dockerSettings() ++
    tsSettings ++
    fmtSettings

  lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    ScalaTsiPlugin,
    AssemblyPlugin
  )

}
