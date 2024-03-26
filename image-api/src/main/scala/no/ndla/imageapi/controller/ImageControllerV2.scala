/*
 * Part of NDLA image-api
 * Copyright (C) 2016 NDLA
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

trait ImageControllerV2 {
  this: ImageRepository
    with ImageSearchService
    with ConverterService
    with ReadService
    with WriteService
    with SearchConverterService
    with Props
    with ErrorHelpers
    with BaseImageController =>
  val imageControllerV2: ImageControllerV2

  class ImageControllerV2 extends Service[Eff] with BaseImageController {
    import ErrorHelpers._
    import props._

    override val serviceName: String         = "images V2"
    override val prefix: EndpointInput[Unit] = "image-api" / "v2" / "images"
    override val endpoints: List[ServerEndpoint[Any, Eff]] = List(
      getImages,
      getTagsSearchable,
      getImagesPost,
      findByImageId,
      findImageByExternalId,
      postNewImage,
      deleteImage,
      deleteLanguage,
      editImage
    )

    /** Does a scroll with [[ImageSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    protected def scrollSearchOr(scrollId: Option[String], language: String, user: Option[TokenUser])(
        orFunction: => Try[(SearchResult, DynamicHeaders)]
    ): Try[(SearchResult, DynamicHeaders)] =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          imageSearchService.scrollV2(scroll, language, user) match {
            case Success(scrollResult) =>
              val body    = searchConverterService.asApiSearchResult(scrollResult)
              val headers = DynamicHeaders.fromMaybeValue("search-context", scrollResult.scrollId)
              Success((body, headers))
            case Failure(ex) => Failure(ex)
          }
        case _ => orFunction
      }

    private def search(
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

      imageSearchService.matchingQuery(settings, user) match {
        case Success(searchResult) =>
          val scrollHeader = DynamicHeaders.fromMaybeValue("search-context", searchResult.scrollId)
          val output       = searchConverterService.asApiSearchResult(searchResult)
          Success((output, scrollHeader))
        case Failure(ex) => Failure(ex)
      }
    }

    def getImages: ServerEndpoint[Any, Eff] = endpoint.get
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
      .out(jsonBody[SearchResult])
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

              search(
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

    def getImagesPost: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Find images.")
      .description("Search for images in the ndla.no database.")
      .in("search")
      .in(jsonBody[SearchParams])
      .errorOut(errorOutputsFor(400))
      .out(jsonBody[SearchResult])
      .out(EndpointOutput.derived[DynamicHeaders])
      .withOptionalUser
      .serverLogicPure(user => { searchParams =>
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

          search(
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
      })

    def findByImageId: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch information for image.")
      .description("Shows info of the image with submitted id.")
      .in(pathImageId)
      .in(languageOpt)
      .out(jsonBody[ImageMetaInformationV2])
      .errorOut(errorOutputsFor(404))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (imageId, language) =>
          readService.withId(imageId, language, user) match {
            case Success(Some(image)) => image.asRight
            case Success(None) =>
              notFoundWithMsg(s"Image with id $imageId and language $language not found").asLeft
            case Failure(ex) => returnLeftError(ex)
          }
        }
      }

    def findImageByExternalId: ServerEndpoint[Any, Eff] = endpoint.get
      .summary("Fetch information for image by external id.")
      .description("Shows info of the image with submitted external id.")
      .in("external_id" / pathExternalId)
      .in(languageOpt)
      .out(jsonBody[ImageMetaInformationV2])
      .errorOut(errorOutputsFor(404))
      .withOptionalUser
      .serverLogicPure { user =>
        { case (externalId, language) =>
          imageRepository.withExternalId(externalId) match {
            case Some(image) =>
              converterService.asApiImageMetaInformationWithDomainUrlV2(image, language, user).handleErrorsOrOk
            case None => notFoundWithMsg(s"Image with external id $externalId not found").asLeft
          }
        }
      }

    def postNewImage: ServerEndpoint[Any, Eff] = endpoint.post
      .summary("Upload a new image with meta information.")
      .description("Upload a new image file with meta data.")
      .in(multipartBody[MetaDataAndFileForm](implicitly))
      .errorOut(errorOutputsFor(400, 401, 403, 413))
      .out(jsonBody[ImageMetaInformationV2])
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure { user => formData =>
        doWithStream(formData.file) { uploadedFile =>
          writeService.storeNewImage(formData.metadata.body, uploadedFile, user).map { storedImage =>
            converterService.asApiImageMetaInformationWithApplicationUrlV2(
              storedImage,
              Some(formData.metadata.body.language),
              Some(user)
            )
          }
        }.flatten.handleErrorsOrOk
      }

    def deleteImage: ServerEndpoint[Any, Eff] = endpoint.delete
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

    def deleteLanguage: ServerEndpoint[Any, Eff] = endpoint.delete
      .summary("Delete language version of image metadata.")
      .description("Delete language version of image metadata.")
      .in(pathImageId / "language" / pathLanguage)
      .out(noContentOrBodyOutput[ImageMetaInformationV2])
      .errorOut(errorOutputsFor(400, 401, 403))
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure { user =>
        { case (imageId, language) =>
          writeService.deleteImageLanguageVersionV2(imageId, language, user).handleErrorsOrOk
        }
      }

    def editImage: ServerEndpoint[Any, Eff] = endpoint.patch
      .summary("Update an existing image with meta information.")
      .description("Updates an existing image with meta data.")
      .in(pathImageId)
      .in(multipartBody[UpdateMetaDataAndFileForm])
      .errorOut(errorOutputsFor(400, 401, 403))
      .out(jsonBody[ImageMetaInformationV2])
      .requirePermission(IMAGE_API_WRITE)
      .serverLogicPure { user => input =>
        val (imageId, formData) = input
        doWithMaybeStream(formData.file) { uploadedFile =>
          writeService.updateImage(imageId, formData.metadata.body, uploadedFile, user)
        }.handleErrorsOrOk
      }

    def getTagsSearchable: ServerEndpoint[Any, Eff] = endpoint.get
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
  }
}
