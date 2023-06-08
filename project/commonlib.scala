import sbt.Keys._
import sbt._
import Dependencies.versions._
import com.earldouglas.xwp.JettyPlugin
import com.scalatsi.plugin.ScalaTsiPlugin
import _root_.io.github.davidgregory084.TpolecatPlugin.autoImport.*
import _root_.io.github.davidgregory084.ScalaVersion.*
import _root_.io.github.davidgregory084.ScalacOption

object commonlib extends Module {
  override val moduleName: String = "common"
  lazy val dependencies: Seq[ModuleID] = withLogging(
    Seq(
      enumeratum,
      enumeratumJson4s,
      sttp,
      "org.json4s"       %% "json4s-native"     % Json4SV,
      "org.json4s"       %% "json4s-ext"        % Json4SV,
      "javax.servlet"     % "javax.servlet-api" % JavaxServletV,
      "org.scala-lang"    % "scala-compiler"    % ScalaV,
      "org.eclipse.jetty" % "jetty-webapp"      % JettyV  % "compile",
      "org.eclipse.jetty" % "jetty-plus"        % JettyV  % "container",
      "javax.servlet"     % "javax.servlet-api" % "4.0.1" % "container;provided;test"
    ),
    melody,
    scalatra,
    tapirHttp4sCirce
  )
  val commonTestExcludeOptions = Set(ScalacOptions.warnUnusedPatVars)
  override lazy val settings: Seq[Def.Setting[_]] = Seq(
    libraryDependencies ++= dependencies,
    tpolecatExcludeOptions ++= commonTestExcludeOptions ++ excludeOptions
  ) ++
    commonSettings

  override lazy val plugins: Seq[sbt.Plugins] = Seq(
    JettyPlugin
  )

  override lazy val disablePlugins = Seq(ScalaTsiPlugin)
}
