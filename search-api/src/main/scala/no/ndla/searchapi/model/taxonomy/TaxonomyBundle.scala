/*
 * Part of NDLA search-api
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.searchapi.model.taxonomy

case class TaxonomyBundle(
    nodes: List[Node]
) {

  val nodeByContentUri: Map[String, List[Node]] = {
    val contentUriToNodes = nodes.flatMap(t => t.contentUri.map(cu => cu -> t))
    contentUriToNodes.groupMap(_._1)(_._2)
  }
}
