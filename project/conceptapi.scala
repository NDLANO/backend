import Dependencies._
import Dependencies.versions._
import com.earldouglas.xwp.JettyPlugin
import sbt.Keys._
import sbt._
import sbtassembly.AssemblyPlugin
import sbtdocker.DockerPlugin

object conceptapi extends Module {
  override val MainClass: Option[String] = Some("no.ndla.conceptapi.Main")
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      scalaTsi,
      scalaUri,
      enumeratum,
      "org.eclipse.jetty"   % "jetty-webapp"                    % JettyV  % "container;compile",
      "org.eclipse.jetty"   % "jetty-plus"                      % JettyV  % "container",
      "javax.servlet"       % "javax.servlet-api"               % "4.0.1" % "container;provided;test",
      "org.json4s"         %% "json4s-native"                   % Json4SV,
      "net.bull.javamelody" % "javamelody-core"                 % JavaMelodyV,
      "org.jrobin"          % "jrobin"                          % "1.5.9",
      "com.amazonaws"       % "aws-java-sdk-cloudwatch"         % AwsSdkV,
      "org.flywaydb"        % "flyway-core"                     % FlywayV,
      "org.typelevel"      %% "cats-effect"                     % CatsEffectV,
      "vc.inreach.aws"      % "aws-signing-request-interceptor" % "0.0.22"
    ) ++ elastic4s ++ database ++ scalatra ++ vulnerabilityOverrides ++ scalaTestAndMockito
  )

  lazy val tsSettings: Seq[Def.Setting[_]] = typescriptSettings(
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
      "ValidationError"
    )
  )

  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "concept-api",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings() ++
    dockerSettings() ++
    tsSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    DockerPlugin,
    JettyPlugin,
    AssemblyPlugin
  )

}
