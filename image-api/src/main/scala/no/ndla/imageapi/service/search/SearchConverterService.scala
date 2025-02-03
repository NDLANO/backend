/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 */

package no.ndla.imageapi.service.search

import io.lemonlabs.uri.Uri.parse
import com.sksamuel.elastic4s.requests.searches.SearchHit
import com.typesafe.scalalogging.StrictLogging
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.api.{ImageAltTextDTO, ImageCaptionDTO, ImageMetaSummaryDTO, ImageTitleDTO}
import no.ndla.imageapi.model.domain.{ImageFileData, ImageMetaInformation, SearchResult}
import no.ndla.imageapi.model.{ImageConversionException, api, domain}
import no.ndla.imageapi.model.search.{SearchableImage, SearchableImageFile, SearchableTag}
import no.ndla.imageapi.service.ConverterService
import no.ndla.language.Language
import no.ndla.language.Language.findByLanguageOrBestEffort
import no.ndla.mapping.ISO639
import no.ndla.network.ApplicationUrl
import no.ndla.search.SearchLanguage
import no.ndla.search.model.{SearchableLanguageList, SearchableLanguageValues}
import cats.implicits._
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser

import scala.util.{Failure, Success, Try}

trait SearchConverterService {
  this: ConverterService with Props =>
  val searchConverterService: SearchConverterService

  class SearchConverterService extends StrictLogging {

    def asSearchableTags(domainModel: ImageMetaInformation): Seq[SearchableTag] =
      domainModel.tags.flatMap(tags =>
        tags.tags.map(tag =>
          SearchableTag(
            tag = tag,
            language = tags.language
          )
        )
      )

    private def asSearchableImageFiles(images: Seq[ImageFileData]): Seq[SearchableImageFile] = {
      images.map(i => {
        SearchableImageFile(
          imageSize = i.size,
          previewUrl = parse("/" + i.fileName.dropWhile(_ == '/')).toString,
          fileSize = i.size,
          contentType = i.contentType,
          dimensions = i.dimensions,
          language = i.language
        )
      })
    }

    def asSearchableImage(image: ImageMetaInformation): SearchableImage = {
      val defaultTitle = image.titles
        .sortBy(title => {
          val languagePriority = SearchLanguage.languageAnalyzers.map(la => la.languageTag.toString()).reverse
          languagePriority.indexOf(title.language)
        })
        .lastOption

      val contributors =
        image.copyright.creators.map(c => c.name) ++
          image.copyright.processors.map(p => p.name) ++
          image.copyright.rightsholders.map(r => r.name)

      val podcastFriendly = image.images
        .getOrElse(Seq.empty)
        .exists(i => i.dimensions.exists(d => (d.height == d.width) && d.width <= 3000 && d.width >= 1400))

      val users =
        image.editorNotes.map(_.updatedBy) :+ image.updatedBy :+ image.createdBy

      SearchableImage(
        id = image.id.get,
        titles = SearchableLanguageValues.fromFields(image.titles),
        alttexts = SearchableLanguageValues.fromFields(image.alttexts),
        captions = SearchableLanguageValues.fromFields(image.captions),
        tags = SearchableLanguageList.fromFields(image.tags),
        contributors = contributors,
        license = image.copyright.license,
        lastUpdated = image.updated,
        defaultTitle = defaultTitle.map(t => t.title),
        modelReleased = Some(image.modelReleased.toString),
        editorNotes = image.editorNotes.map(_.note),
        imageFiles = asSearchableImageFiles(image.images.getOrElse(Seq.empty)),
        podcastFriendly = podcastFriendly,
        domainObject = image,
        users = users
      )
    }

    private def getSearchableImageFileFromSearchableImage(
        meta: SearchableImage,
        language: Option[String]
    ): Try[SearchableImageFile] = {
      findByLanguageOrBestEffort(meta.imageFiles, language) match {
        case None        => Failure(ImageConversionException("Could not find image in meta, this is a bug."))
        case Some(image) => Success(image)
      }
    }

    def asImageMetaSummary(
        searchableImage: SearchableImage,
        language: String,
        user: Option[TokenUser]
    ): Try[ImageMetaSummaryDTO] = {
      val apiToRawRegex = "/v\\d+/images/".r
      val title = Language
        .findByLanguageOrBestEffort(searchableImage.titles.languageValues, Some(language))
        .map(res => ImageTitleDTO(res.value, res.language))
        .getOrElse(ImageTitleDTO("", props.DefaultLanguage))
      val altText = Language
        .findByLanguageOrBestEffort(searchableImage.alttexts.languageValues, Some(language))
        .map(res => ImageAltTextDTO(res.value, res.language))
        .getOrElse(ImageAltTextDTO("", props.DefaultLanguage))
      val caption = Language
        .findByLanguageOrBestEffort(searchableImage.captions.languageValues, Some(language))
        .map(res => ImageCaptionDTO(res.value, res.language))
        .getOrElse(ImageCaptionDTO("", props.DefaultLanguage))

      val supportedLanguages = Language.getSupportedLanguages(
        searchableImage.titles.languageValues,
        searchableImage.alttexts.languageValues,
        searchableImage.captions.languageValues,
        searchableImage.tags.languageValues
      )

      val editorNotes = Option.when(user.hasPermission(IMAGE_API_WRITE))(searchableImage.editorNotes)

      getSearchableImageFileFromSearchableImage(searchableImage, language.some).map(imageFile => {
        ImageMetaSummaryDTO(
          id = searchableImage.id.toString,
          title = title,
          contributors = searchableImage.contributors,
          altText = altText,
          caption = caption,
          previewUrl = apiToRawRegex.replaceFirstIn(ApplicationUrl.get, "/raw") + imageFile.previewUrl,
          metaUrl = ApplicationUrl.get + searchableImage.id,
          license = searchableImage.license,
          supportedLanguages = supportedLanguages,
          modelRelease = searchableImage.modelReleased,
          editorNotes = editorNotes,
          lastUpdated = searchableImage.lastUpdated,
          fileSize = imageFile.fileSize,
          contentType = imageFile.contentType,
          imageDimensions = imageFile.dimensions.map { case domain.ImageDimensions(width, height) =>
            api.ImageDimensionsDTO(width, height)
          }
        )
      })
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

    def asApiSearchResult(searchResult: domain.SearchResult[ImageMetaSummaryDTO]): api.SearchResultDTO =
      api.SearchResultDTO(
        searchResult.totalCount,
        searchResult.page,
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def tagSearchResultAsApiResult(searchResult: SearchResult[String]): api.TagsSearchResultDTO =
      api.TagsSearchResultDTO(
        searchResult.totalCount,
        searchResult.page.getOrElse(1),
        searchResult.pageSize,
        searchResult.language,
        searchResult.results
      )

    def asApiSearchResultV3(
        searchResult: domain.SearchResult[(SearchableImage, MatchedLanguage)],
        language: String,
        user: Option[TokenUser]
    ): Try[api.SearchResultV3DTO] = {
      searchResult.results
        .traverse(r => converterService.asApiImageMetaInformationV3(r._1.domainObject, language.some, user))
        .map(results =>
          api.SearchResultV3DTO(
            searchResult.totalCount,
            searchResult.page,
            searchResult.pageSize,
            searchResult.language,
            results
          )
        )

    }
  }

}
