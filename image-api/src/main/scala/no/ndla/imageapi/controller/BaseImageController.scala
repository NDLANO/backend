/*
 * Part of NDLA image-api
 * Copyright (C) 2022 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.controller

import no.ndla.common.errors.FileTooBigException
import no.ndla.common.model.domain.UploadedFile
import no.ndla.imageapi.Props
import no.ndla.imageapi.model.domain.{ModelReleasedStatus, Sort}
import no.ndla.language.Language
import no.ndla.network.scalatra.NdlaSwaggerSupport
import sttp.model.Part
import sttp.tapir._
import sttp.tapir.model.{CommaSeparated, Delimited}

import java.io.File
import scala.util.{Failure, Try}

trait BaseImageController {
  this: Props with NdlaSwaggerSupport =>

  /** Base class for sharing code between Image controllers. */
  trait BaseImageController {

    import props._

    val queryParam =
      query[Option[String]]("query")
        .description("Return only images with titles, alt-texts or tags matching the specified query.")
    val minSize =
      query[Option[Int]]("minimum-size")
        .description("Return only images with full size larger than submitted value in bytes.")
    val language =
      query[String]("language")
        .description("The ISO 639-1 language code describing language.")
        .default(Language.AllLanguages)
    val languageOpt =
      query[Option[String]]("language")
        .description("The ISO 639-1 language code describing language.")
    val fallback =
      query[Boolean]("fallback")
        .description("Fallback to existing language if language is specified.")
        .default(false)
    val license =
      query[Option[String]]("license")
        .description("Return only images with provided license.")
    val includeCopyrighted =
      query[Boolean]("includeCopyrighted")
        .description("Return copyrighted images. May be omitted.")
        .default(false)
    val sort = query[Option[String]]("sort")
      .description(
        s"""The sorting used on results.
             The following are supported: ${Sort.all.mkString(", ")}.
             Default is by -relevance (desc) when query is set, and title (asc) when query is empty.""".stripMargin
      )
    val pageNo =
      query[Option[Int]]("page")
        .description("The page number of the search hits to display.")
    val pageSize = query[Option[Int]]("page-size")
      .description(
        s"The number of search hits to display for each page. Defaults to $DefaultPageSize and max is $MaxPageSize."
      )

    val pathImageId = path[Long]("image_id").description("Image_id of the image that needs to be fetched.")
    val pathLanguage =
      path[String]("language").description("The ISO 639-1 language code describing language.")
    val pathExternalId =
      path[String]("external_id").description("External node id of the image that needs to be fetched.")

    val scrollId = query[Option[String]]("search-context").description(
      s"""A unique string obtained from a search you want to keep scrolling in. To obtain one from a search, provide one of the following values: ${InitialScrollContextKeywords
          .mkString("[", ",", "]")}.
         |When scrolling, the parameters from the initial search is used, except in the case of '${this.language.name}'.
         |This value may change between scrolls. Always use the one in the latest scroll result (The context, if unused, dies after $ElasticSearchScrollKeepAlive).
         |If you are not paginating past $ElasticSearchIndexMaxResultWindow hits, you can ignore this and use '${this.pageNo.name}' and '${this.pageSize.name}' instead.
         |""".stripMargin
    )

    val modelReleased = query[CommaSeparated[String]]("model-released")
      .description(
        s"Filter whether the image(s) should be model-released or not. Multiple values can be specified in a comma separated list. Possible values include: ${ModelReleasedStatus.values
            .mkString(",")}"
      )
      .default(Delimited[",", String](List.empty))

    val imageIds = query[CommaSeparated[Long]]("ids")
      .description(
        "Return only images that have one of the provided ids. To provide multiple ids, separate by comma (,)."
      )
      .default(Delimited[",", Long](List.empty))
    val podcastFriendly =
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
