/*
 * Part of NDLA image-api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.integration.DraftApiClient
import no.ndla.imageapi.model.ValidationException
import no.ndla.imageapi.model.api.{
  Error,
  ErrorHelpers,
  ImageMetaInformationV2,
  NewImageMetaInformationV2,
  SearchParams,
  SearchResult,
  TagsSearchResult,
  UpdateImageMetaInformation,
  ValidationError
}
import no.ndla.imageapi.model.domain.{DBImageMetaInformation, ModelReleasedStatus, SearchSettings, Sort}
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.search.{ImageSearchService, SearchConverterService}
import no.ndla.imageapi.service.{ConverterService, ReadService, WriteService}
import no.ndla.language.Language
import org.json4s.Formats
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger._
import org.scalatra.util.NotNothing
import org.scalatra.{Delete, Get, Head, NoContent, NotFound, Ok, Options, Patch, Post, Put, Route, RouteTransformer}

import scala.util.{Failure, Success}

trait ImageControllerV2 {
  this: ImageRepository
    with ImageSearchService
    with ConverterService
    with ReadService
    with WriteService
    with DraftApiClient
    with SearchConverterService
    with Role
    with User
    with NdlaController
    with DBImageMetaInformation
    with Props
    with ErrorHelpers =>
  val imageControllerV2: ImageControllerV2

  class ImageControllerV2(implicit val swagger: Swagger)
      extends NdlaController
      with SwaggerSupport
      with FileUploadSupport {

    import props._

    // Swagger-stuff
    protected val applicationDescription                 = "Services for accessing images from NDLA"
    protected implicit override val jsonFormats: Formats = DBImageMetaInformation.jsonEncoder

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewImageMetaInformationV2]()
    registerModel[UpdateImageMetaInformation]()

    val response403 = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response400 = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response413 = ResponseMessage(413, "File too big", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    case class Param[T](paramName: String, description: String)

    private val correlationId =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    private val query =
      Param[Option[String]]("query", "Return only images with titles, alt-texts or tags matching the specified query.")
    private val minSize =
      Param[Option[Int]]("minimum-size", "Return only images with full size larger than submitted value in bytes.")
    private val language = Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    private val license  = Param[Option[String]]("license", "Return only images with provided license.")
    private val includeCopyrighted =
      Param[Option[Boolean]]("includeCopyrighted", "Return copyrighted images. May be omitted.")
    private val sort = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    private val pageNo = Param[Option[Int]]("page", "The page number of the search hits to display.")
    private val pageSize = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
    )
    private val imageId      = Param[String]("image_id", "Image_id of the image that needs to be fetched.")
    private val pathLanguage = Param[String]("language", "The ISO 639-1 language code describing language.")
    private val externalId   = Param[String]("external_id", "External node id of the image that needs to be fetched.")
    private val metadata = Param[NewImageMetaInformationV2](
      "metadata",
      """The metadata for the image file to submit.""".stripMargin
    )

    private val updateMetadata = Param[UpdateImageMetaInformation](
      "metadata",
      """The metadata for the image file to submit.""".stripMargin
    )
    private val file = Param("file", "The image file(s) to upload")

    private val scrollId = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
         |""".stripMargin
    )

    private val modelReleased = Param[Option[Seq[String]]](
      "model-released",
      s"Filter whether the image(s) should be model-released or not. Multiple values can be specified in a comma separated list. Possible values include: ${ModelReleasedStatus.values
          .mkString(",")}"
    )

    private def asQueryParam[T: Manifest: NotNothing](param: Param[T]) =
      queryParam[T](param.paramName).description(param.description)

    private def asHeaderParam[T: Manifest: NotNothing](param: Param[T]) =
      headerParam[T](param.paramName).description(param.description)

    private def asPathParam[T: Manifest: NotNothing](param: Param[T]) =
      pathParam[T](param.paramName).description(param.description)

    private def asObjectFormParam[T: Manifest: NotNothing](param: Param[T]) = {
      val className = manifest[T].runtimeClass.getSimpleName
      val modelOpt  = models.get(className)

      modelOpt match {
        case Some(value) =>
          formParam(param.paramName, value).description(param.description)
        case None =>
          logger.error(s"${param.paramName} could not be resolved as object formParam, doing regular formParam.")
          formParam[T](param.paramName).description(param.description)
      }
    }

    private def asFileParam(param: Param[_]) =
      Parameter(
        name = param.paramName,
        `type` = ValueDataType("file"),
        description = Some(param.description),
        paramType = ParamType.Form
      )

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxImageFileSizeBytes)))

    /** Does a scroll with [[ImageSearchService]] If no scrollId is specified execute the function @orFunction in the
      * second parameter list.
      *
      * @param orFunction
      *   Function to execute if no scrollId in parameters (Usually searching)
      * @return
      *   A Try with scroll result, or the return of the orFunction (Usually a try with a search result).
      */
    private def scrollSearchOr(scrollId: Option[String], language: String)(orFunction: => Any): Any =
      scrollId match {
        case Some(scroll) if !InitialScrollContextKeywords.contains(scroll) =>
          imageSearchService.scroll(scroll, language) match {
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
        language: Option[String],
        license: Option[String],
        sort: Option[Sort],
        pageSize: Option[Int],
        page: Option[Int],
        includeCopyrighted: Boolean,
        shouldScroll: Boolean,
        modelReleasedStatus: Seq[ModelReleasedStatus.Value]
    ) = {
      val settings = query match {
        case Some(searchString) =>
          SearchSettings(
            query = Some(searchString.trim),
            minimumSize = minimumSize,
            language = language,
            license = license,
            sort = sort.getOrElse(Sort.ByRelevanceDesc),
            page = page,
            pageSize = pageSize,
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
            sort = sort.getOrElse(Sort.ByTitleAsc),
            page = page,
            pageSize = pageSize,
            includeCopyrighted = includeCopyrighted,
            shouldScroll = shouldScroll,
            modelReleased = modelReleasedStatus
          )
      }

      imageSearchService.matchingQuery(settings) match {
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
            asQueryParam(license),
            asQueryParam(includeCopyrighted),
            asQueryParam(sort),
            asQueryParam(pageNo),
            asQueryParam(pageSize),
            asQueryParam(scrollId),
            asQueryParam(modelReleased)
          )
          .responseMessages(response500)
      )
    ) {
      val scrollId = paramOrNone(this.scrollId.paramName)
      val language = paramOrNone(this.language.paramName)

      scrollSearchOr(scrollId, language.getOrElse(Language.AllLanguages)) {
        val minimumSize        = intOrNone(this.minSize.paramName)
        val query              = paramOrNone(this.query.paramName)
        val license            = params.get(this.license.paramName)
        val pageSize           = intOrNone(this.pageSize.paramName)
        val page               = intOrNone(this.pageNo.paramName)
        val sort               = Sort.valueOf(paramOrDefault(this.sort.paramName, ""))
        val includeCopyrighted = booleanOrDefault(this.includeCopyrighted.paramName, default = false)
        val shouldScroll       = paramOrNone(this.scrollId.paramName).exists(InitialScrollContextKeywords.contains)
        val modelReleasedStatus =
          paramAsListOfString(this.modelReleased.paramName).map(ModelReleasedStatus.valueOf).flatten

        search(
          minimumSize,
          query,
          language,
          license,
          sort,
          pageSize,
          page,
          includeCopyrighted,
          shouldScroll,
          modelReleasedStatus
        )
      }
    }

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
      val language     = searchParams.language

      scrollSearchOr(searchParams.scrollId, language.getOrElse(Language.AllLanguages)) {
        val minimumSize        = searchParams.minimumSize
        val query              = searchParams.query
        val license            = searchParams.license
        val pageSize           = searchParams.pageSize
        val page               = searchParams.page
        val sort               = Sort.valueOf(searchParams.sort)
        val includeCopyrighted = searchParams.includeCopyrighted.getOrElse(false)
        val shouldScroll       = searchParams.scrollId.exists(InitialScrollContextKeywords.contains)
        val modelReleasedStatus =
          searchParams.modelReleased.getOrElse(Seq.empty).map(ModelReleasedStatus.valueOf).flatten

        search(
          minimumSize,
          query,
          language,
          license,
          sort,
          pageSize,
          page,
          includeCopyrighted,
          shouldScroll,
          modelReleasedStatus
        )
      }
    }

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

      readService.withId(imageId, language) match {
        case Success(Some(image)) => image
        case Success(None) =>
          halt(
            status = 404,
            body = Error(ErrorHelpers.NOT_FOUND, s"Image with id $imageId and language $language not found")
          )
        case Failure(ex) => errorHandler(ex)
      }
    }

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
      val externalId = params(this.externalId.paramName)
      val language   = paramOrNone(this.language.paramName)

      imageRepository.withExternalId(externalId) match {
        case Some(image) => Ok(converterService.asApiImageMetaInformationWithDomainUrlV2(image, language))
        case None        => NotFound(Error(ErrorHelpers.NOT_FOUND, s"Image with external id $externalId not found"))
      }
    }

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
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

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
              writeService.storeNewImage(imageMeta, file) match {
                case Failure(ex) => errorHandler(ex)
                case Success(storedImage) =>
                  converterService.asApiImageMetaInformationWithApplicationUrlV2(storedImage, Some(imageMeta.language))
              }
          }
      }
    }

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
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val imageId = long(this.imageId.paramName)
      writeService.deleteImageAndFiles(imageId) match {
        case Failure(ex) => errorHandler(ex)
        case Success(_)  => Ok()
      }

    }

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
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

      val imageId  = long(this.imageId.paramName)
      val language = params(this.language.paramName)

      writeService.deleteImageLanguageVersion(imageId, language) match {
        case Failure(ex)          => errorHandler(ex)
        case Success(Some(image)) => Ok(image)
        case Success(None)        => NoContent()
      }
    }

    patch(
      "/:image_id",
      operation(
        apiOperation[ImageMetaInformationV2]("newImage")
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
      authUser.assertHasId()
      authRole.assertHasRole(RoleWithWriteAccess)

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
          writeService.updateImage(imageId, metaInfo, fileItem) match {
            case Success(imageMeta) => Ok(imageMeta)
            case Failure(e)         => errorHandler(e)
          }
      }
    }

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

    }
    // TODO: getV3("/:image_id") {
    //         // ... do stuff
    //        }

  }

}
