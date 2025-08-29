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

  case class ClassIdentifier(name: String, packageName: String) {
    override def toString: String = s"$packageName.$name"
  }

  case class DependencyEdge(from: ClassIdentifier, to: ClassIdentifier)

  case class DependencyGraph(
      nodes: Set[ClassIdentifier],
      edges: Set[DependencyEdge]
  ) {
    def dependenciesOf(classId: ClassIdentifier): Set[ClassIdentifier] = {
      edges.filter(_.from == classId).map(_.to)
    }

    def dependents(classId: ClassIdentifier): Set[ClassIdentifier] = {
      edges.filter(_.to == classId).map(_.from)
    }
  }

  def buildDependencyGraph(classes: List[ClassWithArguments]): DependencyGraph = {
    // Create a map for quick lookup of classes by name and package
    val classMap = classes.map(c => ClassIdentifier(c.name, c.packageName) -> c).toMap

    // Create all nodes
    val nodes = classMap.keySet

    // Create edges based on constructor arguments
    val edges = for {
      cls <- classes
      fromId = ClassIdentifier(cls.name, cls.packageName)
      arg <- cls.arguments
      // Include all arguments - both explicit and implicit dependencies matter for cycle detection
      argPackage <- arg.packageName
      // Extract the actual class name from the argument type
      actualTypeName = extractClassName(arg.argType)
      // Try to find a matching class in our parsed classes
      toId <- classMap.keys.find(id => id.name == actualTypeName && id.packageName == argPackage)
    } yield DependencyEdge(fromId, toId)

    val graph = DependencyGraph(nodes, edges.toSet)

//    println(s"\nDependency Graph Summary:")
//    println(s"Total classes: ${nodes.size}")
//    println(s"Total dependencies: ${edges.size}")

    if (edges.nonEmpty) {
//      println(s"\nDependencies found:")
      edges.groupBy(_.from).foreach { case (from, deps) =>
        val depList = deps.map(_.to.toString).mkString(", ")
//        println(s"  $from -> [$depList]")
      }
    }

    graph
  }

  def extractClassName(argType: String): String = {
    val cleanType = if (argType.trim.startsWith("=>")) {
      argType.trim.stripPrefix("=>").trim
    } else {
      argType.trim
    }

    val genericPattern = """^(\w+)\[(.+)\]$""".r
    cleanType match {
      case genericPattern(containerType, innerType) =>
        innerType.trim
      case _ =>
        cleanType
    }
  }

  def findCyclicalDependencies(classes: List[ClassWithArguments]): Unit = {
    val graph  = buildDependencyGraph(classes)
    val cycles = detectCycles(graph)

    if (cycles.isEmpty) {
      println("\nNo cyclical dependencies found! ✅")
    } else {
      println(s"\n⚠️  Found ${cycles.size} cyclical dependencies:")
      cycles.zipWithIndex.foreach { case (cycle, index) =>
        println(s"  Cycle ${index + 1}: ${cycle.mkString(" -> ")} -> ${cycle.head}")
      }
    }
  }

  def detectCycles(graph: DependencyGraph): List[List[ClassIdentifier]] = {
    var visited        = Set.empty[ClassIdentifier]
    var recursionStack = Set.empty[ClassIdentifier]
    var cycles         = List.empty[List[ClassIdentifier]]

    def dfs(node: ClassIdentifier, path: List[ClassIdentifier]): Unit = {
      if (recursionStack.contains(node)) {
        // Found a cycle - extract the cycle from the path
        val cycleStart = path.indexOf(node)
        if (cycleStart >= 0) {
          val cycle = path.drop(cycleStart)
          // Only add cycles that have more than one node (avoid self-references unless they're real)
          if (cycle.size > 1 || graph.dependenciesOf(node).contains(node)) {
            cycles = cycle :: cycles
          }
        }
        return
      }

      if (visited.contains(node)) return

      visited += node
      recursionStack += node

      val dependencies = graph.dependenciesOf(node)
      dependencies.foreach { dep =>
        dfs(dep, node :: path)
      }

      recursionStack -= node
    }

    graph.nodes.foreach { node =>
      if (!visited.contains(node)) {
        dfs(node, List.empty)
      }
    }

    cycles.distinct
  }

  def parseModule(module: String): Unit = {
    val files   = getScalaFilesRecursivly(module)
    val classes = files.flatMap(parseScalaFile)
    println(s"Extracted classes and their constructor arguments in $module\n\n")
    findCyclicalDependencies(classes)
  }

  def filterFile(f: File): Boolean = {
    f.getName.endsWith(".scala") &&
    !f.toString.contains("/test/") &&
    !f.toString.contains("/model/")
    // && f.getName.contains("WriteService")
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

        println(
          s"${textDocument.uri}: Could not find unique occurrence for param: ${param} found: $found"
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
  }
}
