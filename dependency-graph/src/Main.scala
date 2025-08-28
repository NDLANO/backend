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

  case class ParsedArgument(name: String, argType: String, isImplicit: Boolean, isLazyFunctionType: Boolean)

  def parseArgument(param: Term.Param, isImplicit: Boolean): Option[ParsedArgument] = {
    param.decltpe match {
      case None        => None
      case Some(value) =>
        val (typeName, isFunc) = if (value.toString().startsWith("=>")) {
          (value.toString().stripPrefix("=>").trim, true)
        } else {
          (value.toString(), false)
        }

        Some(ParsedArgument(param.name.value, typeName, isImplicit, isFunc))
    }
  }

  def findClassArguments(cls: Defn.Class): Unit = {
    val argumentLists = cls.ctor.paramClauses
    val params        = argumentLists.flatMap { list =>
      val isImplicit = list.mod match {
        case Some(Mod.Using())    => true
        case Some(Mod.Implicit()) => true
        case _                    => false
      }
      list.values.flatMap(p => parseArgument(p, isImplicit))
    }

    params.foreach(println)
  }

  def handleTree(t: Tree): Unit = {
    t.children.collect { case pkg: Pkg =>
      println(pkg.ref)
      println("Pkg: " + pkg.ref)

      val imports        = pkg.body.children.collect { case x: Import => x }
      val classes        = pkg.body.children.collect { case x: Defn.Class => x }
      val classArguments = classes.map(findClassArguments)
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
