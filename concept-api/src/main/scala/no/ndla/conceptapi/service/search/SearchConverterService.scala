/*
 * Part of NDLA concept-api.
 * Copyright (C) 2019 NDLA
 *
 * See LICENSE
 */

package no.ndla.conceptapi.service.search

import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.draft.Copyright
import no.ndla.common.configuration.Constants.EmbedTagName
import no.ndla.common.model.domain.{Tag, Title}
import no.ndla.conceptapi.model.api.{ConceptResponsible, ConceptSearchResult, SubjectTags}
import no.ndla.conceptapi.model.domain.{Concept, SearchResult}
import no.ndla.conceptapi.model.search._
import no.ndla.conceptapi.model.{api, domain}
import no.ndla.conceptapi.service.ConverterService
import no.ndla.language.Language.{UnknownLanguage, findByLanguageOrBestEffort, getSupportedLanguages}
import no.ndla.mapping.ISO639
import no.ndla.search.SearchLanguage
import no.ndla.search.model.{LanguageValue, SearchableLanguageFormats, SearchableLanguageList, SearchableLanguageValues}
import org.json4s._
import org.json4s.native.Serialization.read
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities.EscapeMode

import scala.jdk.CollectionConverters._

trait SearchConverterService {
  this: ConverterService =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {
    implicit val formats: Formats = SearchableLanguageFormats.JSonFormats

    private def parseHtml(html: String) = {
      val document = Jsoup.parseBodyFragment(html)
      document.outputSettings().escapeMode(EscapeMode.xhtml).prettyPrint(false)
      document.body()
    }

    // To be removed
    private[service] def getEmbedResources(html: String): List[String] = {
      parseHtml(html)
        .select(EmbedTagName)
        .asScala
        .flatMap(getEmbedResources)
        .toList
    }

    // To be removed
    private def getEmbedResources(embed: Element): List[String] = {
      val attributesToKeep = List(
        "data-resource"
      )

      attributesToKeep.flatMap(attr =>
        embed.attr(attr) match {
          case "" => None
          case a  => Some(a)
        }
      )
    }

    // To be removed
    private[service] def getEmbedIds(html: String): List[String] = {
      parseHtml(html)
        .select(EmbedTagName)
        .asScala
        .flatMap(getEmbedIds)
        .toList
    }

    private def getEmbedResource(embed: Element): Option[String] = {

      embed.attr("data-resource") match {
        case "" => None
        case a  => Some(a)
      }
    }

    private def getEmbedIds(embed: Element): List[String] = {
      val attributesToKeep = List(
        "data-videoid",
        "data-url",
        "data-resource_id",
        "data-content-id"
      )

      attributesToKeep
        .flatMap(attr =>
          embed.attr(attr) match {
            case "" => None
            case a  => Some(a)
          }
        )
    }

    private def getEmbedValuesFromEmbed(embed: Element, language: String): EmbedValues = {
      EmbedValues(resource = getEmbedResource(embed), id = getEmbedIds(embed), language = language)
    }

    private[service] def getEmbedValues(html: String, language: String): List[EmbedValues] = {
      parseHtml(html)
        .select(EmbedTagName)
        .asScala
        .flatMap(embed => Some(getEmbedValuesFromEmbed(embed, language)))
        .toList
    }

    private def getEmbedResourcesAndIdsToIndex(
        visualElement: Seq[domain.VisualElement],
        metaImage: Seq[domain.ConceptMetaImage]
    ): List[EmbedValues] = {
      val visualElementTuples = visualElement.flatMap(v => getEmbedValues(v.visualElement, v.language))
      val metaImageTuples =
        metaImage.map(m => EmbedValues(id = List(m.imageId), resource = Some("image"), language = m.language))
      (visualElementTuples ++ metaImageTuples).toList

    }

    def asSearchableCopyright(maybeCopyright: Option[Copyright]): Option[SearchableCopyright] = {
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
      val defaultTitle = c.title
        .sortBy(title => {
          val languagePriority = SearchLanguage.languageAnalyzers.map(la => la.languageTag.toString()).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption
      val embedResourcesAndIds = getEmbedResourcesAndIdsToIndex(c.visualElement, c.metaImage)
      val copyright            = asSearchableCopyright(c.copyright);

      SearchableConcept(
        id = c.id.get,
        conceptType = c.conceptType.toString,
        title = SearchableLanguageValues(c.title.map(title => LanguageValue(title.language, title.title))),
        content = SearchableLanguageValues(c.content.map(content => LanguageValue(content.language, content.content))),
        defaultTitle = defaultTitle.map(_.title),
        metaImage = c.metaImage,
        tags = SearchableLanguageList(c.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        subjectIds = c.subjectIds.toSeq,
        lastUpdated = c.updated,
        status = Status(c.status.current.toString, c.status.other.map(_.toString).toSeq),
        updatedBy = c.updatedBy,
        license = c.copyright.flatMap(_.license),
        copyright = copyright,
        embedResourcesAndIds = embedResourcesAndIds,
        visualElement = SearchableLanguageValues(
          c.visualElement.map(element => LanguageValue(element.language, element.visualElement))
        ),
        articleIds = c.articleIds,
        created = c.created,
        source = c.source,
        responsible = c.responsible
      )
    }

    def hitAsConceptSummary(hitString: String, language: String): api.ConceptSummary = {

      val searchableConcept = read[SearchableConcept](hitString)
      val titles            = searchableConcept.title.languageValues.map(lv => Title(lv.value, lv.language))
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
        .getOrElse(api.ConceptContent("", UnknownLanguage.toString()))
      val metaImage = findByLanguageOrBestEffort(searchableConcept.metaImage, language)
        .map(converterService.toApiMetaImage)
        .getOrElse(api.ConceptMetaImage("", "", UnknownLanguage.toString()))
      val tag = findByLanguageOrBestEffort(tags, language).map(converterService.toApiTags)
      val visualElement =
        findByLanguageOrBestEffort(visualElements, language).map(converterService.toApiVisualElement)
      val subjectIds = Option(searchableConcept.subjectIds.toSet).filter(_.nonEmpty)
      val license    = searchableConcept.license.map(converterService.toApiLicense)
      val copyright = searchableConcept.copyright.map(c => {
        api.Copyright(
          license = license,
          origin = c.origin,
          creators = c.creators.map(converterService.toApiAuthor),
          processors = c.processors.map(converterService.toApiAuthor),
          rightsholders = c.rightsholders.map(converterService.toApiAuthor),
          agreementId = None,
          validFrom = None,
          validTo = None
        )
      })

      val responsible = searchableConcept.responsible.map(r => ConceptResponsible(r.responsibleId, r.lastUpdated))

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
        conceptType = searchableConcept.conceptType
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
        searchResult.results
      )

    def toApiStatus(status: Status): api.Status = {
      api.Status(
        current = status.current,
        other = status.other
      )
    }
  }
}
