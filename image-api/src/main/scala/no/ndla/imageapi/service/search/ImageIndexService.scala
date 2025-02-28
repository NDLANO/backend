/*
 * Part of NDLA image-api
 * Copyright (C) 2021 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service.search

import com.sksamuel.elastic4s.ElasticDsl.*
import com.sksamuel.elastic4s.fields.{ElasticField, ObjectField}
import com.sksamuel.elastic4s.requests.indexes.IndexRequest
import com.sksamuel.elastic4s.requests.mappings.MappingDefinition
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.ImageMetaInformation
import no.ndla.imageapi.model.search.SearchableImage
import no.ndla.imageapi.repository.{ImageRepository, Repository}
import no.ndla.search.SearchLanguage

trait ImageIndexService {
  this: SearchConverterService & IndexService & ImageRepository & Props & SearchLanguage =>
  val imageIndexService: ImageIndexService

  class ImageIndexService extends StrictLogging with IndexService[ImageMetaInformation, SearchableImage] {
    override val documentType: String                         = props.SearchDocument
    override val searchIndex: String                          = props.SearchIndex
    override val repository: Repository[ImageMetaInformation] = imageRepository

    override def createIndexRequests(domainModel: ImageMetaInformation, indexName: String): Seq[IndexRequest] = {
      val searchable = searchConverterService.asSearchableImage(domainModel)
      val source     = CirceUtil.toJsonString(searchable)

      Seq(indexInto(indexName).doc(source).id(domainModel.id.get.toString))
    }

    protected def generateLanguageSupportedFieldList(fieldName: String, keepRaw: Boolean = false): Seq[ElasticField] = {
      if (keepRaw) {
        SearchLanguage.languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .fielddata(false)
            .analyzer(langAnalyzer.analyzer)
            .fields(keywordField("raw"))
        )
      } else {
        SearchLanguage.languageAnalyzers.map(langAnalyzer =>
          textField(s"$fieldName.${langAnalyzer.languageTag.toString}")
            .fielddata(false)
            .analyzer(langAnalyzer.analyzer)
        )
      }
    }

    def getMapping: MappingDefinition = {
      val fields: Seq[ElasticField] = List(
        ObjectField("domainObject", enabled = Some(false)),
        intField("id"),
        keywordField("license"),
        dateField("lastUpdated"),
        keywordField("defaultTitle"),
        keywordField("modelReleased"),
        textField("editorNotes"),
        keywordField("podcastFriendly"),
        keywordField("users"),
        nestedField("imageFiles").fields(
          intField("imageSize"),
          textField("previewUrl"),
          ObjectField(
            "dimensions",
            properties = Seq(
              intField("width"),
              intField("height")
            )
          )
        )
      )

      val dynamics = generateLanguageSupportedFieldList("titles", keepRaw = true) ++
        generateLanguageSupportedFieldList("alttexts") ++
        generateLanguageSupportedFieldList("captions") ++
        generateLanguageSupportedFieldList("tags")

      properties(fields ++ dynamics)
    }
  }

}
