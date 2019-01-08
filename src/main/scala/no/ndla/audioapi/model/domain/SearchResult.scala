/*
 * Part of NDLA audio-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.audioapi.model.domain
import no.ndla.audioapi.model.api.AudioSummary

case class SearchResult(totalCount: Long,
                        page: Option[Int],
                        pageSize: Int,
                        language: String,
                        results: Seq[AudioSummary],
                        scrollId: Option[String])
