/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.ValidationException
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.api.{
  Error,
  ErrorHelpers,
  ImageMetaInformationV2,
  NewImageMetaInformationV2,
  SearchParams,
  SearchResult,
  TagsSearchResult,
  UpdateImageMetaInformation
}
import no.ndla.imageapi.model.domain.{DBImageMetaInformation, ModelReleasedStatus, SearchSettings, Sort}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageSearchService, SearchConverterService}
import no.ndla.imageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import no.ndla.network.tapir.auth.Permission.IMAGE_API_WRITE
import no.ndla.network.tapir.auth.TokenUser
import org.scalatra.swagger._
import org.scalatra.{NoContent, NotFound, Ok}

import scala.util.{Failure, Success}

trait ImageControllerV2 {
  this: ImageRepository
    with ImageSearchService
    with ConverterService
    with ReadService
    with WriteService
    with SearchConverterService
    with NdlaController
    with DBImageMetaInformation
    with Props
    with ErrorHelpers
    with BaseImageController =>
  val imageControllerV2: ImageControllerV2

  class ImageControllerV2(override implicit val swagger: Swagger) extends BaseImageController {

    import props._

    /** Does a scroll with [[ImageSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    protected def scrollSearchOr(scrollId: Option[String], language: String, user: Option[TokenUser])(
        orFunction: => Any
    ): Any =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          imageSearchService.scrollV2(scroll, language, user) match {
            case Success(scrollResult) =>
              val responseHeader = scrollResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
              Ok(searchConverterService.asApiSearchResult(scrollResult), headers = responseHeader)
            case Failure(ex) => errorHandler(ex)
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
          val responseHeader = searchResult.scrollId.map(i => this.scrollId.paramName -> i).toMap
          Ok(searchConverterService.asApiSearchResult(searchResult), headers = responseHeader)
        case Failure(ex) => errorHandler(ex)
      }
    }

    get(
      "/",
      operation(
        apiOperation[SearchResult]("getImages")
          .summary("Find images.")
          .description("Find images in the ndla.no database.")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(minSize),
            asQueryParam(language),
            asQueryParam(fallback),
            asQueryParam(license),
            asQueryParam(includeCopyrighted),
            asQueryParam(sort),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(podcastFriendly),
            asQueryParam(scrollId),
            asQueryParam(modelReleased)
          )
          .responseMessages(response500)
      )
    ) {
      val scrollId = paramOrNone(this.scrollId.paramName)
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)
      val fallback = booleanOrDefault(this.fallback.paramName, default = false)
      val user     = TokenUser.fromScalatraRequest(request).toOption

      scrollSearchOr(scrollId, language, user) {
        val minimumSize        = intOrNone(this.minSize.paramName)
        val query              = paramOrNone(this.query.paramName)
        val license            = params.get(this.license.paramName)
        val pageSize           = intOrNone(this.pageSize.paramName)
        val page               = intOrNone(this.pageNo.paramName)
        val podcastFriendly    = booleanOrNone(this.podcastFriendly.paramName)
        val sort               = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
        val includeCopyrighted = booleanOrDefault(this.includeCopyrighted.paramName, default = false)
        val shouldScroll       = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)
        val modelReleasedStatus =
          paramAsListOfString(this.modelReleased.paramName).flatMap(ModelReleasedStatus.valueOf)

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
      }
    }: Unit

    post(
      "/search/",
      operation(
        apiOperation[List[SearchResult]]("getImagesPost")
          .summary("Find images.")
          .description("Search for images in the ndla.no database.")
          .parameters(
            asHeaderParam(correlationId),
            bodyParam[SearchParams],
            asQueryParam(scrollId)
          )
          .responseMessages(response400, response500)
      )
    ) {
      val searchParams = extract[SearchParams](request.body)
      val language     = searchParams.language.getOrElse(Language.AllLanguages)
      val fallback     = searchParams.fallback.getOrElse(false)
      val user         = TokenUser.fromScalatraRequest(request).toOption

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
      }
    }: Unit

    get(
      "/:image_id",
      operation(
        apiOperation[ImageMetaInformationV2]("findByImageId")
          .summary("Fetch information for image.")
          .description("Shows info of the image with submitted id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(imageId),
            asQueryParam(language)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val imageId  = long(this.imageId.paramName)
      val language = paramOrNone(this.language.paramName)
      val user     = TokenUser.fromScalatraRequest(request).toOption

      readService.withId(imageId, language, user) match {
        case Success(Some(image)) => image
        case Success(None) =>
          halt(
            status = 404,
            body = Error(ErrorHelpers.NOT_FOUND, s"Image with id $imageId and language $language not found")
          )
        case Failure(ex) => errorHandler(ex)
      }
    }: Unit

    get(
      "/external_id/:external_id",
      operation(
        apiOperation[ImageMetaInformationV2]("findImageByExternalId")
          .summary("Fetch information for image by external id.")
          .description("Shows info of the image with submitted external id.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(externalId),
            asQueryParam(language)
          )
          .responseMessages(response404, response500)
      )
    ) {
      val user       = TokenUser.fromScalatraRequest(request).toOption
      val externalId = params(this.externalId.paramName)
      val language   = paramOrNone(this.language.paramName)

      imageRepository.withExternalId(externalId) match {
        case Some(image) => Ok(converterService.asApiImageMetaInformationWithDomainUrlV2(image, language, user))
        case None        => NotFound(Error(ErrorHelpers.NOT_FOUND, s"Image with external id $externalId not found"))
      }
    }: Unit

    post(
      "/",
      operation(
        apiOperation[ImageMetaInformationV2]("newImage")
          .summary("Upload a new image with meta information.")
          .description("Upload a new image file with meta data.")
          .consumes("multipart/form-data")
          .parameters(
            asHeaderParam(correlationId),
            asObjectFormParam(metadata),
            formParam(metadata.paramName, models("NewImageMetaInformationV2")),
            asFileParam(file)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response413, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(IMAGE_API_WRITE) { user =>
        val imageMetaFromParam = params.get(this.metadata.paramName)
        val imageMetaFromFile = fileParams
          .get(this.metadata.paramName)
          .map(f => scala.io.Source.fromInputStream(f.getInputStream).mkString)

        imageMetaFromParam.orElse(imageMetaFromFile).map(extract[NewImageMetaInformationV2]) match {
          case None => errorHandler(ValidationException("metadata", "The request must contain image metadata"))
          case Some(imageMeta) =>
            fileParams.get(this.file.paramName) match {
              case None => errorHandler(ValidationException("file", "The request must contain an image file"))
              case Some(file) =>
                writeService.storeNewImage(imageMeta, file, user) match {
                  case Failure(ex) => errorHandler(ex)
                  case Success(storedImage) =>
                    converterService.asApiImageMetaInformationWithApplicationUrlV2(
                      storedImage,
                      Some(imageMeta.language),
                      Some(user)
                    )
                }
            }
        }
      }
    }: Unit

    delete(
      "/:image_id",
      operation(
        apiOperation[Unit]("deleteImage")
          .summary("Deletes the specified images meta data and file")
          .description("Deletes the specified images meta data and file")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(imageId)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response413, response500)
      )
    ) {
      requirePermissionOrAccessDenied(IMAGE_API_WRITE) {
        val imageId = long(this.imageId.paramName)
        writeService.deleteImageAndFiles(imageId) match {
          case Failure(ex) => errorHandler(ex)
          case Success(_)  => Ok()
        }
      }
    }: Unit

    delete(
      "/:image_id/language/:language",
      operation(
        apiOperation[ImageMetaInformationV2]("deleteLanguage")
          .summary("Delete language version of image metadata.")
          .description("Delete language version of image metadata.")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(imageId),
            asPathParam(pathLanguage)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(IMAGE_API_WRITE) { user =>
        val imageId  = long(this.imageId.paramName)
        val language = params(this.language.paramName)

        writeService.deleteImageLanguageVersionV2(imageId, language, user) match {
          case Failure(ex)          => errorHandler(ex)
          case Success(Some(image)) => Ok(image)
          case Success(None)        => NoContent()
        }
      }
    }: Unit

    patch(
      "/:image_id",
      operation(
        apiOperation[ImageMetaInformationV2]("editImage")
          .summary("Update an existing image with meta information.")
          .description("Updates an existing image with meta data.")
          .consumes("form-data")
          .parameters(
            asHeaderParam(correlationId),
            asPathParam(imageId),
            bodyParam[UpdateImageMetaInformation]("metadata")
              .description("The metadata for the image file to submit."),
            asObjectFormParam(metadata),
            formParam(updateMetadata.paramName, models("UpdateImageMetaInformation"))
              .description("metadata used when also updating imagefile"),
            asFileParam(file)
          )
          .authorizations("oauth2")
          .responseMessages(response400, response403, response500)
      )
    ) {
      requirePermissionOrAccessDeniedWithUser(IMAGE_API_WRITE) { user =>
        val imageId            = long(this.imageId.paramName)
        val imageMetaFromParam = params.get(this.updateMetadata.paramName)

        lazy val imageMetaFromFile =
          fileParams
            .get(this.updateMetadata.paramName)
            .map(f => scala.io.Source.fromInputStream(f.getInputStream).mkString)

        val metaToUse = imageMetaFromParam.orElse(imageMetaFromFile).getOrElse(request.body)

        tryExtract[UpdateImageMetaInformation](metaToUse) match {
          case Failure(ex) => errorHandler(ex)
          case Success(metaInfo) =>
            val fileItem = fileParams.get(this.file.paramName)
            writeService.updateImage(imageId, metaInfo, fileItem, user) match {
              case Success(imageMeta) => Ok(imageMeta)
              case Failure(e)         => errorHandler(e)
            }
        }
      }
    }: Unit

    get(
      "/tag-search/",
      operation(
        apiOperation[TagsSearchResult]("getTagsSearchable")
          .summary("Retrieves a list of all previously used tags in images")
          .description("Retrieves a list of all previously used tags in images")
          .parameters(
            asHeaderParam(correlationId),
            asQueryParam(query),
            asQueryParam(pageSize),
            asQueryParam(pageNo),
            asQueryParam(language),
            asQueryParam(sort)
          )
          .responseMessages(response500)
          .authorizations("oauth2")
      )
    ) {
      val query = paramOrDefault(this.query.paramName, "")
      val pageSize = intOrDefault(this.pageSize.paramName, props.DefaultPageSize) match {
        case tooSmall if tooSmall < 1 => props.DefaultPageSize
        case x                        => x
      }
      val pageNo = intOrDefault(this.pageNo.paramName, 1) match {
        case tooSmall if tooSmall < 1 => 1
        case x                        => x
      }
      val language = paramOrDefault(this.language.paramName, Language.AllLanguages)

      val sort = Sort.valueOf(paramOrDefault(this.sort.paramName, "")).getOrElse(Sort.ByRelevanceDesc)

      readService.getAllTags(query, pageSize, pageNo, language, sort) match {
        case Failure(ex)     => errorHandler(ex)
        case Success(result) => Ok(result)
      }

    }: Unit
  }
}
