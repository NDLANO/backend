/*
 * Part of NDLA concept-api
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.CirceUtil
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.common.model.domain.{Tag, Title, concept}
import no.ndla.common.model.api as commonApi
import no.ndla.common.model.api.ResponsibleDTO
import no.ndla.common.model.domain.concept.{Concept, ConceptContent, ConceptType, VisualElement}
import no.ndla.conceptapi.model.api.{ConceptSearchResultDTO}
import no.ndla.conceptapi.model.domain.SearchResult
import no.ndla.conceptapi.model.search.*
import no.ndla.conceptapi.model.api
import no.ndla.conceptapi.service.ConverterService
import no.ndla.language.Language.{UnknownLanguage, findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.mapping.ISO639
import no.ndla.search.AggregationBuilder.toApiMultiTermsAggregation
import no.ndla.search.SearchConverter.getEmbedValues
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {
    private def getEmbedResourcesAndIdsToIndex(
        visualElement: Seq[VisualElement]
    ): List[EmbedValues] = {
      val visualElementTuples = visualElement.flatMap(v => getEmbedValues(v.visualElement, v.language))
      visualElementTuples.toList

    }

    private def asSearchableCopyright(maybeCopyright: Option[DraftCopyright]): Option[SearchableCopyright] = {
      maybeCopyright.map(c => {
        SearchableCopyright(
          origin = c.origin,
          creators = c.creators,
          rightsholders = c.rightsholders,
          processors = c.processors
        )
      })
    }

    def asSearchableConcept(c: Concept): SearchableConcept = {
      val title   = SearchableLanguageValues(c.title.map(title => LanguageValue(title.language, title.title)))
      val content = SearchableLanguageValues(
        c.content.map(content => LanguageValue(content.language, Jsoup.parseBodyFragment(content.content).text()))
      )
      val tags          = SearchableLanguageList(c.tags.map(tag => LanguageValue(tag.language, tag.tags)))
      val visualElement = SearchableLanguageValues(
        c.visualElement.map(element => LanguageValue(element.language, element.visualElement))
      )

      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(c.visualElement)
      val copyright            = asSearchableCopyright(c.copyright)

      val sortableConceptType = c.conceptType match {
        case ConceptType.CONCEPT =>
          SearchableLanguageValues.from(
            "nb" -> "Forklaring",
            "nn" -> "Forklaring",
            "en" -> "Concept"
          )
        case _ =>
          SearchableLanguageValues.from(
            "nb" -> "Glose",
            "nn" -> "Glose",
            "en" -> "Gloss"
          )
      }

      SearchableConcept(
        id = c.id.get,
        conceptType = c.conceptType.entryName,
        title = title,
        content = content,
        defaultTitle = title.defaultValue,
        tags = tags,
        lastUpdated = c.updated,
        status = Status(c.status.current.toString, c.status.other.map(_.toString).toSeq),
        updatedBy = c.updatedBy,
        license = c.copyright.flatMap(_.license),
        copyright = copyright,
        embedResourcesAndIds = embedResourcesAndIds,
        visualElement = visualElement,
        created = c.created,
        source = c.copyright.flatMap(_.origin),
        responsible = c.responsible,
        gloss = c.glossData.map(_.gloss),
        domainObject = c,
        sortableConceptType = sortableConceptType,
        defaultSortableConceptType = sortableConceptType.defaultValue
      )
    }

    def hitAsConceptSummary(hitString: String, language: String): api.ConceptSummaryDTO = {
      val searchableConcept = CirceUtil.unsafeParseAs[SearchableConcept](hitString)
      val titles            = searchableConcept.title.languageValues.map(lv => Title(lv.value, lv.language))
      val contents          = searchableConcept.content.languageValues.map(lv => ConceptContent(lv.value, lv.language))
      val tags              = searchableConcept.tags.languageValues.map(lv => Tag(lv.value, lv.language))
      val visualElements    =
        searchableConcept.visualElement.languageValues.map(lv => concept.VisualElement(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, contents)

      val title = findByLanguageOrBestEffort(titles, language)
        .map(converterService.toApiConceptTitle)
        .getOrElse(api.ConceptTitleDTO("", "", UnknownLanguage.toString()))
      val content = findByLanguageOrBestEffort(contents, language)
        .map(converterService.toApiConceptContent)
        .getOrElse(api.ConceptContent("", "", UnknownLanguage.toString()))
      val tag           = findByLanguageOrBestEffort(tags, language).map(converterService.toApiTags)
      val visualElement =
        findByLanguageOrBestEffort(visualElements, language).map(converterService.toApiVisualElement)
      val license   = converterService.toApiLicense(searchableConcept.license)
      val copyright = searchableConcept.copyright.map(c => {
        commonApi.DraftCopyrightDTO(
          license = Some(license),
          origin = c.origin,
          creators = c.creators.map(_.toApi),
          processors = c.processors.map(_.toApi),
          rightsholders = c.rightsholders.map(_.toApi),
          validFrom = None,
          validTo = None,
          processed = false
        )
      })

      val responsible     = searchableConcept.responsible.map(r => ResponsibleDTO(r.responsibleId, r.lastUpdated))
      val glossData       = converterService.toApiGlossData(searchableConcept.domainObject.glossData)
      val conceptTypeName = searchableConcept.sortableConceptType
        .getLanguageOrDefault(language)
        .getOrElse(searchableConcept.conceptType)

      api.ConceptSummaryDTO(
        id = searchableConcept.id,
        title = title,
        content = content,
        tags = tag,
        supportedLanguages = supportedLanguages,
        lastUpdated = searchableConcept.lastUpdated,
        created = searchableConcept.created,
        status = toApiStatus(searchableConcept.status),
        updatedBy = searchableConcept.updatedBy,
        license = searchableConcept.license,
        copyright = copyright,
        visualElement = visualElement,
        source = searchableConcept.source,
        responsible = responsible,
        conceptType = searchableConcept.conceptType,
        glossData = glossData,
        conceptTypeName = conceptTypeName
      )
    }

    /** Attempts to extract language that hit from highlights in elasticsearch response.
      *
      * @param result
      *   Elasticsearch hit.
      * @return
      *   Language if found.
      */
    def getLanguageFromHit(result: SearchHit): Option[String] = {
      def keyToLanguage(keys: Iterable[String]): Option[String] = {
        val keyLanguages = keys.toList.flatMap(key =>
          key.split('.').toList match {
            case _ :: language :: _ => Some(language)
            case _                  => None
          }
        )

        keyLanguages
          .sortBy(lang => {
            ISO639.languagePriority.reverse.indexOf(lang)
          })
          .lastOption
      }

      val highlightKeys: Option[Map[String, ?]] = Option(result.highlight)
      val matchLanguage                         = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    def asApiConceptSearchResult(searchResult: SearchResult[api.ConceptSummaryDTO]): ConceptSearchResultDTO =
      api.ConceptSearchResultDTO(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results,
        searchResult.aggregations.map(toApiMultiTermsAggregation)
      )

    def toApiStatus(status: Status): api.StatusDTO = {
      api.StatusDTO(
        current = status.current,
        other = status.other
      )
    }
  }
}
