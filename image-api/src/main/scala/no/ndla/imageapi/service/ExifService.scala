/*
 * Part of NDLA image-api
 * Copyright (C) 2026 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi.service

import com.sksamuel.scrimage.metadata.ImageMetadata
import com.typesafe.scalalogging.StrictLogging
import no.ndla.common.model.domain.UploadedFile

import java.io.InputStream
import scala.util.{Try, Using}

object ExifService extends StrictLogging {

  private def extractMetadataMap(metadata: ImageMetadata): Map[String, String] = {
    metadata
      .getDirectories
      .flatMap { directory =>
        directory
          .getTags
          .flatMap { tag =>
            val name  = s"${directory.getName}:${tag.getName}"
            val value = tag.getRawValue
            Option.when(value != null && value.nonEmpty)(name -> value)
          }
      }
      .toMap
  }

  /** Extracts all EXIF key-value pairs from an uploaded image file. Returns an empty map if no EXIF data is found or if
    * reading fails.
    */
  def extractExifData(file: UploadedFile): Map[String, String] = {
    Try {
      Using.resource(file.createStream()) { stream =>
        extractMetadataMap(ImageMetadata.fromStream(stream))
      }
    }.recover { case ex =>
        logger.warn(s"Failed to extract EXIF data from uploaded image: ${ex.getMessage}", ex)
        Map.empty[String, String]
      }
      .getOrElse(Map.empty)
  }

  /** Extracts all EXIF key-value pairs from an InputStream. Returns an empty map if no EXIF data is found or if reading
    * fails.
    */
  def extractExifDataFromStream(stream: InputStream): Map[String, String] = {
    Try {
      extractMetadataMap(ImageMetadata.fromStream(stream))
    }.recover { case ex =>
        logger.warn(s"Failed to extract EXIF data from stream: ${ex.getMessage}", ex)
        Map.empty[String, String]
      }
      .getOrElse(Map.empty)
  }
}
