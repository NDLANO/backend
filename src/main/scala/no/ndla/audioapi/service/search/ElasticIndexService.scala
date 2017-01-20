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
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.ElasticClient
import no.ndla.audioapi.model.Language._
import no.ndla.audioapi.model.domain.{AudioMetaInformation, NdlaSearchException}
import no.ndla.audioapi.model.search.SearchableLanguageFormats
import org.elasticsearch.ElasticsearchException
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait ElasticIndexService {
  this: ElasticClient with SearchConverterService =>
  val elasticIndexService: ElasticIndexService

  class ElasticIndexService extends LazyLogging {

    def indexDocument(toIndex: AudioMetaInformation): Try[AudioMetaInformation] = {
      implicit val formats = SearchableLanguageFormats.JSonFormats

      val searchableAudio = searchConverterService.asSearchableAudioInformation(toIndex)
      val indexRequest = new Index.Builder(write(searchableAudio)).index(AudioApiProperties.SearchIndex).`type`(AudioApiProperties.SearchDocument).id(searchableAudio.id).build
      val result = jestClient.execute(indexRequest)

      result.map(_ => toIndex)
    }

    def indexDocuments(audioData: List[AudioMetaInformation], indexName: String): Try[Int] = {
      implicit val formats = SearchableLanguageFormats.JSonFormats
      val searchableAudio = audioData.map(searchConverterService.asSearchableAudioInformation)

      val bulkBuilder = new Bulk.Builder()
      searchableAudio.foreach(audioMeta => {
        val source = write(audioMeta)
        bulkBuilder.addAction(new Index.Builder(source).index(indexName).`type`(AudioApiProperties.SearchDocument).id(audioMeta.id).build)
      })

      val response = jestClient.execute(bulkBuilder.build())
      response.map(_ => {
        logger.info(s"Indexed ${searchableAudio.size} documents")
        searchableAudio.size
      })
    }

    def createIndex(): Try[String] = {
      val indexName = AudioApiProperties.SearchIndex + "_" + getTimestamp
      if (indexExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val createIndexResponse = jestClient.execute(new CreateIndex.Builder(indexName).build())
        createIndexResponse.map(_ => createMapping(indexName)).map(_ => indexName)
      }
    }

    def createMapping(indexName: String): Try[String] = {
      val mappingResponse = jestClient.execute(new PutMapping.Builder(indexName, AudioApiProperties.SearchDocument, buildMapping()).build())
      mappingResponse.map(_ => indexName)
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

    def aliasTarget: Try[Option[String]] = {
      val getAliasRequest = new GetAliases.Builder().addIndex(s"${AudioApiProperties.SearchIndex}").build()
      jestClient.execute(getAliasRequest) match {
        case Success(result) => {
          val aliasIterator = result.getJsonObject.entrySet().iterator()
          aliasIterator.hasNext match {
            case true => Success(Some(aliasIterator.next().getKey))
            case false => Success(None)
          }
        }
        case Failure(_: NdlaSearchException) => Success(None)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        val addAliasDefinition = new AddAliasMapping.Builder(newIndexName, AudioApiProperties.SearchIndex).build()
        val modifyAliasRequest = oldIndexName match {
          case None => new ModifyAliases.Builder(addAliasDefinition).build()
          case Some(oldIndex) => {
            new ModifyAliases.Builder(
              new RemoveAliasMapping.Builder(oldIndex, AudioApiProperties.SearchIndex).build()
            ).addAlias(addAliasDefinition).build()
          }
        }

        jestClient.execute(modifyAliasRequest)
      }
    }

    def delete(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success()
        case Some(indexName) => {
          if (!indexExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            jestClient.execute(new DeleteIndex.Builder(indexName).build())
          }
        }
      }
    }

    def indexExists(indexName: String): Try[Boolean] = {
      jestClient.execute(new IndicesExists.Builder(indexName).build()) match {
        case Success(_) => Success(true)
        case Failure(_: ElasticsearchException) => Success(false)
        case Failure(t: Throwable) => Failure(t)
      }
    }

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }
}
