/*
 * Part of NDLA image-api.
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import cats.implicits._
import no.ndla.common.model.api.CommaSeparatedList._
import no.ndla.imageapi.controller.multipart.{MetaDataAndFileForm, UpdateMetaDataAndFileForm}
import no.ndla.imageapi.model.api._
import no.ndla.imageapi.model.domain.{ModelReleasedStatus, SearchSettings, Sort}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageSearchService, SearchConverterService}
import no.ndla.imageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.imageapi.{Eff, Props}
import no.ndla.language.Language
import no.ndla.network.tapir.NoNullJsonPrinter._
import no.ndla.network.tapir.TapirErrors.errorOutputsFor
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import no.ndla.network.tapir.{DynamicHeaders, Service}
import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.server.ServerEndpoint

import scala.util.{Failure, Success, Try}

trait ImageControllerV3 {
  this: ImageRepository
    with ImageSearchService
    with ConverterService
    with ReadService
    with WriteService
    with SearchConverterService
    with Props
    with ErrorHelpers
    with BaseImageController =>
  val imageControllerV3: ImageControllerV3

  class ImageControllerV3 extends Service[Eff] with BaseImageController {
    import ErrorHelpers._
    import props._

    override val serviceName: String         = "images V3"
    override val prefix: EndpointInput[Unit] = "image-api" / "v3" / "images"

    /** Does a scroll with [[ImageSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(
        scrollId: Option[String],
        language: String,
        user: Option[TokenUser]
    )(orFunction: => Try[(SearchResultV3, DynamicHeaders)]): Try[(SearchResultV3, DynamicHeaders)] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          for {
            scrollResult <- imageSearchService.scroll(scroll, language)
            body         <- searchConverterService.asApiSearchResultV3(scrollResult, language, user)
            headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
          } yield (body, headers)
        case _ => orFunction
      }

    private def searchV3(
        minimumSize: Option[Int],
        query: Option[String],
        language: String,
        fallback: Boolean,
        license: Option[String],
        sort: Option[Sort],
        pageSize: Option[Int],
        page: Option[Int],
        podcastFriendly: Option[Boolean],
        includeCopyrighted: Boolean,
        shouldScroll: Boolean,
        modelReleasedStatus: Seq[ModelReleasedStatus.Value],
        user: Option[TokenUser]
    ) = {
      val settings = query match {
        case Some(searchString) =>
          SearchSettings(
            query = Some(searchString.trim),
            minimumSize = minimumSize,
            language = language,
            fallback = fallback,
            license = license,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            page = page,
            pageSize = pageSize,
            podcastFriendly = podcastFriendly,
            includeCopyrighted = includeCopyrighted,
            shouldScroll = shouldScroll,
            modelReleased = modelReleasedStatus
          )
        case None =>
          SearchSettings(
            query = None,
            minimumSize = minimumSize,
            license = license,
            language = language,
            fallback = fallback,
            sort = sort.getOrElse(Sort.ByTitleAsc),
            page = page,
            pageSize = pageSize,
            podcastFriendly = podcastFriendly,
            includeCopyrighted = includeCopyrighted,
            shouldScroll = shouldScroll,
            modelReleased = modelReleasedStatus
          )
      }
      for {
        searchResult <- imageSearchService.matchingQueryV3(settings, user)
        output       <- searchConverterService.asApiSearchResultV3(searchResult, language, user)
        scrollHeader = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
      } yield (output, scrollHeader)
    }

    def getImagesV3: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Find images.")
      .description("Find images in the ndla.no database.")
      .in(queryParam)
      .in(minSize)
      .in(language)
      .in(fallback)
      .in(license)
      .in(includeCopyrighted)
      .in(sort)
      .in(pageNo)
      .in(pageSize)
      .in(podcastFriendly)
      .in(scrollId)
      .in(modelReleased)
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[SearchResultV3])
      .out(EndpointOutput.derived[DynamicHeaders])
      .withOptionalUser
      .serverLogicPure { user =>
        {
          case (
                query,
                minimumSize,
                language,
                fallback,
                license,
                includeCopyrighted,
                sortStr,
                pageNo,
                pageSize,
                podcastFriendly,
                scrollId,
                modelReleased
              ) =>
            scrollSearchOr(scrollId, language, user) {
              val sort                = Sort.valueOf(sortStr)
              val shouldScroll        = scrollId.exists(InitialScrollContextKeywords.contains)
              val modelReleasedStatus = modelReleased.values.flatMap(ModelReleasedStatus.valueOf)

              searchV3(
                minimumSize,
                query,
                language,
                fallback,
                license,
                sort,
                pageSize,
                pageNo,
                podcastFriendly,
                includeCopyrighted,
                shouldScroll,
                modelReleasedStatus,
                user
              )
            }.handleErrorsOrOk
        }
      }

    def getImagesPostV3: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find images.")
      .description("Search for images in the ndla.no database.")
      .in("search")
      .in(jsonBody[SearchParams])
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[SearchResultV3])
      .out(EndpointOutput.derived[DynamicHeaders])
      .withOptionalUser
      .serverLogicPure {
        user =>
          { searchParams =>
            val language = searchParams.language.getOrElse(Language.AllLanguages)
            val fallback = searchParams.fallback.getOrElse(false)

            scrollSearchOr(searchParams.scrollId, language, user) {
              val minimumSize        = searchParams.minimumSize
              val query              = searchParams.query
              val license            = searchParams.license
              val pageSize           = searchParams.pageSize
              val page               = searchParams.page
              val podcastFriendly    = searchParams.podcastFriendly
              val sort               = Sort.valueOf(searchParams.sort)
              val includeCopyrighted = searchParams.includeCopyrighted.getOrElse(false)
              val shouldScroll       = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
              val modelReleasedStatus =
                searchParams.modelReleased.getOrElse(Seq.empty).flatMap(ModelReleasedStatus.valueOf)

              searchV3(
                minimumSize,
                query,
                language,
                fallback,
                license,
                sort,
                pageSize,
                page,
                podcastFriendly,
                includeCopyrighted,
                shouldScroll,
                modelReleasedStatus,
                user
              )
            }.handleErrorsOrOk
          }
      }

    def findByImageIdV3: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch information for image.")
      .description("Shows info of the image with submitted id.")
      .in(pathImageId)
      .in(languageOpt)
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[ImageMetaInformationV3])
      .withOptionalUser
      .serverLogicPure { user =>
        { case (imageId, language) =>
          readService.withIdV3(imageId, language, user) match {
            case Success(Some(image)) => image.asRight
            case Success(None) => notFoundWithMsg(s"Image with id $imageId and language $language not found").asLeft
            case Failure(ex)   => returnLeftError(ex)
          }
        }
      }

    def getImagesByIds: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch images that matches ids parameter.")
      .description("Fetch images that matches ids parameter.")
      .in("ids")
      .in(imageIds)
      .in(languageOpt)
      .out(jsonBody[List[ImageMetaInformationV3]])
      .errorOut(errorOutputsFor(400))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (imageIds, language) =>
          readService.getImagesByIdsV3(imageIds.values, language, user).handleErrorsOrOk
        }
      }

    def findImageByExternalIdV3: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch information for image by external id.")
      .description("Shows info of the image with submitted external id.")
      .in("external_id" / pathExternalId)
      .in(languageOpt)
      .out(jsonBody[ImageMetaInformationV3])
      .errorOut(errorOutputsFor(400))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (externalId, language) =>
          imageRepository.withExternalId(externalId) match {
            case Some(image) => converterService.asApiImageMetaInformationV3(image, language, user).handleErrorsOrOk
            case None        => notFoundWithMsg(s"Image with external id $externalId not found").asLeft
          }
        }
      }

    def newImageV3: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Upload a new image with meta information.")
      .description("Upload a new image file with meta data.")
      .out(jsonBody[ImageMetaInformationV3])
      .in(multipartBody[MetaDataAndFileForm])
      .errorOut(errorOutputsFor(400))
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure(user =>
        formData =>
          doWithStream(formData.file) { uploadedFile =>
            writeService.storeNewImage(formData.metadata.body, uploadedFile, user).map { case storedImage =>
              converterService
                .asApiImageMetaInformationV3(
                  storedImage,
                  Some(formData.metadata.body.language),
                  Some(user)
                )
            }
          }.flatten.handleErrorsOrOk
      )

    def deleteImageV3: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Deletes the specified images meta data and file")
      .description("Deletes the specified images meta data and file")
      .in(pathImageId)
      .out(emptyOutput)
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure { _ => imageId =>
        writeService.deleteImageAndFiles(imageId) match {
          case Failure(ex) => returnLeftError(ex)
          case Success(_)  => ().asRight
        }
      }

    def deleteLanguageV3: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete language version of image metadata.")
      .description("Delete language version of image metadata.")
      .in(pathImageId / "language" / pathLanguage)
      .out(noContentOrBodyOutput[ImageMetaInformationV3])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure(user => { case (imageId, language) =>
        writeService.deleteImageLanguageVersionV3(imageId, language, user) match {
          case Failure(ex)          => returnLeftError(ex)
          case Success(Some(image)) => Some(image).asRight
          case Success(None)        => None.asRight
        }
      })

    def editImageV3: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update an existing image with meta information.")
      .description("Updates an existing image with meta data.")
      .in(pathImageId)
      .out(jsonBody[ImageMetaInformationV3])
      .in(multipartBody[UpdateMetaDataAndFileForm])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure { user => input =>
        val (imageId, formData) = input
        doWithMaybeStream(formData.file) { uploadedFile =>
          writeService.updateImageV3(imageId, formData.metadata.body, uploadedFile, user)
        }.handleErrorsOrOk
      }

    def getTagsSearchableV3: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Retrieves a list of all previously used tags in images")
      .description("Retrieves a list of all previously used tags in images")
      .in("tag-search")
      .in(queryParam)
      .in(pageSize)
      .in(pageNo)
      .in(language)
      .in(sort)
      .out(jsonBody[TagsSearchResult])
      .errorOut(errorOutputsFor(400, 401, 403))
      .serverLogicPure { case (q, pageSizeParam, pageNoParam, language, sortStr) =>
        val query = q.getOrElse("")
        val pageSize = pageSizeParam.getOrElse(props.DefaultPageSize) match {
          case tooSmall if tooSmall < 1 => props.DefaultPageSize
          case x                        => x
        }
        val pageNo = pageNoParam.getOrElse(1) match {
          case tooSmall if tooSmall < 1 => 1
          case x                        => x
        }
        val sort = Sort.valueOf(sortStr).getOrElse(Sort.ByRelevanceDesc)

        readService.getAllTags(query, pageSize, pageNo, language, sort).handleErrorsOrOk
      }

    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getImagesV3,
      getImagesPostV3,
      findByImageIdV3,
      getImagesByIds,
      findImageByExternalIdV3,
      newImageV3,
      deleteImageV3,
      deleteLanguageV3,
      editImageV3,
      getTagsSearchableV3
    )

  }
}
