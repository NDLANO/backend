/*
 * Part of NDLA common
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.common.model.taxonomy

case class TaxonomyBundle(nodes: List[Node]) {

  val nodeByContentUri: Map[String, List[Node]] = {
    val contentUriToNodes = nodes.flatMap(t => t.contentUri.map(cu => cu -> t))
    contentUriToNodes.groupMap(_._1)(_._2)
  }
}
