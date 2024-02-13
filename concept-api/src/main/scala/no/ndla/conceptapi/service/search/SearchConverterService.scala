/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.draft.DraftCopyright
import no.ndla.common.model.domain.{Tag, Title}
import no.ndla.common.model.{api => commonApi}
import no.ndla.conceptapi.integration.model.TaxonomyData
import no.ndla.conceptapi.model.api.{ConceptResponsible, ConceptSearchResult, SubjectTags}
import no.ndla.conceptapi.model.domain.{Concept, ConceptType, DBConcept, SearchResult}
import no.ndla.conceptapi.model.search._
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.service.ConverterService
import no.ndla.language.Language.{UnknownLanguage, findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.mapping.ISO639
import no.ndla.search.AggregationBuilder.toApiMultiTermsAggregation
import no.ndla.search.SearchConverter.getEmbedValues
import no.ndla.search.model.domain.EmbedValues
import no.ndla.search.model.{LanguageValue, SearchableLanguageFormats, SearchableLanguageList, SearchableLanguageValues}
import org.json4s._
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup

trait SearchConverterService {
  this: ConverterService with DBConcept =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {
    private def getEmbedResourcesAndIdsToIndex(
        visualElement: Seq[domain.VisualElement],
        metaImage: Seq[domain.ConceptMetaImage]
    ): List[EmbedValues] = {
      val visualElementTuples = visualElement.flatMap(v => getEmbedValues(v.visualElement, v.language))
      val metaImageTuples =
        metaImage.map(m => EmbedValues(id = List(m.imageId), resource = Some("image"), language = m.language))
      (visualElementTuples ++ metaImageTuples).toList

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

    def asSearchableConcept(c: Concept, taxonomyData: TaxonomyData): SearchableConcept = {
      val title = SearchableLanguageValues(c.title.map(title => LanguageValue(title.language, title.title)))
      val content = SearchableLanguageValues(
        c.content.map(content => LanguageValue(content.language, Jsoup.parseBodyFragment(content.content).text()))
      )
      val tags = SearchableLanguageList(c.tags.map(tag => LanguageValue(tag.language, tag.tags)))
      val visualElement = SearchableLanguageValues(
        c.visualElement.map(element => LanguageValue(element.language, element.visualElement))
      )

      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(c.visualElement, c.metaImage)
      val copyright            = asSearchableCopyright(c.copyright)

      val allConnectedSubjects = c.subjectIds.flatMap(subjectId => {
        taxonomyData.subjectsById
          .get(subjectId)
          .map(subject => {
            val translations = subject.translations.map(_.toLanguageValue)
            SearchableLanguageValues(translations)
          })
      })

      val sortableSubject = SearchableLanguageValues.combine(allConnectedSubjects.toSeq)

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
        conceptType = c.conceptType.toString,
        title = title,
        content = content,
        defaultTitle = title.defaultValue,
        metaImage = c.metaImage,
        tags = tags,
        subjectIds = c.subjectIds.toSeq,
        lastUpdated = c.updated,
        status = Status(c.status.current.toString, c.status.other.map(_.toString).toSeq),
        updatedBy = c.updatedBy,
        license = c.copyright.flatMap(_.license),
        copyright = copyright,
        embedResourcesAndIds = embedResourcesAndIds,
        visualElement = visualElement,
        articleIds = c.articleIds,
        created = c.created,
        source = c.copyright.flatMap(_.origin),
        responsible = c.responsible,
        gloss = c.glossData.map(_.gloss),
        domainObject = c,
        sortableSubject = sortableSubject,
        sortableConceptType = sortableConceptType,
        defaultSortableSubject = sortableSubject.defaultValue,
        defaultSortableConceptType = sortableConceptType.defaultValue
      )
    }

    def hitAsConceptSummary(hitString: String, language: String): api.ConceptSummary = {
      implicit val formats: Formats = SearchableLanguageFormats.JSonFormats ++ Concept.serializers
      val searchableConcept         = read[SearchableConcept](hitString)
      val titles                    = searchableConcept.title.languageValues.map(lv => Title(lv.value, lv.language))
      val contents = searchableConcept.content.languageValues.map(lv => domain.ConceptContent(lv.value, lv.language))
      val tags     = searchableConcept.tags.languageValues.map(lv => Tag(lv.value, lv.language))
      val visualElements =
        searchableConcept.visualElement.languageValues.map(lv => domain.VisualElement(lv.value, lv.language))

      val supportedLanguages = getSupportedLanguages(titles, contents)

      val title = findByLanguageOrBestEffort(titles, language)
        .map(converterService.toApiConceptTitle)
        .getOrElse(api.ConceptTitle("", UnknownLanguage.toString()))
      val content = findByLanguageOrBestEffort(contents, language)
        .map(converterService.toApiConceptContent)
        .getOrElse(api.ConceptContent("", "", UnknownLanguage.toString()))
      val metaImage = findByLanguageOrBestEffort(searchableConcept.metaImage, language)
        .map(converterService.toApiMetaImage)
        .getOrElse(api.ConceptMetaImage("", "", UnknownLanguage.toString()))
      val tag = findByLanguageOrBestEffort(tags, language).map(converterService.toApiTags)
      val visualElement =
        findByLanguageOrBestEffort(visualElements, language).map(converterService.toApiVisualElement)
      val subjectIds = Option(searchableConcept.subjectIds.toSet).filter(_.nonEmpty)
      val license    = converterService.toApiLicense(searchableConcept.license)
      val copyright = searchableConcept.copyright.map(c => {
        commonApi.DraftCopyright(
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

      val responsible = searchableConcept.responsible.map(r => ConceptResponsible(r.responsibleId, r.lastUpdated))
      val glossData   = converterService.toApiGlossData(searchableConcept.domainObject.glossData)
      val subjectName = searchableConcept.sortableSubject.getLanguageOrDefault(language)
      val conceptTypeName = searchableConcept.sortableConceptType
        .getLanguageOrDefault(language)
        .getOrElse(searchableConcept.conceptType)

      api.ConceptSummary(
        id = searchableConcept.id,
        title = title,
        content = content,
        metaImage = metaImage,
        tags = tag,
        subjectIds = subjectIds,
        supportedLanguages = supportedLanguages,
        lastUpdated = searchableConcept.lastUpdated,
        status = toApiStatus(searchableConcept.status),
        updatedBy = searchableConcept.updatedBy,
        license = searchableConcept.license,
        copyright = copyright,
        visualElement = visualElement,
        articleIds = searchableConcept.articleIds,
        created = searchableConcept.created,
        source = searchableConcept.source,
        responsible = responsible,
        conceptType = searchableConcept.conceptType,
        glossData = glossData,
        subjectName = subjectName,
        conceptTypeName = conceptTypeName
      )
    }

    def groupSubjectTagsByLanguage(subjectId: String, tags: List[api.ConceptTags]): List[SubjectTags] =
      tags
        .groupBy(_.language)
        .map { case (lang, conceptTags) =>
          val tagsForLang = conceptTags.flatMap(_.tags).distinct
          api.SubjectTags(subjectId, tagsForLang, lang)
        }
        .toList

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

      val highlightKeys: Option[Map[String, _]] = Option(result.highlight)
      val matchLanguage                         = keyToLanguage(highlightKeys.getOrElse(Map()).keys)

      matchLanguage match {
        case Some(lang) =>
          Some(lang)
        case _ =>
          keyToLanguage(result.sourceAsMap.keys)
      }
    }

    def asApiConceptSearchResult(searchResult: SearchResult[api.ConceptSummary]): ConceptSearchResult =
      api.ConceptSearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results,
        searchResult.aggregations.map(toApiMultiTermsAggregation)
      )

    def toApiStatus(status: Status): api.Status = {
      api.Status(
        current = status.current,
        other = status.other
      )
    }
  }
}
