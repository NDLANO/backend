/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.FileTooBigException
import no.ndla.common.model.api.CommaSeparatedList._
import no.ndla.common.model.domain.UploadedFile
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.{ModelReleasedStatus, Sort}
import no.ndla.language.Language
import sttp.model.Part
import sttp.tapir._

import java.io.File
import scala.util.{Failure, Try}
import sttp.tapir.model.Delimited

trait BaseImageController {
  this: Props =>

  /** Base class for sharing code between Image controllers. */
  trait BaseImageController {

    import props._

    val queryParam: EndpointInput.Query[Option[String]] =
      query[Option[String]]("query")
        .description("Return only images with titles, alt-texts or tags matching the specified query.")
    val minSize: EndpointInput.Query[Option[Int]] =
      query[Option[Int]]("minimum-size")
        .description("Return only images with full size larger than submitted value in bytes.")
    val language: EndpointInput.Query[String] =
      query[String]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(Language.AllLanguages)
    val languageOpt: EndpointInput.Query[Option[String]] =
      query[Option[String]]("language")
        .description("The ISO 639-1 language code describing language.")
    val fallback: EndpointInput.Query[Boolean] =
      query[Boolean]("fallback")
        .description("Fallback to existing language if language is specified.")
        .default(false)
    val license: EndpointInput.Query[Option[String]] =
      query[Option[String]]("license")
        .description("Return only images with provided license.")
    val includeCopyrighted: EndpointInput.Query[Boolean] =
      query[Boolean]("includeCopyrighted")
        .description("Return copyrighted images. May be omitted.")
        .default(false)
    val sort: EndpointInput.Query[Option[String]] = query[Option[String]]("sort")
      .description(
        s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
      )
    val pageNo: EndpointInput.Query[Option[Int]] =
      query[Option[Int]]("page")
        .description("The page number of the search hits to display.")
    val pageSize: EndpointInput.Query[Option[Int]] = query[Option[Int]]("page-size")
      .description(
        s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
      )

    val pathImageId: EndpointInput.PathCapture[Long] =
      path[Long]("image_id").description("Image_id of the image that needs to be fetched.")
    val pathLanguage: EndpointInput.PathCapture[String] =
      path[String]("language").description("The ISO 639-1 language code describing language.")
    val pathExternalId: EndpointInput.PathCapture[String] =
      path[String]("external_id").description("External node id of the image that needs to be fetched.")

    val scrollId: EndpointInput.Query[Option[String]] = query[Option[String]]("search-context").description(
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
         |""".stripMargin
    )

    val modelReleased: EndpointInput.Query[Option[Delimited[",", String]]] = listQuery[String]("model-released")
      .description(
        s"Filter whether the image(s) should be model-released or not. Multiple values can be specified in a comma separated list. Possible values include: ${ModelReleasedStatus.values
            .mkString(",")}"
      )

    val userFilter: EndpointInput.Query[Option[Delimited[",", String]]] = listQuery[String]("users")
      .description(
        """List of users to filter by.
          |The value to search for is the user-id from Auth0.
          |UpdatedBy on article and user in editorial-notes are searched.""".stripMargin
      )

    val imageIds: EndpointInput.Query[Option[Delimited[",", Long]]] = listQuery[Long]("ids")
      .description(
        "Return only images that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
    val podcastFriendly: EndpointInput.Query[Option[Boolean]] =
      query[Option[Boolean]]("podcast-friendly")
        .description("Filter images that are podcast friendly. Width==heigth and between 1400 and 3000.")

    val maxImageFileSizeBytes: Int = MaxImageFileSizeBytes

    def doWithStream[T](filePart: Part[File])(f: UploadedFile => Try[T]): Try[T] = {
      val file = UploadedFile.fromFilePart(filePart)
      if (file.fileSize > maxImageFileSizeBytes) Failure(FileTooBigException())
      else file.doWithStream(f)
    }

    def doWithMaybeStream[T](filePart: Option[Part[File]])(f: Option[UploadedFile] => Try[T]): Try[T] = {
      filePart match {
        case Some(value) => doWithStream(value) { file => f(Some(file)) }
        case None        => f(None)
      }
    }

  }
}
