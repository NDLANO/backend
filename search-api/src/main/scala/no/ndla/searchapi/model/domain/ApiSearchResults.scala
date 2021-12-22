/*
 * Part of NDLA search-api.
 * Copyright (C) 2017 NDLA
 *
 * See LICENSE
 */

package no.ndla.searchapi.model.domain

import scala.math.ceil

sealed trait ApiSearchResults {
  def totalCount: Long
  def pageSize: Int

  val numPages: Int = ceil(totalCount.toDouble / pageSize.toDouble).toInt
}

case class ArticleApiSearchResults(totalCount: Long,
                                   page: Int,
                                   pageSize: Int,
                                   language: String,
                                   results: Seq[ArticleApiSearchResult])
    extends ApiSearchResults

case class ImageApiSearchResults(totalCount: Long,
                                 page: Int,
                                 pageSize: Int,
                                 language: String,
                                 results: Seq[ImageApiSearchResult])
    extends ApiSearchResults

case class LearningpathApiSearchResults(totalCount: Long,
                                        page: Int,
                                        pageSize: Int,
                                        language: String,
                                        results: Seq[LearningpathApiSearchResult])
    extends ApiSearchResults

case class AudioApiSearchResults(totalCount: Long,
                                 page: Int,
                                 pageSize: Int,
                                 language: String,
                                 results: Seq[AudioApiSearchResult])
    extends ApiSearchResults
