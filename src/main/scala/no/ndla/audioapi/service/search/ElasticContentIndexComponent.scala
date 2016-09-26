/*
 * Part of NDLA audio_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */


package no.ndla.audioapi.service.search

import java.text.SimpleDateFormat
import java.util.Calendar

import com.sksamuel.elastic4s.ElasticDsl._
import com.sksamuel.elastic4s.mappings.FieldType.{IntegerType, StringType}
import com.sksamuel.elastic4s.mappings.NestedFieldDefinition
import com.typesafe.scalalogging.LazyLogging
import io.searchbox.core.{Bulk, Index}
import io.searchbox.indices.aliases.{AddAliasMapping, GetAliases, ModifyAliases, RemoveAliasMapping}
import io.searchbox.indices.mapping.PutMapping
import io.searchbox.indices.{CreateIndex, DeleteIndex, IndicesExists}
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.ElasticClientComponent
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.SearchableLanguageFormats
import no.ndla.audioapi.model.Language._

trait ElasticContentIndexComponent {
  this: ElasticClientComponent with SearchConverterService =>
  val elasticContentIndex: ElasticContentIndex

  class ElasticContentIndex extends LazyLogging {

    def indexDocuments(audioData: List[AudioMetaInformation], indexName: String): Int = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      val searchableAudio = audioData.map(searchConverterService.asSearchableAudioInformation)

      val bulkBuilder = new Bulk.Builder()
      searchableAudio.foreach(audioMeta => {
        val source = write(audioMeta)
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(AudioApiProperties.SearchDocument).id(audioMeta.id).build)
      })

      val response = jestClient.execute(bulkBuilder.build())
      if (!response.isSucceeded) {
        throw new ElasticsearchException(s"Unable to index documents to ${AudioApiProperties.SearchIndex}. ErrorMessage: {}", response.getErrorMessage)
      }
      logger.info(s"Indexed ${searchableAudio.size} documents")
      searchableAudio.size
    }

    def createIndex(): String = {
      val indexName = AudioApiProperties.SearchIndex + "_" + getTimestamp
      if (!indexExists(indexName)) {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.isSucceeded match {
          case false => throw new ElasticsearchException(s"Unable to create index $indexName. ErrorMessage: {}", createIndexResponse.getErrorMessage)
          case true => createMapping(indexName)
        }
      }
      indexName
    }

    def createMapping(indexName: String) = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, AudioApiProperties.SearchDocument, buildMapping()).build())
      if (!mappingResponse.isSucceeded) {
        throw new ElasticsearchException(s"Unable to create mapping for index $indexName. ErrorMessage: {}", mappingResponse.getErrorMessage)
      }
    }

    def buildMapping(): String = {
      mapping(AudioApiProperties.SearchDocument).fields(
        "id" typed IntegerType,
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("tags", keepRaw = false),
        "license" typed StringType index "not_analyzed",
        "authors" typed StringType
      ).buildWithName.string()
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false) = {
      val languageSupportedField = new NestedFieldDefinition(fieldName)
      languageSupportedField._fields = keepRaw match {
        case true => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer fields ("raw" typed StringType index "not_analyzed"))
        case false => languageAnalyzers.map(langAnalyzer => langAnalyzer.lang typed StringType analyzer langAnalyzer.analyzer)
      }

      languageSupportedField
    }

    def aliasTarget: Option[String] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${AudioApiProperties.SearchIndex}").build()
      val result = jestClient.execute(getAliasRequest)
      result.isSucceeded match {
        case false => None
        case true => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Some(aliasIterator.next().getKey)
            case false => None
          }
        }
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String) = {
      if (!indexExists(newIndexName)) {
        throw new IllegalArgumentException(s"No such index: $newIndexName")
      }

      val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, AudioApiProperties.SearchIndex).build()
      val modifyAliasRequest = oldIndexName match {
        case None => new ModifyAliases.Builder(addAliasDefinition).build()
        case Some(oldIndex) => {
          new ModifyAliases.Builder(
            new RemoveAliasMapping.Builder(oldIndex, AudioApiProperties.SearchIndex).build()
          ).addAlias(addAliasDefinition).build()
        }
      }

      val response = jestClient.execute(modifyAliasRequest)
      if (!response.isSucceeded) {
        logger.error(response.getErrorMessage)
        throw new ElasticsearchException(s"Unable to modify alias ${AudioApiProperties.SearchIndex} -> $oldIndexName to ${AudioApiProperties.SearchIndex} -> $newIndexName. ErrorMessage: {}", response.getErrorMessage)
      }
    }

    def delete(indexName: String) = {
      if (!indexExists(indexName)) {
        throw new IllegalArgumentException(s"No such index: $indexName")
      }

      val response = jestClient.execute(new DeleteIndex.Builder(indexName).build())
      if (!response.isSucceeded) {
        throw new ElasticsearchException(s"Unable to delete index $indexName. ErrorMessage: {}", response.getErrorMessage)
      }
    }

    def indexExists(indexName: String): Boolean = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()).isSucceeded
    }

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
