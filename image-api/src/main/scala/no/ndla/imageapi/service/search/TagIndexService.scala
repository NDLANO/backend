/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.SearchableTag
import no.ndla.imageapi.repository.{ImageRepository, Repository}

trait TagIndexService {
  this: SearchConverterService with IndexService with ImageRepository with Props =>
  val tagIndexService: TagIndexService

  class TagIndexService extends StrictLogging with IndexService[ImageMetaInformation, SearchableTag] {
    override val documentType: String                         = props.TagSearchDocument
    override val searchIndex: String                          = props.TagSearchIndex
    override val repository: Repository[ImageMetaInformation] = imageRepository

    override def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = CirceUtil.toJsonString(t)
        indexInto(indexName).doc(source).id(s"${t.language}.${t.tag}")
      })
    }

    def getMapping: MappingDefinition = {
      properties(
        List(
          textField("tag").fields(keywordField("raw")),
          keywordField("language")
        )
      )
    }
  }

}
