/*
 * Part of NDLA backend
 * Copyright (C) 2023 NDLA
 *
 * See LICENSE
 *
 */

import sbt.*
import sbt.Keys.*

import scala.io.Source
import scala.util.{Failure, Success, Try}

object AutoEnvPlugin extends AutoPlugin {
  object autoImport {
    lazy val envFilePaths: SettingKey[Seq[String]] =
      settingKey[Seq[String]]("List of env files to attempt to load, non-existant files will be ignored.")
    lazy val disableEnvLoading: SettingKey[Boolean] = settingKey[Boolean]("Disable auto-loading environment variables")
    lazy val disableSystemPropertyLoading: SettingKey[Boolean] =
      settingKey[Boolean]("Disable auto-loading system property variables")
  }

  import autoImport.*

  override def trigger                                   = allRequirements
  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

  private def removeQuotes(value: String): String = value.trim match {
    case quoted if quoted.startsWith("'") && quoted.endsWith("'")   => quoted.substring(1, quoted.length - 1)
    case quoted if quoted.startsWith("\"") && quoted.endsWith("\"") => quoted.substring(1, quoted.length - 1)
    case unquoted                                                   => unquoted
  }

  private def loadVarMap(filename: String): Map[String, String] = Try(Source.fromFile(filename)) match {
    case Failure(_)      => Map.empty
    case Success(source) =>
      val envMap = source.getLines.foldLeft(Map.empty[String, String]) { case (acc, cur) =>
        val withoutExport = cur.stripPrefix("export ")
        val splitByEqual  = withoutExport.split("=")
        if (withoutExport.strip().startsWith("#")) {
          acc
        } else if (splitByEqual.length > 1) {
          val key   = splitByEqual.head
          val value = removeQuotes(splitByEqual.tail.mkString("="))
          acc + (key -> value)
        } else {
          acc
        }
      }
      source.close()

      envMap
  }

  def setSystemProperties(map: Map[String, String], disabled: Boolean): Unit =
    if (!disabled) {
      map.foreach { case (key, value) => System.setProperty(key, value) }
    }

  override def projectSettings: Seq[Setting[?]] = Seq(
    envFilePaths := Seq(
      ".env",
      s".env.${name.value}",
      s"${name.value}/.env"
    ),
    disableEnvLoading            := false,
    disableSystemPropertyLoading := false,
    envVars                      := {
      if (disableEnvLoading.value) {
        state.value.log.info("`disableEnvLoading` disabled, not overriding environment.")
        envVars.value
      } else {
        val fileList = envFilePaths.value.mkString("'", "', '", "'")
        state.value.log.info(s"Loading [$fileList]")
        val env = envFilePaths.value.map(loadVarMap).reduce(_ ++ _)
        setSystemProperties(env, disableSystemPropertyLoading.value)
        env ++ envVars.value
      }
    }
  )
}
