/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.taxonomy.service

import no.ndla.taxonomy.domain.NodeConnection

object RankableConnectionUpdater {
  fun rank(
      existingConnections: List<NodeConnection>,
      updatedConnection: NodeConnection,
      desiredRank: Int,
  ): List<NodeConnection> {
    updatedConnection.rank = desiredRank
    if (existingConnections.isEmpty()) return listOf(updatedConnection)

    val sorted =
        existingConnections
            .filter { it.publicId != updatedConnection.publicId }
            .sortedBy { it.rank }

    val (before, after) = sorted.partition { it.rank < desiredRank }

    var lastRank = desiredRank
    for (conn in after) {
      if (conn.rank <= lastRank) {
        conn.rank = lastRank + 1
        lastRank = conn.rank
      } else break
    }
    return before + updatedConnection + after
  }
}
