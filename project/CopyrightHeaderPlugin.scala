/*
 * Part of NDLA backend
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

import CopyrightHeaderPlugin.autoImport.{copyrightGenerate, copyrightCheck}
import sbt.*
import sbt.Keys.*
import sbt.AutoPlugin

import java.nio.file.{Files, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.asJavaIterableConverter
import scala.util.matching.Regex

object CopyrightHeaderPlugin extends AutoPlugin {

  object autoImport {
    val copyrightGenerate: TaskKey[Unit] = taskKey[Unit]("Generate copyright for scala files")
    val copyrightCheck: TaskKey[Unit]    = taskKey[Unit]("Check copyright for scala files")
  }

  def copyrightTemplate(year: String, submodule: Option[String]): String = {
    s"""/*
       | * Part of NDLA ${submodule.getOrElse("backend")}
       | * Copyright (C) $year NDLA
       | *
       | * See LICENSE
       | *
       | */""".stripMargin
  }

  def readLines(file: File): List[String] = {
    val src   = Source.fromFile(file)
    val lines = src.getLines().toList
    src.close()
    lines
  }

  def locateCopyright(lines: List[String]): Option[(Int, Int)] = {
    val x     = lines.slice(0, 20)
    val start = x.indexOf("/*")
    val end   = x.indexOf(" */")

    if (start != -1 && end != -1) Some((start, end))
    else None
  }
  val yearPattern: Regex   = """Copyright \(C\) (\d{4})""".r
  val modulePattern: Regex = """Part of NDLA (.*)""".r

  private val currentYear = java.time.Year.now.getValue.toString

  def validateOrFixFile(submodule: Option[String], file: File, skipWrite: Boolean = false): Boolean = {
    val lines             = readLines(file)
    val copyrightLocation = locateCopyright(lines)
    val output = copyrightLocation match {
      case Some((start, end)) =>
        val existingCopyright    = lines.slice(start, end + 1).mkString("\n")
        val year                 = yearPattern.findFirstMatchIn(existingCopyright).map(_.group(1))
        val module               = modulePattern.findFirstMatchIn(existingCopyright).map(_.group(1))
        val cop                  = copyrightTemplate(year.getOrElse(currentYear), module)
        val newCopyrightElements = cop.split("\n")
        lines.patch(start, newCopyrightElements, end + 1)
      case None =>
        val cop = copyrightTemplate(currentYear, submodule)
        cop.split("\n").toList ++ lines
    }
    val mismatch = output != lines
    if (mismatch) {
      println(s"${file.getAbsolutePath}")
      if (!skipWrite)
        Files.write(file.toPath, output.asJava, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }
    mismatch
  }

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    Compile / compile := (Compile / compile dependsOn copyrightGenerate).value,
    copyrightCheck := {
      val appName                = name.value
      val sourceFiles: Seq[File] = (Compile / sources).value
      val testFiles: Seq[File]   = (Test / sources).value
      val allFiles               = sourceFiles ++ testFiles
      val filteredPath           = s"/$appName/target/"
      val filteredFiles          = allFiles.filterNot(_.getAbsolutePath.contains(filteredPath))

      val gotError = filteredFiles.foldLeft(false) { case (foundError, file) =>
        validateOrFixFile(appName.some, file, skipWrite = true) || foundError
      }
      if (gotError) throw new RuntimeException("Some files have incorrect copyright headers...")
    },
    copyrightGenerate := {
      val appName                = name.value
      val sourceFiles: Seq[File] = (Compile / sources).value
      val testFiles: Seq[File]   = (Test / sources).value
      val allFiles               = sourceFiles ++ testFiles
      val filteredPath           = s"/$appName/target/scala-2.13/src_managed"
      val filteredFiles          = allFiles.filterNot(_.getAbsolutePath.contains(filteredPath))

      filteredFiles.foreach(file => validateOrFixFile(appName.some, file))
    }
  )

  override def trigger: PluginTrigger                    = AllRequirements
  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

}
