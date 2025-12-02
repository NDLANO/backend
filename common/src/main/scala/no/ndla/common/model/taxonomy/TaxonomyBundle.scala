/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.taxonomy

import scala.collection.mutable.ListBuffer

case class TaxonomyBundle(nodeByContentUri: Map[String, List[Node]], noContentUriNodes: List[Node]) {
  def nodes: List[Node] = nodeByContentUri.valuesIterator.flatten.toList ++ noContentUriNodes
}

object TaxonomyBundle {
  def apply(nodes: List[Node]): TaxonomyBundle = fromNodeList(nodes)

  def fromNodeList(nodes: List[Node]): TaxonomyBundle = {
    // NOTE: Using dirty mutable code for memory/perf reasons
    val curiToNode = scala.collection.mutable.Map[String, List[Node]]()
    val noCuri     = ListBuffer[Node]()
    for (n <- nodes) {
      n.contentUri match {
        case Some(curi) =>
          val existing = curiToNode.getOrElse(curi, List.empty)
          curiToNode.put(curi, existing :+ n)
        case None => noCuri.addOne(n)
      }
    }

    new TaxonomyBundle(curiToNode.toMap, noCuri.toList)

  }

  def empty: TaxonomyBundle = TaxonomyBundle(Map.empty, List.empty)
}
