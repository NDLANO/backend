/*
 * Part of NDLA draft-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.draftapi.model.domain

import com.sksamuel.elastic4s.http.RequestFailure

case class NdlaSearchException(rf: RequestFailure)
    extends RuntimeException(
      s"""
     |index: ${rf.error.index.getOrElse("Error did not contain index")}
     |reason: ${rf.error.reason}
     |body: ${rf.body}
     |shard: ${rf.error.shard.getOrElse("Error did not contain shard")}
     |type: ${rf.error.`type`}
   """.stripMargin
    )
