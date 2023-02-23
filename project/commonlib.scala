import sbt.Keys._
import sbt._
import Dependencies.versions._
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin

object commonlib extends Module {
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      sttp,
      "javax.servlet"       % "javax.servlet-api" % JavaxServletV,
      "org.eclipse.jetty"   % "jetty-webapp"      % JettyV  % "compile",
      "org.eclipse.jetty"   % "jetty-plus"        % JettyV  % "container",
      "javax.servlet"       % "javax.servlet-api" % "4.0.1" % "container;provided;test",
      "net.bull.javamelody" % "javamelody-core"   % JavaMelodyV
    ) ++ scalatra ++ scalaTestAndMockito
  )
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    name := "common",
    libraryDependencies ++= dependencies
  ) ++
    commonSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    JettyPlugin
  )

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
