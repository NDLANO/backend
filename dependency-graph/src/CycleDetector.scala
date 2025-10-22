import scala.sys.exit

object CycleDetector {

  case class ClassIdentifier(name: String, packageName: String) {
    override def toString: String = s"$packageName.$name"
  }

  case class DependencyEdge(from: ClassIdentifier, to: ClassIdentifier)

  case class DependencyGraph(nodes: Set[ClassIdentifier], edges: Set[DependencyEdge]) {
    def dependenciesOf(classId: ClassIdentifier): Set[ClassIdentifier] = {
      edges.filter(_.from == classId).map(_.to)
    }

    def dependents(classId: ClassIdentifier): Set[ClassIdentifier] = {
      edges.filter(_.to == classId).map(_.from)
    }
  }

  case class DependencyEdgeWithIndirection(from: ClassIdentifier, to: ClassIdentifier, isIndirect: Boolean)

  case class EnhancedDependencyGraph(nodes: Set[ClassIdentifier], edges: Set[DependencyEdgeWithIndirection]) {
    def dependenciesOf(classId: ClassIdentifier): Set[ClassIdentifier] = {
      edges.filter(_.from == classId).map(_.to)
    }

    def dependents(classId: ClassIdentifier): Set[ClassIdentifier] = {
      edges.filter(_.to == classId).map(_.from)
    }

    def isIndirectDependency(from: ClassIdentifier, to: ClassIdentifier): Boolean = {
      edges.find(e => e.from == from && e.to == to).exists(_.isIndirect)
    }
  }

  def buildDependencyGraph(classes: List[ScalaFileParser.ClassWithArguments]): DependencyGraph = {
    val classMap = classes.map(c => ClassIdentifier(c.name, c.packageName) -> c).toMap
    val nodes    = classMap.keySet
    val edges    = for {
      cls           <- classes
      fromId         = ClassIdentifier(cls.name, cls.packageName)
      arg           <- cls.arguments
      argPackage    <- arg.packageName
      actualTypeName = extractClassName(arg.argType)
      toId          <- classMap.keys.find(id => id.name == actualTypeName && id.packageName == argPackage)
    } yield DependencyEdge(fromId, toId)

    DependencyGraph(nodes, edges.toSet)
  }

  def buildEnhancedDependencyGraph(classes: List[ScalaFileParser.ClassWithArguments]): EnhancedDependencyGraph = {
    val classMap = classes.map(c => ClassIdentifier(c.name, c.packageName) -> c).toMap
    val nodes    = classMap.keySet
    val edges    = for {
      cls           <- classes
      fromId         = ClassIdentifier(cls.name, cls.packageName)
      arg           <- cls.arguments
      argPackage    <- arg.packageName
      actualTypeName = extractClassName(arg.argType)
      toId          <- classMap.keys.find(id => id.name == actualTypeName && id.packageName == argPackage)
    } yield DependencyEdgeWithIndirection(fromId, toId, arg.isLazyFunctionType)

    EnhancedDependencyGraph(nodes, edges.toSet)
  }

  def extractClassName(argType: String): String = {
    val cleanType =
      if (argType.trim.startsWith("=>")) {
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

  def findCyclicalDependencies(classes: List[ScalaFileParser.ClassWithArguments]): Unit = {
    val enhancedGraph = buildEnhancedDependencyGraph(classes)
    val cycles        = detectCyclesWithIndirection(enhancedGraph)

    if (cycles.isEmpty) {
      Logger.info("✅ No cyclical dependencies found!")
    } else {
      val (directCycles, indirectCycles) = cycles.partition(_._2)

      if (directCycles.nonEmpty) {
        Logger.error(s"Found ${directCycles.size} direct cyclical dependencies:")
        directCycles
          .zipWithIndex
          .foreach { case ((cycle, _), index) =>
            Logger.warn(s"  Cycle ${index + 1}: ${cycle.mkString(" -> ")} -> ${cycle.head}")
          }
      }

      if (indirectCycles.nonEmpty) {
        Logger.info(
          s"Found ${indirectCycles.size} cyclical dependencies broken by indirection (=>) - these are generally safe:"
        )
        indirectCycles
          .zipWithIndex
          .foreach { case ((cycle, _), index) =>
            Logger.info(s"  Indirect cycle ${index + 1}: ${cycle.mkString(" -> ")} -> ${cycle.head}")
          }
      }

      // Exit with status code 1 if any direct cycles were found
      if (directCycles.nonEmpty) {
        Logger.error("❌ Exiting with error code 1 due to direct cyclical dependencies")
        exit(1)
      }
    }
  }

  def detectCyclesWithIndirection(graph: EnhancedDependencyGraph): List[(List[ClassIdentifier], Boolean)] = {
    var globalVisited = Set.empty[ClassIdentifier]
    var cycles        = List.empty[(List[ClassIdentifier], Boolean)]

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
              // Check if the cycle is broken by indirection
              val cycleEdges           = cycle.zip(cycle.tail :+ cycle.head)
              val hasBrokenIndirection = cycleEdges.exists { case (from, to) =>
                graph.isIndirectDependency(from, to)
              }
              cycles = (cycle, !hasBrokenIndirection) :: cycles
            }
          }
          return
        }

        if (localVisited.contains(node)) return
        localVisited += node
        recursionStack += node
        val dependencies = graph.dependenciesOf(node)
        dependencies.foreach { dep =>
          dfs(dep, node :: path)
        }
        recursionStack -= node
      }

      if (!globalVisited.contains(startNode)) {
        dfs(startNode, List.empty)
        globalVisited ++= localVisited
      }
    }

    graph
      .nodes
      .foreach { node =>
        dfsFromNode(node)
      }

    // Additional check for direct 2-node cycles that might be missed by DFS
    val directCycles = for {
      nodeA <- graph.nodes
      nodeB <- graph.dependenciesOf(nodeA)
      if graph.dependenciesOf(nodeB).contains(nodeA) && nodeA != nodeB
      // To avoid duplicates, only include cycles where nodeA < nodeB lexicographically
      if nodeA.toString < nodeB.toString
    } yield {
      val cycle                = List(nodeA, nodeB)
      val hasBrokenIndirection = graph.isIndirectDependency(nodeA, nodeB) || graph.isIndirectDependency(nodeB, nodeA)
      (cycle, !hasBrokenIndirection)
    }

    val allCycles = (
      cycles ++ directCycles
    ).distinct

    // Filter out cycles that are not actually valid paths
    val validCycles = allCycles.filter { case (cycle, _) =>
      // For 2-node cycles, verify that the dependency actually exists
      if (cycle.size == 2) {
        val nodeA = cycle(0)
        val nodeB = cycle(1)
        graph.dependenciesOf(nodeA).contains(nodeB) && graph.dependenciesOf(nodeB).contains(nodeA)
      } else {
        // For longer cycles, verify each step in the path
        cycle
          .zip(cycle.tail :+ cycle.head)
          .forall { case (from, to) =>
            graph.dependenciesOf(from).contains(to)
          }
      }
    }

    validCycles
  }
}
