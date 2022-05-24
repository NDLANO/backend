/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import io.lemonlabs.uri.Uri.parse
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.auth.Role
import no.ndla.imageapi.model.api.{ImageAltText, ImageCaption, ImageMetaSummary, ImageTitle}
import no.ndla.imageapi.model.domain.{ImageMetaInformation, SearchResult}
import no.ndla.imageapi.model.api
import no.ndla.imageapi.model.domain
import no.ndla.imageapi.model.search.{SearchableImage, SearchableTag}
import no.ndla.imageapi.service.ConverterService
import no.ndla.language.Language
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl
import no.ndla.search.SearchLanguage
import no.ndla.search.model.{LanguageValue, SearchableLanguageList, SearchableLanguageValues}

trait SearchConverterService {
  this: ConverterService with Role with Props =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends LazyLogging {

    def asSearchableTags(domainModel: ImageMetaInformation): Seq[SearchableTag] =
      domainModel.tags.flatMap(tags =>
        tags.tags.map(tag =>
          SearchableTag(
            tag = tag,
            language = tags.language
          )
        )
      )

    def asSearchableImage(image: ImageMetaInformation): SearchableImage = {
      val imageWithAgreement = converterService.withAgreementCopyright(image)

      val defaultTitle = imageWithAgreement.titles
        .sortBy(title => {
          val languagePriority = SearchLanguage.languageAnalyzers.map(la => la.languageTag.toString()).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      SearchableImage(
        id = imageWithAgreement.id.get,
        titles =
          SearchableLanguageValues(imageWithAgreement.titles.map(title => LanguageValue(title.language, title.title))),
        alttexts = SearchableLanguageValues(
          imageWithAgreement.alttexts.map(alttext => LanguageValue(alttext.language, alttext.alttext))
        ),
        captions = SearchableLanguageValues(
          imageWithAgreement.captions.map(caption => LanguageValue(caption.language, caption.caption))
        ),
        tags = SearchableLanguageList(imageWithAgreement.tags.map(tag => LanguageValue(tag.language, tag.tags))),
        contributors = image.copyright.creators.map(c => c.name) ++ image.copyright.processors
          .map(p => p.name) ++ image.copyright.rightsholders.map(r => r.name),
        license = imageWithAgreement.copyright.license,
        imageSize = imageWithAgreement.size,
        previewUrl = parse("/" + imageWithAgreement.imageUrl.dropWhile(_ == '/')).toString,
        lastUpdated = imageWithAgreement.updated,
        defaultTitle = defaultTitle.map(t => t.title),
        modelReleased = Some(image.modelReleased.toString),
        editorNotes = image.editorNotes.map(_.note),
        fileSize = image.size,
        contentType = image.contentType,
        imageDimensions = image.imageDimensions
      )
    }

    def asImageMetaSummary(searchableImage: SearchableImage, language: String): ImageMetaSummary = {
      val apiToRawRegex = "/v\\d+/images/".r
      val title = Language
        .findByLanguageOrBestEffort(searchableImage.titles.languageValues, Some(language))
        .map(res => ImageTitle(res.value, res.language))
        .getOrElse(ImageTitle("", props.DefaultLanguage))
      val altText = Language
        .findByLanguageOrBestEffort(searchableImage.alttexts.languageValues, Some(language))
        .map(res => ImageAltText(res.value, res.language))
        .getOrElse(ImageAltText("", props.DefaultLanguage))
      val caption = Language
        .findByLanguageOrBestEffort(searchableImage.captions.languageValues, Some(language))
        .map(res => ImageCaption(res.value, res.language))
        .getOrElse(ImageCaption("", props.DefaultLanguage))

      val supportedLanguages = Language.getSupportedLanguages(
        searchableImage.titles.languageValues,
        searchableImage.alttexts.languageValues,
        searchableImage.captions.languageValues,
        searchableImage.tags.languageValues
      )

      val editorNotes = Option.when(authRole.userHasWriteRole())(searchableImage.editorNotes)

      ImageMetaSummary(
        id = searchableImage.id.toString,
        title = title,
        contributors = searchableImage.contributors,
        altText = altText,
        caption = caption,
        previewUrl = apiToRawRegex.replaceFirstIn(ApplicationUrl.get, "/raw") + searchableImage.previewUrl,
        metaUrl = ApplicationUrl.get + searchableImage.id,
        license = searchableImage.license,
        supportedLanguages = supportedLanguages,
        modelRelease = searchableImage.modelReleased,
        editorNotes = editorNotes,
        lastUpdated = searchableImage.lastUpdated,
        fileSize = searchableImage.fileSize,
        contentType = searchableImage.contentType,
        imageDimensions = searchableImage.imageDimensions.map { case domain.ImageDimensions(width, height) =>
          api.ImageDimensions(width, height)
        }
      )
    }

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

    def asApiSearchResult(searchResult: domain.SearchResult[ImageMetaSummary]): api.SearchResult =
      api.SearchResult(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def tagSearchResultAsApiResult(searchResult: SearchResult[String]): api.TagsSearchResult =
      api.TagsSearchResult(
        searchResult.totalCount,
        searchResult.page.getOrElse(1),
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )
  }

}
