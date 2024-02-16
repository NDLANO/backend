/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.SearchableTag
import no.ndla.imageapi.repository.{ImageRepository, Repository}
import no.ndla.search.model.SearchableLanguageFormats
import org.json4s.Formats
import org.json4s.native.Serialization.write

trait TagIndexService {
  this: SearchConverterService with IndexService with ImageRepository with Props =>
  val tagIndexService: TagIndexService

  class TagIndexService extends StrictLogging with IndexService[ImageMetaInformation, SearchableTag] {
    implicit val formats: Formats                             = SearchableLanguageFormats.JSonFormats
    override val documentType: String                         = props.TagSearchDocument
    override val searchIndex: String                          = props.TagSearchIndex
    override val repository: Repository[ImageMetaInformation] = imageRepository

    override def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest] = {
      val tags = searchConverterService.asSearchableTags(domainModel)

      tags.map(t => {
        val source = write(t)
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
