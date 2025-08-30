import java.io.File
import java.nio.file.Files
import scala.meta.*
import scala.meta.dialects.Scala3
import scala.meta.internal.semanticdb.SymbolOccurrence.Role.REFERENCE
import scala.meta.internal.semanticdb.{TextDocument, TextDocuments}
import scala.sys.exit

object ScalaFileParser {

  case class ClassWithArguments(
      name: String,
      packageName: String,
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
    if (typeName == "String") return Some("java.lang")
    if (typeName == "Long") return Some("scala")
    if (typeName == "Int") return Some("scala")
    if (typeName.startsWith("List[")) return Some("scala")
    if (typeName.startsWith("Seq[")) return Some("scala")
    if (typeName.startsWith("Option[")) return Some("scala")
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

    def fixSymbol(symbol: String): String = {
      symbol.replace("/", ".").stripSuffix(s".$typeName#")
    }

    refs match {
      case Seq(head) =>
        Some(fixSymbol(head.symbol))
      case _ =>
        val textMatched = found.filter(occ => occ.symbol.endsWith(s"$typeName#"))
        // TODO: There is probably some better way to find this reference that doesnt rely on picking one if there are multiple
        if (textMatched.nonEmpty)
          return textMatched.headOption.map(s => fixSymbol(s.symbol))
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

  def findClassArguments(cls: Defn.Class, pkg: Pkg, textDocument: TextDocument): ClassWithArguments = {
    val argumentLists = cls.ctor.paramClauses
    val arguments     = argumentLists.flatMap { list =>
      val isImplicit = list.mod match {
        case Some(Mod.Using())    => true
        case Some(Mod.Implicit()) => true
        case _                    => false
      }
      list.values.flatMap(p => parseArgument(p, isImplicit, textDocument))
    }

    ClassWithArguments(
      name = cls.name.value,
      packageName = pkg.ref.syntax,
      arguments = arguments.toList
    )
  }

  def handleTree(t: Tree, textDocument: TextDocument): List[ClassWithArguments] = {
    val x = t.children.collect { case pkg: Pkg =>
      val classes = pkg.body.children.collect { case x: Defn.Class => x }
      classes.map(c => findClassArguments(c, pkg, textDocument))
    }

    x.flatten
  }

  def parseScalaFile(filePath: String): List[ClassWithArguments] = {
    val semanticDBFile = new File(s"$filePath.semanticdb")
    if (!semanticDBFile.exists()) {
      Logger.error(s"No semanticdb file found for $filePath, please compile module first.")
      exit(1)
    }
    val dbBytes       = Files.readAllBytes(semanticDBFile.toPath)
    val textDocuments = TextDocuments.parseFrom(dbBytes)
    val textDocument  = textDocuments.documents.head

    val path  = new File(filePath).toPath
    val bytes = Files.readAllBytes(path)
    val text  = new String(bytes)
    val input = Input.VirtualFile(path.toString, text)
    val tree  = input.parse[Source].get
    handleTree(tree, textDocument)
  }
}
