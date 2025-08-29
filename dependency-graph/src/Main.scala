import java.io.File
import java.nio.file.Files
import scala.meta.*
import scala.meta.dialects.Scala3
import scala.meta.internal.semanticdb.SymbolOccurrence.Role.REFERENCE
import scala.meta.internal.semanticdb.{TextDocument, TextDocuments}
import scala.sys.exit

object Main {
  def main(args: Array[String]): Unit = {
    Logger.info("Building dependency graph")
    val modules = detectModules()
    modules.foreach(parseModule)
  }

  def ignoredModules = List(
    "modules",
    "dependency-graph"
  )

  def detectModules(): List[String] = {
    val currentDir = new File(".")
    if (currentDir.exists && currentDir.isDirectory) {
      val dirs = currentDir.listFiles.filter(_.isDirectory).toList
      dirs
        .filter(d => new File(d, "package.mill").exists)
        .map(_.getName)
        .filterNot(ignoredModules.contains)

    } else {
      throw new RuntimeException("Current directory is not a valid directory and that is weird.")
    }
  }

  def parseModule(module: String): Unit = {
    Logger.info(s"---- $module ----")
    val files   = getScalaFilesRecursivly(module)
    val classes = files.flatMap(ScalaFileParser.parseScalaFile)
    CycleDetector.findCyclicalDependencies(classes)
  }

  def filterFile(f: File): Boolean = {
    f.getName.endsWith(".scala") &&
    !f.toString.contains("/test/") &&
    !f.toString.contains("/model/") &&
    !f.toString.contains("scala-2")
  }

  def getScalaFilesRecursivly(directory: String): List[String] = {
    val d = new java.io.File(directory)
    if (d.exists && d.isDirectory) {
      val nestedDirectories = d.listFiles.filter(_.isDirectory).toList
      val filesInD          = d.listFiles.filter(_.isFile).toList
      val scalaFiles        = filesInD.filter(filterFile).map(_.getAbsolutePath)
      val nestedFiles       = nestedDirectories.flatMap(f => getScalaFilesRecursivly(f.getAbsolutePath))
      scalaFiles ++ nestedFiles
    } else {
      List[String]()
    }
  }
}
