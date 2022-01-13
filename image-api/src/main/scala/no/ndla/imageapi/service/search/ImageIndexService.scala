/*
 * Part of NDLA image-api.
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.fields.ElasticField
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.sksamuel.elastic4s.requests.mappings.dynamictemplate.DynamicTemplateRequest
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.{SearchableImage, SearchableLanguageFormats}
import no.ndla.imageapi.repository.{ImageRepository, Repository}
import org.json4s.native.Serialization.write

trait ImageIndexService {
  this: SearchConverterService with IndexService with ImageRepository =>
  val imageIndexService: ImageIndexService

  class ImageIndexService extends LazyLogging with IndexService[ImageMetaInformation, SearchableImage] {
    implicit val formats = SearchableLanguageFormats.JSonFormats
    override val documentType: String = ImageApiProperties.SearchDocument
    override val searchIndex: String = ImageApiProperties.SearchIndex
    override val repository: Repository[ImageMetaInformation] = imageRepository

    override def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest] = {
      val source = write(searchConverterService.asSearchableImage(domainModel))
      Seq(indexInto(indexName).doc(source).id(domainModel.id.get.toString))
    }

    def getMapping: MappingDefinition = {
      val fields: Seq[ElasticField] = List(
        intField("id"),
        keywordField("license"),
        intField("imageSize"),
        textField("previewUrl"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        keywordField("modelReleased"),
        textField("editorNotes"),
      )

      val dynamics: Seq[DynamicTemplateRequest] = generateLanguageSupportedDynamicTemplates("titles", keepRaw = true) ++
        generateLanguageSupportedDynamicTemplates("alttexts", keepRaw = false) ++
        generateLanguageSupportedDynamicTemplates("captions", keepRaw = false) ++
        generateLanguageSupportedDynamicTemplates("tags", keepRaw = false)

      properties(fields).dynamicTemplates(dynamics)
    }
  }

}
