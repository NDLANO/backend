import java.io.File
import java.nio.file.Files
import scala.collection.immutable.{AbstractSeq, LinearSeq}
import scala.meta.*
import scala.meta.dialects.Scala3
import scala.meta.internal.io.ListFiles
import scala.meta.internal.semanticdb.SymbolOccurrence.Role.{DEFINITION, REFERENCE}
import scala.meta.internal.semanticdb.{
  ClassSignature,
  MethodSignature,
  Signature,
  TextDocument,
  TextDocuments,
  TypeSignature,
  ValueSignature
}
import scala.util.boundary

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

  case class ClassWithArguments(
      name: String,
      arguments: List[ParsedArgument]
  )
  case class ParsedArgument(
      name: String,
      argType: String,
      packageName: Option[String],
      isImplicit: Boolean,
      isLazyFunctionType: Boolean
  )

  def findPackageName(
      param: Term.Param,
      typeName: String,
      textDocument: TextDocument
  ): Option[String] = {
    val found = textDocument.occurrences.filter { occ =>
      occ.range.exists { r =>
        val paramStart = param.pos.startLine
        val paramEnd   = param.pos.endLine
        val start      = r.startLine
        val end        = r.endLine
        paramStart >= start && paramEnd <= end
      }
    }
    val refs = found.filter { occ => occ.role == REFERENCE }

    refs match {
      case Seq(head) =>
        Some(head.symbol.replace("/", ".").stripSuffix(s".$typeName#"))
      case _ =>
        println(
          s"Could not find unique occurrence for param: $param in textDocument: ${textDocument.uri}, found: $found"
        )
        None
    }
  }

  def getParamType(param: Term.Param): Option[(String, Boolean)] = {
    param.decltpe match {
      case None        => None
      case Some(value) =>
        if (value.toString().startsWith("=>")) {
          Some((value.toString().stripPrefix("=>").trim, true))
        } else {
          Some((value.toString(), false))
        }
    }
  }

  def parseArgument(
      param: Term.Param,
      isImplicit: Boolean,
      textDocument: TextDocument
  ): Option[ParsedArgument] = {
    getParamType(param) match {
      case None                   => None
      case Some(typeName, isFunc) =>
        val packageName = findPackageName(param, typeName, textDocument)

        Some(
          ParsedArgument(
            name = param.name.value,
            argType = typeName,
            packageName = packageName,
            isImplicit = isImplicit,
            isLazyFunctionType = isFunc
          )
        )
    }
  }

  def findClassArguments(cls: Defn.Class, textDocument: TextDocument): Unit = {
    val argumentLists = cls.ctor.paramClauses
    val params        = argumentLists.flatMap { list =>
      val isImplicit = list.mod match {
        case Some(Mod.Using())    => true
        case Some(Mod.Implicit()) => true
        case _                    => false
      }
      list.values.flatMap(p => parseArgument(p, isImplicit, textDocument))
    }

    params.foreach(println)
  }

  def handleTree(t: Tree, textDocument: TextDocument): Unit = {
    t.children.collect { case pkg: Pkg =>
      val classes        = pkg.body.children.collect { case x: Defn.Class => x }
      val classArguments = classes.map(c => findClassArguments(c, textDocument))
    }
  }

  def parseScalaFile(filePath: String): String = {
    println(s"Parsing: $filePath")
    val semanticDBPath = new File(s"$filePath.semanticdb").toPath
    val dbBytes        = Files.readAllBytes(semanticDBPath)
    val textDocuments  = TextDocuments.parseFrom(dbBytes)
    val textDocument   = textDocuments.documents.head

    val path  = new File(filePath).toPath
    val bytes = Files.readAllBytes(path)
    val text  = new String(bytes)
    val input = Input.VirtualFile(path.toString, text)
    val tree  = input.parse[Source].get
    handleTree(tree, textDocument)
    text
  }
}
