/*
 * Part of NDLA backend
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

import TypescriptGenerationPlugin.autoImport.typescriptGenerate
import sbt.*
import sbt.Keys.*

import java.nio.file.{Files, StandardOpenOption}
import scala.io.Source
import scala.jdk.CollectionConverters.asJavaIterableConverter
import scala.sys.process.ProcessLogger
import scala.util.matching.Regex

object TypescriptGenerationPlugin extends AutoPlugin {

  object autoImport {
    val typescriptGenerate: TaskKey[Unit] = taskKey[Unit]("Generate typescript for openapi files")
  }

  override lazy val projectSettings: Seq[Setting[?]] = Seq(
    // Compile / compile := (Compile / compile dependsOn typescriptGenerate).value,
//    typescriptGenerate := {
//      val appName    = name.value
//      val log        = streams.value.log
//      val className  = (Compile / mainClass).value.get
//      val outputFile = (Compile / sourceManaged).value / "typescript" / "openapi.ts"
//      println(s"Generating typescript for '$appName' from '$className' to '$outputFile'")
//
//      val runTask = (Compile / runMain).toTask(s" $className")
//
//      val result = Def.taskDyn {
//        log.info(s"Running typescript generation $className")
//        runTask.
//      }
//
//      IO.write(outputFile, result.toString)
//      log.info(s"Output written to $outputFile")
//
//    }


    typescriptGenerate := Def.taskDyn {
      val log = streams.value.log
      val className = (Compile / mainClass).value.get
      val outputFile = (Compile / resourceManaged).value / "output.txt"

      log.info(s"Running $className...")

      Def.task {
        val forkOptions = ForkOptions()
        val cp = (Compile / fullClasspath).value.map(_.data)
        val scalaRun = new ForkRun(forkOptions)

        val output = new StringBuffer()
        val processLogger = new ProcessLogger {
          override def out(s: => String): Unit = { output.append(s).append("\n"); log.info(s) }
          override def err(s: => String): Unit = { output.append(s).append("\n"); log.error(s) }
          override def buffer[T](f: => T): T = f
        }

        val exitCode = scalaRun.fork(className, cp, Seq.empty, processLogger)

        if (exitCode.exitValue() == 0) {
          IO.write(outputFile, output.toString)
          log.info(s"Output written to $outputFile")
        } else {
          sys.error(s"Process failed with exit code $exitCode")
        }
      }
    }.value
  )

  override def trigger: PluginTrigger                    = AllRequirements
  override def projectConfigurations: Seq[Configuration] = super.projectConfigurations

}
