import com.earldouglas.xwp.JettyPlugin
import com.itv.scalapact.plugin.ScalaPactPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import sbt._
import sbt.Keys._
import sbtdocker.DockerPlugin
import Dependencies.common._
import Dependencies._

object oembedproxy {
  lazy val mainClass = "no.ndla.oembedproxy.JettyLauncher"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      ndlaNetwork,
      "org.eclipse.jetty" % "jetty-webapp" % JettyV % "container;compile",
      "org.eclipse.jetty" % "jetty-plus" % JettyV % "container",
      "javax.servlet" % "javax.servlet-api" % "3.1.0" % "container;provided;test",
      "org.json4s" %% "json4s-native" % Json4SV,
      "org.scalaj" %% "scalaj-http" % "2.4.2",
      "io.lemonlabs" %% "scala-uri" % "1.5.1",
      "org.jsoup" % "jsoup" % "1.11.3",
      "org.scalatest" %% "scalatest" % ScalaTestV % "test",
      "org.mockito" %% "mockito-scala" % MockitoV % "test",
      "org.mockito" %% "mockito-scala-scalatest" % MockitoV % "test"
    ) ++ scalatra ++ vulnerabilityOverrides)

  lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "oembed-proxy",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings ++
    assemblySettings(mainClass) ++
    dockerSettings() ++
    fmtSettings

  lazy val plugins = Seq(
    JettyPlugin,
    DockerPlugin
  )

  lazy val disablePlugins = Seq(
    ScalaTsiPlugin
  )
}
