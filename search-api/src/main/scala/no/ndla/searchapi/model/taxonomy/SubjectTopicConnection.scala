/*
 * Part of NDLA search-api.
 * Copyright (C) 2018 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.taxonomy

case class SubjectTopicConnection(subjectid: String,
                                  topicid: String,
                                  id: String,
                                  primary: Boolean,
                                  rank: Int,
                                  relevanceId: Option[String])
