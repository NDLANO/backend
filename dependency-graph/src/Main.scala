import java.io.File
import scala.meta.*
import scala.meta.dialects.Scala3

object Main {
  def main(args: Array[String]): Unit = {
    println("Building dependency graph")
    parseModule("draft-api")
  }

  def parseModule(module: String): Unit = {

    val files = getScalaFilesRecursivly(module)
    val trees = files.map(parseScalaFile)
    println(s"Parsed ${trees.length} files in module $module")
  }

  def filterFile(f: File): Boolean = {
    f.getName.endsWith(".scala") && f.getName.contains("WriteService") && !f.getName.contains("Test")
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

  def handleTree(t: Tree): Unit = {
    t.children.collect { case cls: Pkg =>
      println(cls.ref)

      println(cls.getClass.getSimpleName)
//      println(s"Class: ${cls.syntax}")
    }
  }

  def parseScalaFile(filePath: String): String = {
    println(s"Parsing: $filePath")
    val path  = new java.io.File(filePath).toPath
    val bytes = java.nio.file.Files.readAllBytes(path)
    val text  = new String(bytes, "UTF-8")
    val input = Input.VirtualFile(path.toString, text)
    val tree  = input.parse[Source].get
    handleTree(tree)
//    println(exampleTree)

    text
  }
}
