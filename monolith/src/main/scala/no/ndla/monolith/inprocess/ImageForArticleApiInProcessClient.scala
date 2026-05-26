/*
 * Part of NDLA monolith
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.monolith.inprocess

import no.ndla.articleapi.integration.{ImageApiClient, ImageWithCopyright}

import scala.util.{Failure, Try}

/** In-process implementation of article-api's [[ImageApiClient]] trait that delegates to image-api's
  * [[no.ndla.imageapi.service.ReadService]] directly, skipping HTTP/JSON ser-de.
  *
  * The producer registry is taken by-name to avoid construction-order cycles between the per-app
  * [[no.ndla.imageapi.ComponentRegistry]] and [[no.ndla.articleapi.ComponentRegistry]] in the monolith.
  */
class ImageForArticleApiInProcessClient(producerCr: => no.ndla.imageapi.ComponentRegistry) extends ImageApiClient {

  override def getImagesWithIds(ids: Seq[String]): Try[Seq[ImageWithCopyright]] = {
    // The HTTP endpoint parses ids via tapir's Delimited[",", Long]; mirror that here so the in-process path has
    // identical input validation semantics.
    Try(ids.iterator.map(_.toLong).toList).flatMap { longIds =>
      producerCr.readService.getImagesByIdsV3(longIds, None, None) match {
        case scala.util.Success(images) =>
          scala.util.Success(images.map(img => ImageWithCopyright(img.id, img.copyright)))
        case Failure(ex) => Failure(ex)
      }
    }
  }
}
