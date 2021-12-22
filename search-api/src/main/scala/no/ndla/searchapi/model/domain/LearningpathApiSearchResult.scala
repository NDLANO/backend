/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

case class LearningpathApiTitle(title: String, language: String)
case class LearningpathApiDescription(description: String, language: String)
case class LearningpathApiIntro(introduction: String, language: String)
case class LearningPathApiTags(tags: Seq[String], language: String)
case class LearningpathApiSearchResult(id: Long,
                                       title: LearningpathApiTitle,
                                       description: LearningpathApiDescription,
                                       introduction: LearningpathApiIntro,
                                       metaUrl: String,
                                       coverPhotoUrl: Option[String],
                                       duration: Option[Int],
                                       status: String,
                                       lastUpdated: String,
                                       tags: LearningPathApiTags,
                                       supportedLanguages: Seq[String],
                                       isBasedOn: Option[Long])
