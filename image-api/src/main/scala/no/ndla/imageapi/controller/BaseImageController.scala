/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.imageapi.Props
import no.ndla.imageapi.model.api.{Error, NewImageMetaInformationV2, UpdateImageMetaInformation, ValidationError}
import no.ndla.imageapi.model.domain.{DBImageMetaInformation, ModelReleasedStatus, Sort}
import no.ndla.network.scalatra.NdlaSwaggerSupport
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.servlet.{FileUploadSupport, MultipartConfig}
import org.scalatra.swagger._

trait BaseImageController {
  this: Props with NdlaController with DBImageMetaInformation with NdlaSwaggerSupport =>

  /** Base class for sharing code between Image controllers. */
  class BaseImageController(implicit override val swagger: Swagger)
      extends NdlaController
      with NdlaSwaggerSupport
      with FileUploadSupport {

    import props._

    // Swagger-stuff
    protected val applicationDescription                 = "Services for accessing images from NDLA"
    protected implicit override val jsonFormats: Formats = DefaultFormats ++ ImageMetaInformation.jsonEncoders

    // Additional models used in error responses
    registerModel[ValidationError]()
    registerModel[Error]()
    registerModel[NewImageMetaInformationV2]()
    registerModel[UpdateImageMetaInformation]()

    val response403: ResponseMessage = ResponseMessage(403, "Access Denied", Some("Error"))
    val response404: ResponseMessage = ResponseMessage(404, "Not found", Some("Error"))
    val response400: ResponseMessage = ResponseMessage(400, "Validation error", Some("ValidationError"))
    val response413: ResponseMessage = ResponseMessage(413, "File too big", Some("Error"))
    val response500: ResponseMessage = ResponseMessage(500, "Unknown error", Some("Error"))

    protected val correlationId: Param[Option[String]] =
      Param[Option[String]]("X-Correlation-ID", "User supplied correlation-id. May be omitted.")
    protected val query: Param[Option[String]] =
      Param[Option[String]]("query", "Return only images with titles, alt-texts or tags matching the specified query.")
    protected val minSize: Param[Option[Int]] =
      Param[Option[Int]]("minimum-size", "Return only images with full size larger than submitted value in bytes.")
    protected val language: Param[Option[String]] =
      Param[Option[String]]("language", "The ISO 639-1 language code describing language.")
    protected val fallback: Param[Option[Boolean]] =
      Param[Option[Boolean]]("fallback", "Fallback to existing language if language is specified.")
    protected val license: Param[Option[String]] =
      Param[Option[String]]("license", "Return only images with provided license.")
    protected val includeCopyrighted: Param[Option[Boolean]] =
      Param[Option[Boolean]]("includeCopyrighted", "Return copyrighted images. May be omitted.")
    protected val sort: Param[Option[String]] = Param[Option[String]](
      "sort",
      s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
    )
    protected val pageNo: Param[Option[Int]] =
      Param[Option[Int]]("page", "The page number of the search hits to display.")
    protected val pageSize: Param[Option[Int]] = Param[Option[Int]](
      "page-size",
      s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
    )
    protected val imageId: Param[String] = Param[String]("image_id", "Image_id of the image that needs to be fetched.")
    protected val pathLanguage: Param[String] =
      Param[String]("language", "The ISO 639-1 language code describing language.")
    protected val externalId: Param[String] =
      Param[String]("external_id", "External node id of the image that needs to be fetched.")
    protected val metadata: Param[NewImageMetaInformationV2] = Param[NewImageMetaInformationV2](
      "metadata",
      """The metadata for the image file to submit.""".stripMargin
    )

    protected val updateMetadata: Param[UpdateImageMetaInformation] = Param[UpdateImageMetaInformation](
      "metadata",
      """The metadata for the image file to submit.""".stripMargin
    )
    protected val file: Param[Nothing] = Param("file", "The image file(s) to upload")

    protected val scrollId: Param[Option[String]] = Param[Option[String]](
      "search-context",
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.paramName}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.paramName}' and '${this.pageSize.paramName}' instead.
         |""".stripMargin
    )

    protected val modelReleased: Param[Option[Seq[String]]] = Param[Option[Seq[String]]](
      "model-released",
      s"Filter whether the image(s) should be model-released or not. Multiple values can be specified in a comma separated list. Possible values include: ${ModelReleasedStatus.values
          .mkString(",")}"
    )
    protected val imageIds = Param[Option[String]](
      "ids",
      "Return only images that have one of the provided ids. To provide multiple ids, separate by comma (,)."
    )
    protected val podcastFriendly: Param[Option[Boolean]] =
      Param[Option[Boolean]](
        "podcast-friendly",
        "Filter images that are podcast friendly. Width==heigth and between 1400 and 3000."
      )

    configureMultipartHandling(MultipartConfig(maxFileSize = Some(MaxImageFileSizeBytes.toLong)))
  }
}
