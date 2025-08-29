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
    val modules = List("draft-api")
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
    val classMap = classes.map(c => ClassIdentifier(c.name, c.packageName) -> c).toMap
    val nodes    = classMap.keySet
    val edges    = for {
      cls <- classes
      fromId = ClassIdentifier(cls.name, cls.packageName)
      arg        <- cls.arguments
      argPackage <- arg.packageName
      actualTypeName = extractClassName(arg.argType)
      toId <- classMap.keys.find(id => id.name == actualTypeName && id.packageName == argPackage)
    } yield DependencyEdge(fromId, toId)

    DependencyGraph(nodes, edges.toSet)
  }

  def extractClassName(argType: String): String = {
    val cleanType = if (argType.trim.startsWith("=>")) {
      argType.trim.stripPrefix("=>").trim
    } else {
      argType.trim
    }

    val genericPattern = """^(\w+)\[(.+)\]$""".r
    cleanType match {
      case genericPattern(containerType, innerType) => innerType.trim
      case _                                        => cleanType
    }
  }

  def findCyclicalDependencies(classes: List[ClassWithArguments]): Unit = {
    val graph  = buildDependencyGraph(classes)
    val cycles = detectCycles(graph)

    if (cycles.isEmpty) {
      Logger.info("✅ No cyclical dependencies found!")
    } else {
      Logger.error(s"Found ${cycles.size} cyclical dependencies:")
      cycles.zipWithIndex.foreach { case (cycle, index) =>
        Logger.warn(s"  Cycle ${index + 1}: ${cycle.mkString(" -> ")} -> ${cycle.head}")
      }
    }
  }

  def detectCycles(graph: DependencyGraph): List[List[ClassIdentifier]] = {
    var globalVisited = Set.empty[ClassIdentifier]
    var cycles        = List.empty[List[ClassIdentifier]]

    def dfsFromNode(startNode: ClassIdentifier): Unit = {
      var recursionStack = Set.empty[ClassIdentifier]
      var localVisited   = Set.empty[ClassIdentifier]

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

        if (localVisited.contains(node)) return
        localVisited += node
        recursionStack += node
        val dependencies = graph.dependenciesOf(node)
        dependencies.foreach { dep => dfs(dep, node :: path) }
        recursionStack -= node
      }

      if (!globalVisited.contains(startNode)) {
        dfs(startNode, List.empty)
        globalVisited ++= localVisited
      }
    }

    graph.nodes.foreach { node => dfsFromNode(node) }

    // Additional check for direct 2-node cycles that might be missed by DFS
    val directCycles = for {
      nodeA <- graph.nodes
      nodeB <- graph.dependenciesOf(nodeA)
      if graph.dependenciesOf(nodeB).contains(nodeA) && nodeA != nodeB
      // To avoid duplicates, only include cycles where nodeA < nodeB lexicographically
      if nodeA.toString < nodeB.toString
    } yield List(nodeA, nodeB)

    val allCycles = (cycles ++ directCycles).distinct

    // Filter out cycles that are not actually direct paths
    val validCycles = allCycles.filter { cycle =>
      // For 2-node cycles, verify that the dependency actually exists
      if (cycle.size == 2) {
        val nodeA = cycle(0)
        val nodeB = cycle(1)
        graph.dependenciesOf(nodeA).contains(nodeB) && graph.dependenciesOf(nodeB).contains(nodeA)
      } else {
        // For longer cycles, verify each step in the path
        cycle.zip(cycle.tail :+ cycle.head).forall { case (from, to) =>
          graph.dependenciesOf(from).contains(to)
        }
      }
    }

    validCycles
  }

  def parseModule(module: String): Unit = {
    Logger.info(s"Parsing scala files in $module")
    val files   = getScalaFilesRecursivly(module)
    val classes = files.flatMap(parseScalaFile)
    Logger.info(s"Extracted classes and their constructor arguments in $module\n\n")
    findCyclicalDependencies(classes)
  }

  def filterFile(f: File): Boolean = {
    f.getName.endsWith(".scala") &&
    !f.toString.contains("/test/") &&
    !f.toString.contains("/model/")
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

        Logger.warn(
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
