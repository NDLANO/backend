/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

import scala.collection.mutable

case class TaxonomyBundle(nodeByContentUri: Map[String, List[Node]]) {}

object TaxonomyBundle {
  def apply(nodes: List[Node]): TaxonomyBundle = {
    val map = mutable.Map.empty[String, List[Node]]
    for (n <- nodes) {
      n.contentUri.foreach(cu => {
        val existing = map.getOrElseUpdate(cu, List.empty)
        map.update(cu, existing :+ n)
      })
    }
    new TaxonomyBundle(map.toMap)
  }
}
