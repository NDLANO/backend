/*
 * Part of NDLA audio-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.audioapi.service

import cats.implicits.*
import no.ndla.audioapi.model.api
import no.ndla.audioapi.repository.{AudioRepository, SeriesRepository}
import no.ndla.audioapi.service.search.{SearchConverterService, TagSearchService}
import no.ndla.common.errors.{NotFoundException, ValidationException}

import scala.util.{Failure, Success, Try}

trait ReadService {
  this: AudioRepository & SeriesRepository & ConverterService & TagSearchService & SearchConverterService =>
  lazy val readService: ReadService

  class ReadService {

    def seriesWithId(seriesId: Long, language: Option[String]): Try[api.SeriesDTO] = {
      seriesRepository.withId(seriesId) match {
        case Failure(ex)   => Failure(ex)
        case Success(None) =>
          Failure(
            NotFoundException(
              s"The series with id '$seriesId' and language '${language.getOrElse("")}' was not found."
            )
          )
        case Success(Some(series)) => converterService.toApiSeries(series, language)
      }
    }

    def getAllTags(input: String, pageSize: Int, page: Int, language: String): Try[api.TagsSearchResultDTO] = {
      val result = tagSearchService.matchingQuery(
        query = input,
        searchLanguage = language,
        page = page,
        pageSize = pageSize
      )

      result.map(searchConverterService.tagSearchResultAsApiResult)
    }

    def withId(id: Long, language: Option[String]): Option[api.AudioMetaInformationDTO] =
      audioRepository.withId(id).flatMap(audio => converterService.toApiAudioMetaInformation(audio, language).toOption)

    def withExternalId(externalId: String, language: Option[String]): Option[api.AudioMetaInformationDTO] =
      audioRepository
        .withExternalId(externalId)
        .flatMap(audio => converterService.toApiAudioMetaInformation(audio, language).toOption)

    def getAudiosByIds(audioIds: List[Long], language: Option[String]): Try[List[api.AudioMetaInformationDTO]] = {
      for {
        ids <-
          if (audioIds.isEmpty) Failure(ValidationException("ids", "Query parameter 'ids' is missing"))
          else Success(audioIds)
        domainAudios <- audioRepository.withIds(ids)
        api          <- domainAudios.traverse(audio => converterService.toApiAudioMetaInformation(audio, language))
      } yield api
    }

    def getMetaAudioDomainDump(pageNo: Int, pageSize: Int): api.AudioMetaDomainDumpDTO = {
      val (safePageNo, safePageSize) = (math.max(pageNo, 1), math.max(pageSize, 0))
      val results                    = audioRepository.getByPage(safePageSize, (safePageNo - 1) * safePageSize)

      api.AudioMetaDomainDumpDTO(audioRepository.audioCount, pageNo, pageSize, results)
    }
  }
}
