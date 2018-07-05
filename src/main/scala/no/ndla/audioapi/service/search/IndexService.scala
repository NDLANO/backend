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

import com.sksamuel.elastic4s.http.ElasticDsl._
import com.sksamuel.elastic4s.mappings.{MappingDefinition, NestedFieldDefinition}
import com.typesafe.scalalogging.LazyLogging
import no.ndla.audioapi.AudioApiProperties
import no.ndla.audioapi.integration.Elastic4sClient
import no.ndla.audioapi.model.Language._
import no.ndla.audioapi.model.domain.AudioMetaInformation
import no.ndla.audioapi.model.search.SearchableLanguageFormats
import org.json4s.native.Serialization.write

import scala.util.{Failure, Success, Try}

trait IndexService {
  this: Elastic4sClient with SearchConverterService =>
  val indexService: IndexService

  class IndexService extends LazyLogging {
    implicit val formats = SearchableLanguageFormats.JSonFormats

    def indexDocument(toIndex: AudioMetaInformation): Try[AudioMetaInformation] = {
      val source = write(searchConverterService.asSearchableAudioInformation(toIndex))

      val response = e4sClient.execute {
        indexInto(AudioApiProperties.SearchIndex / AudioApiProperties.SearchDocument)
          .doc(source)
          .id(toIndex.id.get.toString)
      }

      response match {
        case Success(_)  => Success(toIndex)
        case Failure(ex) => Failure(ex)
      }
    }

    def indexDocuments(audioData: List[AudioMetaInformation], indexName: String): Try[Int] = {
      if (audioData.isEmpty) {
        Success(0)
      } else {
        val response = e4sClient.execute {
          bulk(audioData.map(audio => {
            val source = write(searchConverterService.asSearchableAudioInformation(audio))
            indexInto(indexName / AudioApiProperties.SearchDocument).doc(source).id(audio.id.get.toString)
          }))
        }
        response match {
          case Success(_)  => Success(audioData.size)
          case Failure(ex) => Failure(ex)
        }
      }
    }

    def createIndexWithGeneratedName(): Try[String] = {
      createIndexWithName(AudioApiProperties.SearchIndex + "_" + getTimestamp)
    }

    def createIndexWithName(indexName: String): Try[String] = {
      if (indexWithNameExists(indexName).getOrElse(false)) {
        Success(indexName)
      } else {
        val response = e4sClient.execute {
          createIndex(indexName)
            .mappings(buildMapping)
            .indexSetting("max_result_window", AudioApiProperties.ElasticSearchIndexMaxResultWindow)
        }

        response match {
          case Success(_)  => Success(indexName)
          case Failure(ex) => Failure(ex)
        }

      }
    }

    def buildMapping: MappingDefinition = {
      mapping(AudioApiProperties.SearchDocument).fields(
        intField("id"),
        languageSupportedField("titles", keepRaw = true),
        languageSupportedField("tags", keepRaw = false),
        keywordField("license"),
        keywordField("defaultTitle"),
        textField("authors").fielddata(true)
      )
    }

    private def languageSupportedField(fieldName: String, keepRaw: Boolean = false): NestedFieldDefinition = {
      NestedFieldDefinition(fieldName).fields(keepRaw match {
        case true =>
          languageAnalyzers.map(langAnalyzer =>
            textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer).fields(keywordField("raw")))
        case false =>
          languageAnalyzers.map(langAnalyzer =>
            textField(langAnalyzer.lang).fielddata(true).analyzer(langAnalyzer.analyzer))
      })
    }

    def aliasTarget: Try[Option[String]] = {
      val response = e4sClient.execute {
        getAliases(Nil, List(AudioApiProperties.SearchIndex))
      }

      response match {
        case Success(results) => Success(results.result.mappings.headOption.map(t => t._1.name))
        case Failure(ex)      => Failure(ex)
      }

    }

    def updateAliasTarget(oldIndexName: Option[String], newIndexName: String): Try[Any] = {
      if (!indexWithNameExists(newIndexName).getOrElse(false)) {
        Failure(new IllegalArgumentException(s"No such index: $newIndexName"))
      } else {
        oldIndexName match {
          case None => e4sClient.execute(addAlias(AudioApiProperties.SearchIndex).on(newIndexName))
          case Some(oldIndex) =>
            e4sClient.execute {
              removeAlias(AudioApiProperties.SearchIndex).on(oldIndex)
              addAlias(AudioApiProperties.SearchIndex).on(newIndexName)
            }
        }

      }
    }

    def deleteIndexWithName(optIndexName: Option[String]): Try[_] = {
      optIndexName match {
        case None => Success(optIndexName)
        case Some(indexName) => {
          if (!indexWithNameExists(indexName).getOrElse(false)) {
            Failure(new IllegalArgumentException(s"No such index: $indexName"))
          } else {
            e4sClient.execute(deleteIndex(indexName))
          }
        }
      }
    }

    def indexWithNameExists(indexName: String): Try[Boolean] = {
      val response = e4sClient.execute {
        indexExists(indexName)
      }

      response match {
        case Success(resp) if resp.status != 404 => Success(true)
        case Success(_)                          => Success(false)
        case Failure(ex)                         => Failure(ex)
      }
    }

    def getTimestamp: String = {
      new SimpleDateFormat("yyyyMMddHHmmss").format(Calendar.getInstance.getTime)
    }
  }

}
