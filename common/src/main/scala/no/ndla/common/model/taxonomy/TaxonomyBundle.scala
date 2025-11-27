/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.taxonomy

case class TaxonomyBundle(nodeByContentUri: Map[String, List[Node]]) {
  def nodes: List[Node] = nodeByContentUri.valuesIterator.flatten.toList
}

object TaxonomyBundle {
  def apply(nodes: List[Node]): TaxonomyBundle = {
    val nodeByContentUri: Map[String, List[Node]] = {
      val contentUriToNodes = nodes.flatMap(t => t.contentUri.map(cu => cu -> t))
      contentUriToNodes.groupMap(_._1)(_._2)
    }

    new TaxonomyBundle(nodeByContentUri)
  }

  def fromNodeList(nodes: List[Node]): TaxonomyBundle = apply(nodes)

  def empty: TaxonomyBundle = TaxonomyBundle(Map.empty)
}
